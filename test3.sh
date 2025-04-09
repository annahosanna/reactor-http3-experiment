#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg
time2=$(date +%s)
# result = $(awk "BEGIN { print ${time2} - ${time1} }")
#echo "${result}"
awk "BEGIN { print ${time2} - ${time1} }"
