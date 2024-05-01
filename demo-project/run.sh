#!/bin/sh

cd .. && ./gradlew clean publishAllPublicationsToLocalRepository

cd demo-project

./gradlew -I ../reference/configure-develocity.gradle \
    -Ddevelocity.url=https://ge.solutions-team.gradle.com/ \
    -Ddevelocity.injection-enabled=true \
    -Ddevelocity.plugin.version=3.17.2 \
    -Ddevelocity.ccud-plugin.version=2.0.1
