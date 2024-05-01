#!/bin/sh

cd .. && ./gradlew clean publishAllPublicationsToLocalRepository

cd demo-project

mkdir -p build
rm build/configure-develocity.gradle
echo 'initscript { repositories { maven { url = "../build/local-repo" } } }' >> build/configure-develocity.gradle
echo '' >> build/configure-develocity.gradle
cat ../reference/configure-develocity.gradle >> build/configure-develocity.gradle

./gradlew -I build/configure-develocity.gradle \
    -Ddevelocity.url=https://ge.solutions-team.gradle.com/ \
    -Ddevelocity.injection-enabled=true \
    -Ddevelocity.plugin.version=3.17.2 \
    -Ddevelocity.ccud-plugin.version=2.0.1
