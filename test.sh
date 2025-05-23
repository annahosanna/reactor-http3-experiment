#!/bin/bash

hostname=$(hostname)

# for i in {1..5}
# do
  time3=$(date +%s)
  for j in {1..5000}
    do
      sleep 0.0000001
      curl http://${hostname}/fortune -d "key=k1&value=v1" -H "Content-Type:application/x-www-form-urlencoded" 2>>./log.txt >/dev/null &
    done
  time4=$(date +%s)
  echo "Connections per second:"
  awk "BEGIN { print 5000.0/(${time4} - ${time3}) }"
  echo "TIME_WAIT"
  netstat -an | grep TIME_WAIT | wc -l
# done
