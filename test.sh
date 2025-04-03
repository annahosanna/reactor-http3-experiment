#!/bin/bash

#curl --cacert ./certs.pem https://gks-macbook-air.local/fortune -d "param0&param1=value1&param2=value2&&==&param3==value3=value4&value5&" -i -H "Content-Type:application/x-www-form-urlencoded" -vvv
#sleep 1
#curl --cacert ./certs.pem https://gks-macbook-air.local/fortune -d "param1=value1" -i -H "Content-Type:application/x-www-form-urlencoded" -vvv
#sleep 1
#curl --cacert ./certs.pem https://gks-macbook-air.local/fortune -d "param0" -i -H "Content-Type:application/x-www-form-urlencoded" -vvv
#sleep 1
for i in {1..1000}
do
curl --cacert ./certs.pem https://gks-macbook-air.local/fortune -d "param${i}=value1" -i -H "Content-Type:application/x-www-form-urlencoded" -vvv &
done
