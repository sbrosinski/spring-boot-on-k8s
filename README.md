# Tutorial: Deploying a Spring Boot app on Kubernetes

I set up this project to demonstrate how to dockerize a Spring Boot app and deploy, configure and scale it on Kubernetes.
In this tutorial I'm using [minikube](https://github.com/kubernetes/minikube) locally, you can also read my last post on [how to run Kubernetes on AWS](https://brosinski.com/post/kubernetes-on-aws-with-kops/) or try hosted Kubernetes in [Google Container Engine](https://cloud.google.com/container-engine/).

## Prequisites

To follow along, [check out this repo](https://github.com/sbrosinski/spring-boot-on-k8s) and make sure you have the following tools ready:

* [docker](https://www.docker.com/products/docker#/) - to build the docker images we want to deploy
* [minikube](https://github.com/kubernetes/minikube) - a local Kubernetes environment
* [kubectl](http://kubernetes.io/docs/user-guide/prereqs/) - the Kubernetes command line interface, on macOS you can `brew install kubernetes-cli` it

## The Spring Boot Service

The details of this service don't matter much. I used the [Spring Initializr](http://start.spring.io/) to create a very simple Spring Boot app which answers on port 8090 to these routes:

* `/hello` - which returns {"greeting": "hello world"}
* `/health` - to report the app's health status

The app can be built with `gradle clean build` which results in a standalone jar named `demo-service-0.0.1-SNAPSHOT.jar` in `build/libs`. The simplest way to run the app is with `java -jar build/libs/demo-service-0.0.1-SNAPSHOT.jar`.

## Creating a Docker image

We need a container which has a JDK. If you just create an Ubuntu image with the standard Oracle JDK installation, you will end up with and image size of about 1 GB. Not nice to work with. There are better options though: [Creating a minimial JDK installation based on an AlpineLinux image](https://developer.atlassian.com/blog/2015/08/minimal-java-docker-containers/).
The `docker/minimal-java` directory contains a Dockerfile I generated taking that approach.

So we'll just create our own JDK base image first:

    cd docker/minimal-java
    docker build -t sbrosinski/minimal-java .

Now we create a container for our demo service inherating from that base image:

    cd docker/demo-service
    docker build -t sbrosinski/demo-service .

This Dockerfile is very simple and based on [Spring Boot's docker intro](https://spring.io/guides/gs/spring-boot-docker/):

    FROM sbrosinski/minimal-java
    VOLUME /tmp
    EXPOSE 8090
    ADD demo-service-0.0.1-SNAPSHOT.jar app.jar
    RUN sh -c 'touch /app.jar'
    ENV JAVA_OPTS=""
    ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]

If you do this in production [you should probably add some memory limiting options](http://matthewkwilliams.com/index.php/2016/03/17/docker-cgroups-memory-constraints-and-java-cautionary-tale/) to the java call.

## Running the Docker image

To try it out, we can run it locally just using docker:

    docker run -p 8090:8090 -t --name demo-service --rm b7i/demo-service:latest
    curl http://localhost:8090/hello => {"greeting":"hello world"}
    docker stop demo-service

## Publishing the Docker image

Kubernetes will have to pull the docker image from a registry. For this example we can use a public repository on DockerHub. Register on [docker.com](http://docker.com) to create a docker ID.
You can now log into your DockerHub account from your machine with:

    docker login

Push your image to DockerHub with:    

    docker push sbrosinski/demo-service

The image for the demo service is publicly available at [https://hub.docker.com/r/sbrosinski/demo-service/](https://hub.docker.com/r/sbrosinski/demo-service/).     

## Setting up Kubernetes

We're using the local Kubernetes cluster provided by minikube. Start your cluster with:

    minikube start

You can take a look at the (still empty) Kubernetes dashboard with:

    minikube dashboard        

## Deploying the service to Kubernetes

To run our application on the minikube cluster we need to specify a deployment. The deployment descriptor looks like this:

    apiVersion: extensions/v1beta1
    kind: Deployment
    metadata:
    name: demo-service-deployment
    spec:
    replicas: 2 # tells deployment to run 2 pods matching the template
    template: # create pods using pod definition in this template
        metadata:
        labels:
            app: demo-service
        spec:
        containers:
        - name: demo-service
            image: sbrosinski/demo-service
            ports:
            - containerPort: 8090

Create this deployment on the cluster using kubectl:

    kubectl create -f deployment.yml 

You can look at the deployment with:

    kubectl describe deployment demo-service-deployment

    Name:  			demo-service-deployment
    Namespace:     		default
    CreationTimestamp:     	Fri, 18 Nov 2016 11:42:05 +0100
    Labels:			app=demo-service
    Selector:      		app=demo-service
    Replicas:      		2 updated | 2 total | 2 available | 0 unavailable
    StrategyType:  		RollingUpdate
    MinReadySeconds:       	0
    RollingUpdateStrategy: 	1 max unavailable, 1 max surge
    OldReplicaSets:		<none>
    NewReplicaSet: 		demo-service-deployment-1946011246 (2/2 replicas created)
    Events:
    FirstSeen    	LastSeen       	Count  	From   				SubobjectPath  	Type   		Reason 			Message
    ---------    	--------       	-----  	----   				-------------  	--------       	------ 			-------
    1m   		1m     		1      	{deployment-controller }       			Normal 		ScalingReplicaSet      	Scaled up replica set demo-service-deployment-1946011246 to 2

Two pods have been created, a replica set, and the default rolling update strategy. You can also look at the pods with:

    kubectl get pods

    NAME                                       READY     STATUS    RESTARTS   AGE
    demo-service-deployment-1946011246-ap47n   1/1       Running   0          3m
    demo-service-deployment-1946011246-u3dcj   1/1       Running   0          3m

We can join these pods as part of a service and expose it outside of our cluster. Create a service with:

    kubectl create -f service.yml

The service descriptor looks like this:

    apiVersion: v1
    kind: Service
    metadata:
    name: demo-service
    spec:
    ports:
        - port: 8090
        targetPort: 8090
    selector:
        app: demo-service
    type: NodePort

By specifying a service type of `NodePort` we declare to expose the service outside the cluster. Type `LoadBalance`would create a load balancer (e.g. ELB on AWS, but this feature is not availabe for minikube), type `ClusterIP` would expose the service only within the cluster.
We can look at the service details with:

    kubectl describe service demo-service

    Name:  			demo-service
    Namespace:     	default
    Labels:			<none>
    Selector:      	app=demo-service
    Type:  			NodePort
    IP:    			10.0.0.221
    Port:  			<unset>	8090/TCP
    NodePort:      	<unset>	31039/TCP
    Endpoints:     	172.17.0.6:8090,172.17.0.7:8090
    Session Affinity:  None
    No events.

To now access the service, we can use a minikube command to tell us the exact service address:

    minikube service demo-service

This would open your browser and point it, for example, to `http://192.168.99.100:31039`. Port 31029 is the NodePort we requested and the IP address is the address of our minikube cluster. We can now access the service routes:

    curl http://192.168.99.100:31039/hello => {"greeting":"hello world"}        

That's it for now. To expand on this you can try the following: Make the service use a DB and play with Kubernetes persistent volumes and service discovery. 

