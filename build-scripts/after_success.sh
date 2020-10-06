#!/bin/bash
echo
echo "After successful build ..."
echo

cd $TRAVIS_BUILD_DIR
./gradlew sonarqube
