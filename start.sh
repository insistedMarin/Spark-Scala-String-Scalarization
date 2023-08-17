#!/bin/sh

# config
chmod -R 777 /src/data

# run java
java --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED -jar /app/dataprocessing-assembly-0.1.0-SNAPSHOT.jar "$@"
