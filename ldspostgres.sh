#!/usr/bin/env bash

echo Creating folders
mkdir -p ldspostgres/conf
mkdir -p ldspostgres/results
echo Removing previous test-output
rm -f ldspostgres/results/*

ENV_FILE='ldspostgres-compose.env'
if [ -f $ENV_FILE ]; then
    export $(grep -v '^#' $ENV_FILE | envsubst | xargs -0)
fi

echo "Cleaning existing associated volumes and data"
docker-compose -f ldspostgres-compose.yml down
docker volume rm $(docker volume ls -q -f "name=ldspostgres")

echo Starting LDS postgres
docker-compose -f ldspostgres-compose.yml up -d --force-recreate

echo Building container image: ldstestctrl
docker build -t ldstestctrl .

sleep 10

echo Running load-test
./run-loadtest.sh ldspostgres

echo Stopping LDS postgres
docker-compose -f ldspostgres-compose.yml down
