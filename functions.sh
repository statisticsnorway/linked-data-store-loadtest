#!/usr/bin/env bash

#
# Sets up test-environment then runs a 5 minute LDS load-test.
# This script will fail early on if no LDS is running.
#

check_whether_lds_network_and_containers_exists() {
  LDS_NETWORK=ldsloadtest
  if [ "${LDS_NETWORK}" = "" ]; then
    echo "ERROR: LDS docker network network does not exists, it should have a name that starts with: ldsloadtest"
    exit 1
  else
    echo "Using docker network: ${LDS_NETWORK}"
  fi

  LDS_NETWORK_CONTAINER_IDS=$(docker ps -q -f "network=${LDS_NETWORK}")
  if [ "${LDS_NETWORK_CONTAINER_IDS}" = "" ]; then
    echo "ERROR: LDS is not running in docker network with name that starts with: ldsloadtest"
    exit 1
  else
    echo Listing relevant LDS and/or loadtest related containers:
    docker ps -f "network=${LDS_NETWORK}"
  fi
}

stop_ldsloadtestcontroller() {
  if [ "$(docker ps -q -f 'name=ldsloadtestcontroller')" != "" ]; then
    echo Stopping test-controller container
    docker stop $(docker ps -q -f 'name=ldsloadtestcontroller')
  fi
  if [ "$(docker ps -aq -f 'name=ldsloadtestcontroller')" != "" ]; then
    echo Removing stopped test-controller container
    docker rm $(docker ps -aq -f 'name=ldsloadtestcontroller')
  fi
}

stop_httploadtest() {
  if [ "$(docker ps -q -f 'name=httploadtestbaseline')" != "" ]; then
    echo Stopping httploadtest-baseline container
    docker stop $(docker ps -q -f 'name=httploadtestbaseline')
  fi
  if [ "$(docker ps -aq -f 'name=httploadtestbaseline')" != "" ]; then
    echo Removing exited httploadtest-baseline container
    docker rm $(docker ps -aq -f 'name=httploadtestbaseline')
  fi
}

start_ldsloadtestcontroller() {
  echo Starting test-controller container
  docker run --network ldsloadtest -t -d -v $(pwd)/setup:/loadtest -v $(pwd)/${OUTPUTFOLDER}/results:/results -w /loadtest --name ldsloadtestcontroller ldstestctrl sh
}

start_httploadtest() {
  echo Starting httploadtest-baseline container
  docker pull cantara/httploadtest-baseline:latest
  docker run --network ldsloadtest --name 'httploadtestbaseline' -d -p 28086:8086 -v $(pwd)/${OUTPUTFOLDER}/results:/home/HTTPLoadTest-baseline/results cantara/httploadtest-baseline:latest
}

stop_test_containers() {
  stop_ldsloadtestcontroller
  stop_httploadtest
}

start_test_containers() {
  start_ldsloadtestcontroller
  start_httploadtest
}

restart_ldsloadtestcontroller() {
  stop_ldsloadtestcontroller
  start_ldsloadtestcontroller
}

restart_http_loadtest_baseline() {
  stop_httploadtest
  start_httploadtest
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
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" http://httploadtestbaseline:8086/HTTPLoadTest-baseline/health) -ne 200 ]; then
    echo ERROR: HTTPLoadTest-baseline is not responding correctly.
    exit 1
  fi
}

configure_loadtest() {
  echo Configuring loadtest ...
  echo Uploading zip-file with templates
  if [ $(docker exec -it ldsloadtestcontroller sh -c 'zip -qr gsim.zip gsim && curl -s -o /dev/null --write-out "%{http_code}" -F "file=@gsim.zip" http://httploadtestbaseline:8086/HTTPLoadTest-baseline/loadTest/zip') -ne 200 ]; then echo ERROR: Unable to upload zip-file; exit 1; fi
  echo Uploading read-specification
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/read.json' http://httploadtestbaseline:8086/HTTPLoadTest-baseline/loadTest/form/read) -ne 200 ]; then echo ERROR: Unable to upload read-specification; exit 1; fi
  echo Uploading write-specification
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/write.json' http://httploadtestbaseline:8086/HTTPLoadTest-baseline/loadTest/form/write) -ne 200 ]; then echo ERROR: Unable to upload write-specification; exit 1; fi
  echo Uploading benchmark-configuration
  if [ $(docker exec -it ldsloadtestcontroller curl -s -o /dev/null --write-out "%{http_code}" --data-urlencode 'jsonConfig@/loadtest/gsim/benchmark.json' http://httploadtestbaseline:8086/HTTPLoadTest-baseline/loadTest/form/benchmark) -ne 200 ]; then echo ERROR: Unable to upload benchmark-configuration; exit 1; fi
}

run_loadtest() {
  if [ $(docker exec -it ldsloadtestcontroller bash -c 'jq ".test_no_of_threads='$1' | .test_sleep_in_ms=0 | .test_read_write_ratio=80 | .test_duration_in_seconds='$2' | .test_randomize_sleeptime=false" /loadtest/gsim/config.json | curl -s -o /dev/null --write-out "%{http_code}" -H "Content-Type: application/json" -d "@-" http://httploadtestbaseline:8086/HTTPLoadTest-baseline/loadTest') -ne 200 ]; then
    echo "ERROR: Unable to trigger loadtest with $1 threads"
    exit 1
  fi
  echo "Running test with $1 threads for $2 seconds"
  sleep $2
  sleep 2
}

warm_up_lds() {
  echo "Warming up . . ."
  run_loadtest $1 $2
  sleep 10
  mkdir -p ${OUTPUTFOLDER}/results/warmup
  mv ${OUTPUTFOLDER}/results/*.csv ${OUTPUTFOLDER}/results/*.json ${OUTPUTFOLDER}/results/warmup
}

create_gnuplot_files() {
  GP_FILE_PREFIX=performance_by_threads
  echo "Generating gnuplot files"
  docker exec -it ldsloadtestcontroller java -cp "/opt/plotgen/*:/opt/plotgen/lib/*" no.ssb.lds.loadtest.HTTPLoadTestBaselineStatistics /results /results $GP_FILE_PREFIX
  echo "Generating svg file using gnuplot"
  docker exec -it -w /results ldsloadtestcontroller gnuplot ${GP_FILE_PREFIX}.gnu
  docker exec -it -w /results ldsloadtestcontroller gnuplot ${GP_FILE_PREFIX}_load.gnu
  docker exec -it -w /results ldsloadtestcontroller gnuplot ${GP_FILE_PREFIX}_total_throughput.gnu
  docker exec -it -w /results ldsloadtestcontroller gnuplot ${GP_FILE_PREFIX}_read_latency.gnu
  docker exec -it -w /results ldsloadtestcontroller gnuplot ${GP_FILE_PREFIX}_write_latency.gnu
}
