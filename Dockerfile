FROM zenika/kotlin:1.3-jdk11

RUN apt-get update
RUN apt-get install -y git
RUN mkdir /cottontaildb-data

ADD config.json /cottontaidb-data
ADD build/distributions/cottontaildb-bin.tar /

EXPOSE 1865

ENTRYPOINT /cottontaildb-bin/bin/cottontaildb /cottontail-data/config.json