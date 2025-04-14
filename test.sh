#!/bin/bash


hostname=$(hostname)
time1=$(date +%s)
# Test rapid connections
for i in {1..10000}
do
  sleep 0.0001
  curl --cacert ./certs.pem http://${hostname}/fortune -d "key${i}=$time1" -H "Content-Type:application/x-www-form-urlencoded" &
done
time2=$(date +%s)
awk "BEGIN { print 10000.0/(${time2} - ${time1}) }"
