#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg
time2=$(date +%s)
echo "Requests per second for a single connection:"
awk "BEGIN { print 500000.0/(${time2} - ${time1}) }"
