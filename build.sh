#!/usr/bin/env bash

git clone -b utilities_2_0 https://github.com/MGX-metagenomics/utilities.git
cd utilities
mvn install -DskipTests=true
cd ..

git clone -b GPMS_2_0 https://github.com/MGX-metagenomics/GPMS.git
cd GPMS
mvn install -DskipTests=true
cd ..

mvn install -DskipTests=true
