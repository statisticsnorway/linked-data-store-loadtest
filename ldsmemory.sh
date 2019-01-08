#!/usr/bin/env bash

echo Creating folders
mkdir -p ldsmemory/conf
mkdir -p ldsmemory/results
echo Removing previous test-output
rm -rf ldsmemory/results/*

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
#sleep 5 && docker logs linked-data-store-loadtest_lds_1

echo Building container image: ldstestctrl
docker build -t ldstestctrl .

sleep 5

echo Running load-test
./run-loadtest.sh ldsmemory

echo Stopping LDS memory
docker-compose -f ldsmemory-compose.yml down
