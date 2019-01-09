#!/usr/bin/env bash

echo Creating folders
mkdir -p interactive/conf
mkdir -p interactive/results
echo Removing previous test-output
rm -rf interactive/results/*

ENV_FILE='ldsmemory-compose.env'
if [ -f $ENV_FILE ]; then
    source $ENV_FILE
fi

LDS_NETWORK=$(docker network ls -f name=ldsloadtest -q)
if [ "$LDS_NETWORK" == "" ]; then
    echo Create network
    docker network create ldsloadtest
fi

echo Starting LDS memory
docker-compose -f ldsmemory-compose.yml up -d --force-recreate

echo Building container image: ldstestctrl
docker build -t ldstestctrl .

sleep 5

echo Running load-test
./interactive-loadtest.sh init

#echo Stopping LDS memory
#docker-compose -f ldsmemory-compose.yml down
