#!/bin/bash

hostname=$(hostname)
time1=$(date +%s)
echo "Start time: ${time1}"
# Test rapid connections
for i in {1..10000}
do
  sleep 0.000001
  curl --cacert ./certs.pem http://${hostname}/fortune -d "key${i}=${time1}" -H "Content-Type:application/x-www-form-urlencoded" -o /dev/null > /dev/null 2>&1 &
done
time2=$(date +%s)
echo "End time (estimated): ${time2}"
echo "Connections per second:"
awk "BEGIN { print 10000.0/(${time2} - ${time1}) }"
