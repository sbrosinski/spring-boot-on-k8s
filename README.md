# Tutorial: Deploying a Spring Boot app on Kubernetes

I set up this project to demonstrate how to dockerize a Spring Boot app and deploy, configure and scale it on Kubernetes.

## Prequisites

To follow along, check out this repo and make sure you have the following tools ready:

* [docker](https://www.docker.com/products/docker#/) - to build the docker images we want to deploy
* [minikube](https://github.com/kubernetes/minikube) - a local Kubernetes environment
* [kubectl](http://kubernetes.io/docs/user-guide/prereqs/) - the Kubernetes command line interface, on macOS you can `brew install kubernetes-cli` it

## The Spring Boot Service

The details of this service don't matter much. I used the [Spring Initializr](http://start.spring.io/) to create a very simple Spring Boot app which answers on port 8090 to these routes:

* `/hello` - which returns {"greeting": "hello world"}
* `/health` - to report the app's health status

The app can be built with `gradle clean build` which results in a standalone jar named `demo-service-0.0.1-SNAPSHOT.jar` in `build/libs`. The simplest way to run the app is with `java -jar build/libs/demo-service-0.0.1-SNAPSHOT.jar`.

# Creating a Docker images

# Running the Docker images

# Setting up Kubernetes

# Deploying the service to Kubernetes


