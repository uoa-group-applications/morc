#!/bin/bash

#borrowed from ReadyTalk/swt-bling

if [ "$TRAVIS_REPO_SLUG" == "uoa-group-applications/morc" ] && [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then

  mvn javadoc:javadoc
  cp -R target/site/apidocs $HOME/apidocs-latest

  cd $HOME
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/uoa-group-applications/morc gh-pages > /dev/null

  cd gh-pages
  git rm -rf ./apidocs
  cp -Rf $HOME/apidocs-latest ./apidocs
  git add -f .
  git commit -m "Adding SNAPSHOT apidocs $TRAVIS_BUILD_NUMBER to gh-pages"
  git push -fq origin gh-pages > /dev/null
  
fi