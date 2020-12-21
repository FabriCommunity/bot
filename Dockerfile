FROM openjdk:15-jdk-slim

COPY build/libs/community-bot-*-all.jar /usr/local/lib/community-bot.jar

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "/usr/local/lib/community-bot.jar"]
