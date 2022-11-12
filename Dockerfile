
FROM gradle:7-jdk11 as cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle.kts /home/gradle/java-code/
COPY gradle.properties /home/gradle/java-code
WORKDIR /home/gradle/java-code
RUN gradle clean build -i --stacktrace

FROM gradle:7-jdk11 as builder
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY . /src
WORKDIR /src
RUN gradle buildFatJar -i --stacktrace

FROM openjdk:11-slim

WORKDIR /app
COPY --from=builder /src/nodes ./nodes
COPY --from=builder /src/build/libs/*jar ./app.jar
ENTRYPOINT ["java","-jar","app.jar"]