#!/usr/bin/env bash

echo Creating folders
mkdir -p ldsmemory/conf
mkdir -p ldsmemory/results
echo Removing previous test-output
rm -rf ldsmemory/results/*

ENV_FILE='ldsmemory-compose.env'
if [ -f $ENV_FILE ]; then
    export $(grep -v '^#' $ENV_FILE | envsubst | xargs -0)
fi

echo Starting LDS memory
docker-compose -f ldsmemory-compose.yml up -d --force-recreate

echo Building container image: ldstestctrl
docker build -t ldstestctrl .

sleep 5

echo Running load-test
./run-loadtest.sh ldsmemory

echo Stopping LDS memory
docker-compose -f ldsmemory-compose.yml down
