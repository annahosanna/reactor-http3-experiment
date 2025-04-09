#!/bin/bash

# Warmup
for i in {1..5}
do
sleep 1
curl --cacert ./certs.pem http://gks-macbook-air.local/fortune -d "param${i}=value${i}" -i -H "Content-Type:application/x-www-form-urlencoded"
done
echo "--- Starting ---"
# Attempt a whole bunch of requests
for i in {1..10000}
do
sleep 0.0025
curl -s --cacert ./certs.pem http://gks-macbook-air.local/fortune -d "param${i}=value${i}" -i -H "Content-Type:application/x-www-form-urlencoded" >/dev/null &
done
