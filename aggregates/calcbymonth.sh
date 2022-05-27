#!/bin/sh
YEAR=$1
for MONTH in 01 02 03 04 05 06 07 08 09 10 11 12; do
echo $MONTH;
mysql -u root xwiki -e "set @MONTH=$YEAR$MONTH; source $(dirname "$0")/calc.sql;"
done
