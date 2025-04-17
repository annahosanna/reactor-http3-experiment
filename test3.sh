#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg
time2=$(date +%s)

singleconnection=`awk "BEGIN { print ${time2} - ${time1} }"`
rps=`awk "BEGIN { print 500000.0/(${time2} - ${time1}) }"`
spr=`awk "BEGIN { print (${time2} - ${time1})/500000.0 }"`

echo "Time to perform 5kk request on a single connection: ${singleconnection}. Requests per second: ${rps}. Seconds per request: ${spr}."
