#!/bin/bash
start=$(date +%s)

if [[ ${JAVA_VER} == "18" ]]; then
    ls -al /usr/lib/jvm
    sudo apt-get -y remove --purge oracle-java8-installer*
    sudo rm -rf /usr/lib/jvm/jdk1.8.0* /usr/lib/jvm/java-8-oracle* /usr/lib/jvm/.java-8-oracle* /usr/lib/jvm/default-java
    ls -al /usr/lib/jvm
    sudo apt-get -y update
    sudo apt-get -y install oracle-java8-installer
    sudo ln -s /usr/lib/jvm/java-8-oracle /usr/lib/jvm/default-java
    ls -al /usr/lib/jvm
fi

end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))

echo "Pre-build process finished in $minutes minute(s) and $seconds seconds"
