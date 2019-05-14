#!/bin/sh

url=$1
id=$2
credentials=$3


curl -X DELETE $1/repository/service/$2 --user $3
