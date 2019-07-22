
FROM ubuntu:18.04 AS builder

LABEL version="1.0"
LABEL maintainer="sebastian@jaenicke.org"

# build dependencies
RUN DEBIAN_FRONTEND=noninteractive apt-get -y update && apt-get -y install \
    git openjdk-8-jdk maven protobuf-compiler

RUN groupadd mgxserv && useradd -m -g mgxserv mgxserv && chown -R mgxserv /home/mgxserv

COPY . /tmp/
RUN cd /tmp/ && ./build.sh && mkdir -p /vol/mgx-sw/lib/annotationclient && cp -va target/*jar /vol/mgx-sw/lib/annotationclient && \
    mkdir -p /vol/mgx-sw/bin 

RUN cp AnnotationClient /vol/mgx-sw/bin
RUN ln -s /vol/mgx-sw/bin/AnnotationClient /vol/mgx-sw/bin/SeqRunFetcher


FROM ubuntu:18.04 

RUN ln -fs /usr/share/zoneinfo/Europe/Berlin /etc/localtime

# runtime dependencies
RUN apt-get -y update && apt-get -y install openjdk-8-jre-headless

RUN groupadd mgxserv && useradd -m -g mgxserv mgxserv && chown -R mgxserv /home/mgxserv

COPY --from=builder /vol/mgx-sw/ /vol/mgx-sw

ENV PATH="/vol/mgx-sw/bin:${PATH}"

USER mgxserv
