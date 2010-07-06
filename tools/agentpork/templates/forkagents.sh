#!/bin/bash

. etc/multiagent.properties

for ((i=0;i<$NUM_CLONES;))
do
	for ((j=0;j<5;j++))
	do
		echo "cloning agent ------------> " $i
		./create_clone.sh $i $1
		i=$(($i + 1))
		sleep 1
	done
	sleep 30
        for ((j=5;j>0;j--))
        do
		k=$(($i - $j))
                echo "stopping agent ------------> " $k
                ./stop_clone.sh $k
                sleep 1
        done
	sleep 5
done
