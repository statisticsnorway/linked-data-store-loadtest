#!/usr/bin/env bash

#
# Sets up test-environment then runs a 5 minute LDS load-test.
# This script will fail early on if no LDS is running.
#

. $(dirname "$0")/functions.sh

run_loadtest_all() {
  echo "Running loadtests with threads ranging from 1 to 50"
  for i in {1..10}; do run_loadtest $i 10; done
  for ((i=12;i<=20;i=i+2)) do run_loadtest $i 10; done
  for ((i=25;i<=50;i=i+5)) do run_loadtest $i 10; done
  sleep 5
}


###################################
## Program execution starts here ##
###################################

OUTPUTFOLDER="$1"
echo Output-folder: ${OUTPUTFOLDER}

check_whether_lds_network_and_containers_exists
stop_test_containers
start_test_containers
check_health_of_lds
prepopulate_data
check_health_of_loadtest
configure_loadtest
warm_up_lds 10 30
run_loadtest_all
create_gnuplot_files
stop_test_containers
