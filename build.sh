#!/usr/bin/env bash

set -e

command -v mvn >/dev/null 2>&1 || { echo >&2 "mvn not found."; exit 1; }

git clone -b utilities_2_0 https://github.com/MGX-metagenomics/utilities.git
cd utilities
mvn install -DskipTests=true
cd ..

git clone -b GPMS_2_0 https://github.com/MGX-metagenomics/GPMS.git
cd GPMS
mvn install -DskipTests=true
cd ..

rm -rf utilities GPMS

mvn install -DskipTests=true
