#!/bin/bash

echo "This will take a minute"

# Make test2.sh
inst=100
reps=7000
totalreps=`awk "BEGIN { print (${inst} * ${reps}) }"`".0"

cat << EOF1 > ./test2.sh
#!/bin/bash

rm ./err-log-*

time1=\$(date +%s)

for i in {1..${inst}}
do
sleep .001
curl -K ./test2.cfg 2>./err-log-\${i}.txt >/dev/null &
done
EOF1

echo "count=\`ps -ax | grep \"curl\" | wc -l | tr -d '[:blank:]'\`" >> ./test2.sh
echo 'while [ $count -gt 2 ]' >> ./test2.sh
echo 'do' >> ./test2.sh
echo '  echo "Count: $count"' >> ./test2.sh
echo "  count=\`ps -ax | grep \"curl\" | wc -l | tr -d '[:blank:]'\`" >> ./test2.sh
echo "done" >> ./test2.sh


echo "inst=${inst}" >> test2.sh
echo "reps=${reps}" >> test2.sh
echo "totalreps=${totalreps}" >> test2.sh

echo 'time2=$(date +%s)' >> ./test2.sh
echo 'singleconnection=$(awk "BEGIN { print $time2 - $time1 }")' >> ./test2.sh
echo 'rps=$(awk "BEGIN { print ${totalreps}/($time2 - $time1) }")' >> ./test2.sh
echo 'spr=`awk "BEGIN { print (${time2} - ${time1})/${totalreps} }"`' >> ./test2.sh
echo 'echo "Time to perform ${reps} request ${inst} connections::Total time: ${singleconnection}; Requests per second: ${rps}; Seconds per request: ${spr}"' >> ./test2.sh

echo 'echo "Connection errors:"' >> ./test2.sh
echo "grep \"Connection\" ./err-log-*  | wc -l | tr -d '[:blank:]'" >> ./test2.sh

echo 'echo "TIME_WAIT connections"' >> ./test2.sh
echo "netstat -an | grep TIME_WAIT | wc -l | tr -d '[:blank:]'" >> ./test2.sh
echo "rm ./err-log-*" >> ./test2.sh

# Make test2.cfg

cat << EOF2 > ./test2.cfg
cacert = ./certs.pem
data = "k1=v1&k2=\"\"&k3=&k4=%00"
# request = "POST"
header = "Content-Type:application/x-www-form-urlencoded"
connect-timeout = 1
parallel
EOF2

hostname=$(hostname)
count=1
while [ $count -lt ${reps} ]
do
  echo "url = http://${hostname}/fortune" >> ./test2.cfg
  count=$((count + 1))
done

chmod +x ./test2.sh
