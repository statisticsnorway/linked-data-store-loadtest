#!/usr/bin/env bash

echo Creating folders
mkdir -p ldsneo4j/conf
mkdir -p ldsneo4j/results
echo Removing previous test-output
rm -f ldsneo4j/results/*

ENV_FILE='ldsneo4j-compose.env'
if [ -f $ENV_FILE ]; then
    export $(grep -v '^#' $ENV_FILE | envsubst | xargs -0)
fi

echo "Cleaning existing associated volumes and data"
docker-compose -f ldsneo4j-compose.yml down
docker volume rm $(docker volume ls -q -f "name=ldsneo4j")

echo Starting LDS neo4j
docker-compose -f ldsneo4j-compose.yml up -d --force-recreate

echo Building container image: ldstestctrl
docker build -t ldstestctrl .

sleep 20

echo Running load-test
./run-loadtest.sh ldsneo4j

echo Stopping LDS neo4j
docker-compose -f ldsneo4j-compose.yml down
