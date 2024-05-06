FROM openjdk:11

COPY ./target/rebuild.jar /app/rebuild/rebuild-boot.jar

WORKDIR /app/rebuild/

CMD java -jar -Duser.timezone=Asia/Shanghai -DDataDirectory=/app/rebuild/.rebuild rebuild-boot.jar

EXPOSE 18080
