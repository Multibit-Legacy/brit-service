#!/bin/bash
# Synchronize BRIT backups from the hamburg server (217.172.188.142) to local
#
# This script assumes:
#   + you have the password for the user jim to the hamburg server
#   + you have the server 'hamburg' setup in your ~/.ssh/config file something like:
# Host hamburg
# HostName 217.172.188.142
#        User jim
#        Port 8231
#
# Create the local directory to which backups will be copied
mkdir /var/brit/hamburg
#
# Copy the BRIT backup files from hamburg to local
rsync -avz --progress jim@hamburg:/var/brit/matcher/backup /var/brit/hamburg