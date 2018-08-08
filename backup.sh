#!/bin/sh


timestamp=`date "+%Y_%m_%d-%H_%M_%S"`.js
echo "Taking a backup of the repo at $timestamp"

curl -o $timestamp https://service-catalogue-repository.herokuapp.com/backup

echo "Uploading $f"
curl -T "$timestamp" ftp://$url/SBR_TDT%40sbr.gov.au/service_catalogue_repository_backups/ --user $user:$pass

echo "Cleaning up"
rm $timestamp