#!/bin/zsh

cd .. && ./gradlew clean publishAllPublicationsToLocalRepository

cd demo-project

./gradlew -I demo.init.gradle.kts \
    -Ddevelocity.url=https://ge.solutions-team.gradle.com/ \
    -Ddevelocity.injection-enabled=true \
    -Ddevelocity.plugin.version=3.17.2
