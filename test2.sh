#!/bin/bash

for i in {1..4}
do
sleep .01
./test3.sh &
done
