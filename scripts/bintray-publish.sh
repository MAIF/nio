#!/usr/bin/env bash
if [ -z "$TRAVIS_TAG" ];
then
    echo 'Not a tag, just run test'

    if test "$TRAVIS_PULL_REQUEST" = "false"
    then
        CI='true' sbt -J-Xmx2G -J-Xss20M -J-XX:ReservedCodeCacheSize=128m ++$TRAVIS_SCALA_VERSION ";test;nio-server/assembly;nio-server/dist;nio-server/docker:publish"
        sbt -J-Xmx2G -J-Xss20M -J-XX:ReservedCodeCacheSize=128m ++$TRAVIS_SCALA_VERSION ";+jvm/publishLocal;+jvm/publish"
        echo "Uploading nio.jar"
        curl -T ./nio-server/target/scala-2.12/nio.jar -u${BINTRAY_USER}:${BINTRAY_PASS} -H 'X-Bintray-Publish: 1' -H 'X-Bintray-Override: 1' -H 'X-Bintray-Version: latest' -H 'X-Bintray-Package: nio.jar' https://api.bintray.com/content/maif/binaries/nio.jar/latest/nio.jar
        curl -T ./nio-server/target/universal/nio.zip -u${BINTRAY_USER}:${BINTRAY_PASS} -H 'X-Bintray-Publish: 1' -H 'X-Bintray-Override: 1' -H 'X-Bintray-Version: latest' -H 'X-Bintray-Package: nio-dist' https://api.bintray.com/content/maif/binaries/nio-dist/latest/nio-dist.zip
    else
        sbt -J-XX:ReservedCodeCacheSize=128m ++$TRAVIS_SCALA_VERSION ";test"
    fi
else
echo "Tag ${TRAVIS_TAG}, Publishing client"
    sbt -J-Xmx2G -J-Xss20M -J-XX:ReservedCodeCacheSize=128m ++$TRAVIS_SCALA_VERSION ";nio-server/assembly;nio-server/dist;nio-server/docker:publish"
    sbt -J-Xmx2G -J-Xss20M -J-XX:ReservedCodeCacheSize=128m ++$TRAVIS_SCALA_VERSION "+publish"
    echo "Uploading nio.jar"
    curl -T ./nio-server/target/scala-2.12/nio.jar -u${BINTRAY_USER}:${BINTRAY_PASS} -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" -H "X-Bintray-Version: latest" -H "X-Bintray-Package: nio.jar" https://api.bintray.com/content/maif/binaries/nio.jar/latest/nio.jar
    curl -T ./nio-server/target/universal/nio.zip -u${BINTRAY_USER}:${BINTRAY_PASS} -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" -H "X-Bintray-Version: latest" -H "X-Bintray-Package: nio-dist" https://api.bintray.com/content/maif/binaries/nio-dist/latest/nio-dist.zip

    BINARIES_VERSION=`echo "${TRAVIS_TAG}" | cut -d "v" -f 2`
    curl -T ./nio-server/target/scala-2.12/nio.jar -u${BINTRAY_USER}:${BINTRAY_PASS} -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" -H "X-Bintray-Version: ${BINARIES_VERSION}" -H "X-Bintray-Package: nio.jar" https://api.bintray.com/content/maif/binaries/nio.jar/${BINARIES_VERSION}/nio.jar
    curl -T ./nio-server/target/universal/nio.zip -u${BINTRAY_USER}:${BINTRAY_PASS} -H "X-Bintray-Publish: 1" -H "X-Bintray-Override: 1" -H "X-Bintray-Version: ${BINARIES_VERSION}" -H "X-Bintray-Package: nio-dist" https://api.bintray.com/content/maif/binaries/nio-dist/${BINARIES_VERSION}/nio-dist.zip
fi

