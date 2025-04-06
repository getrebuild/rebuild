#FROM openjdk:11-jre-slim
FROM amazoncorretto:11-alpine
RUN apk add ttf-dejavu
RUN apk add wget

RUN mkdir -p /app/rebuild/.rebuild/
RUN wget -O /app/rebuild/.rebuild/SourceHanSansK-Regular.ttf https://www.qn-cdn.getrebuild.com/pub/deploy/SourceHanSansK-Regular.ttf
RUN wget -O /app/rebuild/rebuild-boot.jar https://www.qn-cdn.getrebuild.com/pub/deploy/rebuild-boot-4.0.0-beta3.jar

EXPOSE 18080
WORKDIR /app/rebuild/
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-DDataDirectory=/app/rebuild/.rebuild", "-jar", "rebuild-boot.jar"]
