#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg
time2=$(date +%s)

echo "Time taken to process 500,000 requests for a single connection:"
awk "BEGIN { print ${time2} - ${time1} }"

echo "Requests per second for a single connection:"
awk "BEGIN { print 500000.0/(${time2} - ${time1}) }"

echo "Seconds per request for a single connection:"
awk "BEGIN { print (${time2} - ${time1})/500000.0 }"
