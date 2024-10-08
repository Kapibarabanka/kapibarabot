FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.10_7_1.10.1_3.4.2

WORKDIR /app

COPY build.sbt ./
COPY ./src ./src
COPY ./project/build.properties ./project/
COPY ./project/Dependencies.scala ./project/

COPY ./ao3-scrapper/src ./ao3-scrapper/src

RUN sbt clean compile

ENTRYPOINT ["sbt", "runMain com.kapibarabanka.kapibarabot.Application"]