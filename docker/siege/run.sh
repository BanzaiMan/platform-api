#!/usr/bin/env bash

set -o errexit
set -o nounset

docker build -t alexwlchan/siege .

docker run -v $(pwd)/siegerc:/siegerc -it alexwlchan/siege siege --rc=/siegerc https://iiif.wellcomecollection.org
