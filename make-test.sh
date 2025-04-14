#!/bin/bash

cat << EOF > ./test3.cfg
cacert = ./certs.pem
data = "k1=v1&k2=\"\"&k3="
request = "POST"
header = "Content-Type:application/x-www-form-urlencoded"
parallel
EOF

hostname=$(hostname)
for i in {1..500000}
do
echo "url = https://${hostname}/fortune" >> ./test3.cfg
done
