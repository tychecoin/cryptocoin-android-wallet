#!/usr/bin/env bash

set -e

source script/env.sh

cd $EXTERNAL_LIBS_BUILD_ROOT

url="https://github.com/swap-dev/swap"
version="swap-android"

if [ ! -d "swap" ]; then
  git clone ${url} -b ${version}
  cd swap
  git submodule update --recursive --init
else
  cd swap
  git checkout ${version}
  git pull
  git submodule update --recursive --init
fi
