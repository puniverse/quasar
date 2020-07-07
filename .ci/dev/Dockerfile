FROM azul/zulu-openjdk:11
RUN apt-get update
ARG USER="tester"
ARG UID=999
ARG GID=999
RUN groupadd -g ${GID} -o ${USER}
RUN useradd -o -m -u ${UID} -g ${GID} ${USER}
