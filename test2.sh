#!/bin/bash

for i in {1..10}
do
sleep .001
./test3.sh &
done
