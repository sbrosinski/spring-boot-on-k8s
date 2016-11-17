Build minimal java 8 image in

cd minimal-java
docker build -t b7i/minimal-java .

cd demo-service
docker build -t b7i/demo-service:latest .

docker run -p 8090:8090 -t --name demo-service --rm b7i/demo-service:latest
docker stop demo-service

docker login
docker tag b7i/demo-service sbrosinski/demo-service
docker push sbrosinski/demo-service

