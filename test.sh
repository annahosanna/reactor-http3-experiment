#!/bin/bash

curl --cacert ./certs.pem https://gks-macbook-air.local/fortune -d "param1=value1&param2=value2" -i -H "Content-Type:application/x-www-form-urlencoded" -vvv

