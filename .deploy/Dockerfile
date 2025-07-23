FROM amazoncorretto:11-alpine

USER root

RUN apk add ttf-dejavu
RUN apk add wget

RUN mkdir -p /app/rebuild/.rebuild/
ADD ./rebuild.jar /app/rebuild/rebuild-boot.jar
ADD https://www.qn-cdn.getrebuild.com/pub/deploy/SourceHanSansK-Regular.ttf /app/rebuild/.rebuild/

EXPOSE 18080
WORKDIR /app/rebuild/
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-DDataDirectory=/app/rebuild/.rebuild", "-jar", "rebuild-boot.jar"]
