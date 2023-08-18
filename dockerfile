FROM apache/spark:3.4.1-scala2.12-java11-python3-r-ubuntu
USER root
COPY target/scala-2.12/dataprocessing-assembly-0.1.0-SNAPSHOT.jar /opt/spark/work-dir
COPY start.sh /opt/spark/work-dir
VOLUME /opt/spark/work-dir/src/data

ENTRYPOINT ["/opt/spark/work-dir/start.sh"]
CMD ["-i", "src/data/0308.csv", "-o", "src/data/output/"]