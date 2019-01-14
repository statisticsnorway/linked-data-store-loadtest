#!/usr/bin/env bash

. $(dirname "$0")/functions.sh

OUTPUTFOLDER="out"

case "$1" in

  "help")
    echo Commands: help, init, restart_httploadtest, configure, prepopulate, warmup, run, stop, plot
    ;;

  "init")
    echo init
    check_whether_lds_network_and_containers_exists
    stop_test_containers
    start_test_containers
    sleep 5
    check_health_of_lds
    check_health_of_loadtest
    configure_loadtest
    ;;

  "restart_httploadtest")
    restart_http_loadtest_baseline
    ;;

  "configure")
    configure_loadtest
    ;;

  "prepopulate")
    prepopulate_data
    ;;

  "warmup")
    echo warmup
    warm_up_lds $2 $3
    ;;

  "run")
    echo run
    run_loadtest $2 $3
    ;;

  "stop")
    echo stop
    stop_test_containers
    ;;

  "plot")
    echo plot
    create_gnuplot_files
    ;;

esac
