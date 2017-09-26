FROM openjdk:8
  
EXPOSE 8080  

ADD target/lra-coordinator-swarm.jar /opt
CMD ["java", "-Xmx512m",  "-jar", "/opt/lra-coordinator-swarm.jar", "-Dswarm.logging=TRACE", "-Dswarm.transactions.object-store-path=../../data"]
