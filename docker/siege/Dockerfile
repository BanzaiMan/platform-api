FROM ubuntu:14.04

# Install Siege following instructions from
# https://www.linode.com/docs/tools-reference/tools/load-testing-with-siege
RUN sudo apt-get update && sudo apt-get upgrade -y --show-upgraded
RUN apt-get install -y wget
RUN wget http://download.joedog.org/siege/siege-latest.tar.gz
RUN tar -zxvf siege-latest.tar.gz
RUN sudo apt-get install -y build-essential
RUN apt-get install -y libssl-dev
RUN cd siege-*/ && ./configure --prefix=/usr/local --with-ssl=/usr/bin/openssl && make && make install
