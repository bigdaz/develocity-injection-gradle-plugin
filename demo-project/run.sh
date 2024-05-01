#!/bin/zsh

cd .. && ./gradlew clean publishAllPublicationsToLocalRepository

cd demo-project && ./gradlew -I demo.init.gradle.kts
