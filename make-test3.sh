#!/bin/bash

echo "This will take just a minute"

reps=500000
repsdec=${reps}".0"
echo "#!/bin/bash" > ./test3.sh
echo "" >> ./test3.sh
echo 'time1=$(date +%s)' >> ./test3.sh
echo 'curl -K ./test3.cfg 2>./err-log-0.txt >/dev/null' >> ./test3.sh
echo 'time2=$(date +%s)' >> ./test3.sh
echo 'singleconnection=`awk "BEGIN { print ${time2} - ${time1} }"`' >> ./test3.sh
echo 'rps=`awk "BEGIN { print '${repsdec}'/(${time2} - ${time1}) }"`' >> ./test3.sh
echo 'spr=`awk "BEGIN { print (${time2} - ${time1})/'${repsdec}' }"`' >> ./test3.sh
echo 'echo "Time to perform '${reps}' request on a single connection: ${singleconnection}. Requests per second: ${rps}. Seconds per request: ${spr}."' >> ./test3.sh

echo 'echo "Connection errors:"' >> ./test3.sh
echo "grep \"Connection\" ./err-log-0.txt  | wc -l | tr -d '[:blank:]'" >> ./test3.sh

echo 'echo "TIME_WAIT connections"' >> ./test3.sh
echo "netstat -an | grep TIME_WAIT | wc -l | tr -d '[:blank:]'" >> ./test3.sh

cat << EOF > ./test3.cfg
cacert = ./certs.pem
data = "k1=v1&k2=\"\"&k3=&k4=%00"
request = "POST"
header = "Content-Type:application/x-www-form-urlencoded"
parallel
EOF

hostname=$(hostname)
count=1
while [ $count -lt ${reps} ]
do
  echo "url = http://${hostname}/fortune" >> ./test3.cfg
  count=$((count + 1))
done

chmod +x ./test3.sh
