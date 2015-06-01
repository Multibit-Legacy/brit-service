#!/bin/bash
# Create a backup of the BRIT matcher store 
# All files in the store directory are added to a datestamped zip file
# The backup file will typically be: backup-2015-06-01-17-43-02.zip
# and will be stored in a directory /var/brit/matcher/backup
mkdir /var/brit/matcher/backup
echo Creating zip file containing contents of /var/brit/matcher/store...
FILENAME="/var/brit/matcher/backup/backup-`date '+%Y-%m-%d-%H-%M-%S'`.zip" 
zip -r $FILENAME  /var/brit/matcher/store
echo
echo Backup created:
ls -l $FILENAME
