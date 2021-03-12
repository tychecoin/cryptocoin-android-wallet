#!/usr/bin/env bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT

url="https://github.com/tychecoin/crypto"
version="crypto-android"

if [ ! -d "crypto" ]; then
  git clone ${url} -b ${version}
  cd crypto
  git submodule update --recursive --init
else
  cd crypto
  git checkout ${version}
  git pull
  git submodule update --recursive --init
fi
