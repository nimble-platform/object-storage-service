#!/usr/bin/env bash

if [ "$1" != "build" ] ; then
    echo "Running without build"
else
    mvn clean install | grep SUCCESS
    if [ $? -ne 0 ] ; then
        echo "Failed to build the service"
        exit 1
    fi
    docker build -t evgeniyh/object-store . | grep "evgeniyh/object-store:latest"
    if [ $? -ne 0 ] ; then
        echo "Failed to build the docker image"
        exit 1
    fi
fi

docker stop $(docker ps -aq) > /dev/null

gnome-terminal -e "docker run  -it -p 998:8080 \
-e OBJECT_STORE_CREDENTIALS='' \
evgeniyh/object-store"