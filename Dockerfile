FROM openjdk:11
VOLUME /tmp
EXPOSE 8018
ADD ./target/savingtransaction-0.0.1-SNAPSHOT.jar savingtransaction.jar
ENTRYPOINT ["java","-jar","/savingtransaction.jar"]