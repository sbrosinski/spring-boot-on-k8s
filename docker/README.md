Build minimal java 8 image in

cd minimal-java
docker build -t sbrosinski/minimal-java .

cd demo-service
docker build -t sbrosinski/demo-service:latest .

docker run -p 8090:8090 -t --name demo-service --rm sbrosinski/demo-service:latest
docker stop demo-service

docker login
docker push sbrosinski/demo-service

