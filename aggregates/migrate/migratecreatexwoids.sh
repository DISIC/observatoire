#!/bin/sh
LEN=0
if [ "$#" -gt 0 ];
then
LEN=${#1}
fi
if [ "$LEN" -eq "6" ];
then
PERIOD=$1
echo "Running script for $PERIOD"
mysql -u root xwiki -e "set @MONTH=$PERIOD; source $(dirname "$0")/avis_migrate_createxwoids.sql;"
elif [ "$LEN" -eq "4" ];
then
YEAR=$1
for MONTH in 01 02 03 04 05 06 07 08 09 10 11 12; do
PERIOD=$YEAR$MONTH
echo "Running script for $PERIOD"
mysql -u root xwiki -e "set @MONTH=$PERIOD; source $(dirname "$0")/avis_migrate_createxwoids.sql;"
done
else
echo "Missing year or month parameter"
fi
