#!/bin/bash

hostname=$(hostname)
time=$(date +%s)
# Test rapid connections
for i in {1..10000}
do
curl --cacert ./certs.pem http://${hostname}/fortune -d "key${i}=$time" -H "Content-Type:application/x-www-form-urlencoded" &
done
