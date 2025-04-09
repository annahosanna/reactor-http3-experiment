#!/bin/bash

# Test rapid connections
for i in {1..10000}
do
time=$(date +%s)
curl --cacert ./certs.pem http://gks-macbook-air.local/fortune -d "key${i}=$time" -H "Content-Type:application/x-www-form-urlencoded" &
done
