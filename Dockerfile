FROM amazoncorretto:11-alpine
#docker pull amazoncorretto:11-alpine

RUN apk add ttf-dejavu

EXPOSE 18080

COPY ./target/rebuild.jar /app/rebuild/rebuild-boot.jar
#COPY ./.deploy/SourceHanSansK-Regular.ttf /app/rebuild/.rebuild/

WORKDIR /app/rebuild/

CMD java -jar -Duser.timezone=Asia/Shanghai -DDataDirectory=/app/rebuild/.rebuild rebuild-boot.jar
