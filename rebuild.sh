#! /bin/bash

mvn clean install
docker build -t evgeniyh/object-store-service .
docker push evgeniyh/object-store-service
cf push object-store-service --docker-image evgeniyh/object-store-service --no-manifest
