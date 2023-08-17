FROM amazoncorretto:20.0.2-al2023-generic
COPY target/scala-2.13/dataprocessing-assembly-0.1.0-SNAPSHOT.jar /app/
COPY start.sh /app/
VOLUME /src/data

ENTRYPOINT ["/app/start.sh"]
CMD ["-i", "src/data/0308.csv", "-o", "src/data/output/"]