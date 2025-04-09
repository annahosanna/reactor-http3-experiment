#!/bin/bash

time1=$(date +%s)
curl -K ./test3.cfg
time2=$(date +%s)
awk "BEGIN { print ${time2} - ${time1} }"
