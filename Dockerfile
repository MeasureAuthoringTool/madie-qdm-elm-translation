## Use AWS AL2 + Corretto base image
FROM amazoncorretto:17.0.10-al2023-headless

COPY target/*.jar app.jar

## Update Packages
RUN yum update -y --security

## Download New Relic
RUN curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.jar \
    && curl -O https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic.yml

ENTRYPOINT ["java","-Dspring.profiles.active=docker","-javaagent:newrelic.jar","-jar","/app.jar"]
