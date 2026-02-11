# -----------------------
# 2) RUNTIME (Azul Zulu)
# -----------------------
FROM azul/zulu-openjdk-alpine:21
WORKDIR /opt/app

COPY target/scala-3.3.7/*assembly*.jar app.jar

EXPOSE 2575
ENTRYPOINT ["java","-jar","app.jar"]



## 1) BUILD (gera o assembly jar)
#FROM sbtscala/scala-sbt:eclipse-temurin-21.0.3_9_1.10.0_3.4.2 AS build
#WORKDIR /app
#
#COPY build.sbt ./
#COPY project ./project
#COPY src ./src
#
#RUN sbt -batch clean assembly
#
## 2) RUNTIME
#FROM eclipse-temurin:21-jre
#WORKDIR /opt/app
#
#COPY --from=build C:/Users/lilia/IdeaProjects/HealthDataPlatform/healthdatahl7v2conector/target/scala-3.3.7/HealthDataHl7v2Conector-assembly-0.1.0-SNAPSHOT.jar app.jar
#
#EXPOSE 2575
#ENTRYPOINT ["java","-jar","app.jar"]
