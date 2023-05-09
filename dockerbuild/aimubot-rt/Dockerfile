FROM aimubot-rt:openjre8-v1

MAINTAINER Inari Aimu

VOLUME ["/aimubot-rt/"]

WORKDIR "/aimubot-rt/"

ENTRYPOINT ["java", "-jar", "aimubot-1.0-SNAPSHOT-all.jar"]

ENV JAVA_HOME /usr/lib/jvm/default-jvm
ENV PATH ${PATH}:${JAVA_HOME}/bin
