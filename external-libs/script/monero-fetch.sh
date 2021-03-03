#!/usr/bin/env bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT

url="https://github.com/gonzothedev/crypto"
version="cryptocoin-android"

if [ ! -d "cryptocoin" ]; then
  git clone ${url} -b ${version}
  cd cryptocoin
  git submodule update --recursive --init
else
  cd cryptocoin
  git checkout ${version}
  git pull
  git submodule update --recursive --init
fi
