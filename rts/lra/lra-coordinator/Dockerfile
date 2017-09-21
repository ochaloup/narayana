FROM maven:latest
# FROM fabric8/java-jboss-openjdk8-jdk:1.2.3  
  
ENV JAVA_APP_JAR lra-coordinator-swarm.jar  
ENV AB_ENABLED off  
# to get more info about LRA processing in console you can add switch  
#  -Dswarm.logging=TRACE  
ENV JAVA_OPTIONS -Xmx512m  
  
EXPOSE 8080  

RUN pwd
RUN ls 
RUN cd ../../.. &&\
    ./build.sh -B  -fae -DallTests -DskipTests &&\
    cd -
RUN ls *
 
ADD target/lra-coordinator-swarm.jar /deployments/  
  
