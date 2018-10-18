#!/usr/bin/env bash
for x in $(ls /loadtest/gsim/schemas | perl -n -e'/(.*)\.json/ && print "$1 "'); do
  echo $x
  for a in 0 1 2 3 4 5 6 7 8 9 A B C D E F; do
    for b in 0 1 2 3 4 5 6 7 8 9 A B C D E F; do
       cat /loadtest/gsim/templates/$x.json | sed "s/#now/2018-10-16T12:01:21Z/" | sed "s/#mrid/${a}${b}C8D2B7-0EB3-4A6D-91BB-A7451649F2F6/" | curl -H "Content-Type: application/json" -X PUT -d @- http://lds:9090/ns/$x/${a}${b}C8D2B7-0EB3-4A6D-91BB-A7451649F2F6 >/dev/null 2>&1;
    done;
  done;
done