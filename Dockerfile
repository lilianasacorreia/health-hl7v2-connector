# -----------------------
# 2) RUNTIME (Azul Zulu)
# -----------------------
FROM azul/zulu-openjdk-alpine:21
WORKDIR /opt/app

COPY target/scala-3.3.7/*assembly*.jar app.jar

EXPOSE 2575
ENTRYPOINT ["java","-jar","app.jar"]
