#!/bin/sh

chmod -R 777 /opt/spark/work-dir/src/data

# Use spark-submit to run the Spark application
/opt/spark/bin/spark-submit \
  --class Main \
  --master local[*] \
  /opt/spark/work-dir/dataprocessing-assembly-0.1.0-SNAPSHOT.jar \
  "$@"