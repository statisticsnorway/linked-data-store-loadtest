#!/usr/bin/env bash

#
# Sets up test-environment then runs a 5 minute LDS load-test.
# This script will fail early on if no LDS is running.
#

check_whether_lds_network_and_containers_exists() {
  LDS_NETWORK=$(docker network list | grep linkeddatastoredocker | awk '{print $2; exit}')
  if [ "${LDS_NETWORK}" = "" ]; then
    echo "ERROR: LDS docker network network does not exists, it should have a name that starts with: linkeddatastoredocker"
    exit 1
  else
    echo "Using docker network: ${LDS_NETWORK}"
  fi

  LDS_NETWORK_CONTAINER_IDS=$(docker ps -q -f "network=${LDS_NETWORK}")
  if [ "${LDS_NETWORK_CONTAINER_IDS}" = "" ]; then
    echo "ERROR: LDS is not running in docker network with name that starts with: linkeddatastoredocker"
    exit 1
  else
    echo Listing relevant LDS and/or loadtest related containers:
    docker ps -f "network=${LDS_NETWORK}"
  fi
}

docker_build_ldstestctrl() {
  echo Building container image: ldstestctrl
  docker build -t ldstestctrl .
}

start_test_containers() {
  echo Creating test results folder on host in current-working-dirctory
  mkdir -p results
  echo Removing any previous test-output
  rm -f results/*

  if [ "$(docker ps -q -f 'name=ldsloadtestcontroller')" != "" ]; then
    echo Stopping test-controller container
    docker stop $(docker ps -q -f 'name=ldsloadtestcontroller')
  fi
  if [ "$(docker ps -aq -f 'name=ldsloadtestcontroller')" != "" ]; then
    echo Removing stopped test-controller container
    docker rm $(docker ps -aq -f 'name=ldsloadtestcontroller')
  fi
  echo Starting test-controller container
  docker run --network $(docker network list | grep linkeddatastoredocker | awk '{print $2; exit}') -t -d -v $(pwd)/setup:/loadtest -v $(pwd)/results:/results -w /loadtest --name ldsloadtestcontroller ldstestctrl sh


  if [ "$(docker ps -q -f 'ancestor=cantara/httploadtest-baseline' -f 'name=loadtest')" != "" ]; then
    echo Stopping httploadtest-baseline container
    docker stop $(docker ps -q -f 'ancestor=cantara/httploadtest-baseline' -f 'name=loadtest')
  fi
  if [ "$(docker ps -aq -f 'ancestor=cantara/httploadtest-baseline' -f 'name=loadtest')" != "" ]; then
    echo Removing exited httploadtest-baseline container
    docker rm $(docker ps -aq -f 'ancestor=cantara/httploadtest-baseline' -f 'name=loadtest')
  fi
  echo Starting httploadtest-baseline container
  docker run --network $(docker network list | grep linkeddatastoredocker | awk '{print $2; exit}') --name 'loadtest' -d -p 28086:8086 -v $(pwd)/results:/home/HTTPLoadTest-baseline/results cantara/httploadtest-baseline
}

check_health_of_lds() {
  echo Checking health of LDS service
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" http://lds:9090/ping) -ne 200 ]; then
    echo ERROR: LDS is not responding correctly.
    exit 1
  fi
}

prepopulate_data() {
  echo Pre-populating data for managed domains:
  docker exec -it ldsloadtestcontroller /prepopulate.sh
}

check_health_of_loadtest() {
  echo Checking health of httploadtest-baseline container
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" http://loadtest:8086/HTTPLoadTest-baseline/health) -ne 200 ]; then
    echo ERROR: HTTPLoadTest-baseline is not responding correctly.
    exit 1
  fi
}

configure_loadtest() {
  echo Configuring loadtest ...
  echo Uploading zip-file with templates
  if [ $(docker exec -it ldsloadtestcontroller sh -c 'zip -qr gsim.zip gsim && curl -s -o /dev/null --write-out "%{http_code}" -F "file=@gsim.zip" http://loadtest:8086/HTTPLoadTest-baseline/loadTest/zip') -ne 200 ]; then echo ERROR: Unable to upload zip-file; exit 1; fi
  echo Uploading read-specification
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/read.json' http://loadtest:8086/HTTPLoadTest-baseline/loadTest/form/read) -ne 200 ]; then echo ERROR: Unable to upload read-specification; exit 1; fi
  echo Uploading write-specification
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/write.json' http://loadtest:8086/HTTPLoadTest-baseline/loadTest/form/write) -ne 200 ]; then echo ERROR: Unable to upload write-specification; exit 1; fi
  echo Uploading benchmark-configuration
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/benchmark.json' http://loadtest:8086/HTTPLoadTest-baseline/loadTest/form/benchmark) -ne 200 ]; then echo ERROR: Unable to upload benchmark-configuration; exit 1; fi
}

run_loadtest() {
  if [ $(docker exec -it ldsloadtestcontroller bash -c 'jq ".test_no_of_threads='$1' | .test_sleep_in_ms=0 | .test_read_write_ratio=80 | .test_duration_in_seconds='$2' | .test_randomize_sleeptime=false" /loadtest/gsim/config.json | curl -s -o /dev/null --write-out "%{http_code}" -H "Content-Type: application/json" -d "@-" http://loadtest:8086/HTTPLoadTest-baseline/loadTest') -ne 200 ]; then
    echo "ERROR: Unable to trigger loadtest with $1 threads"
    exit 1
  fi
  echo "Running test with $1 threads for $2 seconds"
  sleep $2
  sleep 1
}

warm_up_lds() {
  echo "Warming up . . ."
  run_loadtest 10 60
  mkdir -p results/warmup
  mv results/*.csv results/*.json results/warmup
}

run_loadtest_all() {
  echo "Running loadtests with threads ranging from 1 to 200"
  for i in {1..10}; do run_loadtest $i 10; done
  for ((i=12;i<=20;i=i+2)) do run_loadtest $i 10; done
  for ((i=30;i<=90;i=i+10)) do run_loadtest $i 10; done
  for ((i=100;i<=200;i=i+50)) do run_loadtest $i 10; done
}

create_gnuplot_files() {
  echo "Generating gnuplot files"
  docker exec -it ldsloadtestcontroller java -cp "/opt/plotgen/*:/opt/plotgen/lib/*" no.ssb.lds.loadtest.HTTPLoadTestBaselineStatistics /results /results performance_by_threads
  echo "Generating svg file using gnuplot"
  docker exec -it -w /results ldsloadtestcontroller gnuplot performance_by_threads.gnu
}



###################################
## Program execution starts here ##
###################################

check_whether_lds_network_and_containers_exists
docker_build_ldstestctrl
start_test_containers
check_health_of_lds
prepopulate_data
check_health_of_loadtest
configure_loadtest
warm_up_lds
run_loadtest_all
create_gnuplot_files
