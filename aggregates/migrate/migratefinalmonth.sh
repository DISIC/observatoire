#!/bin/sh
PERIOD=202213
echo "Running script for $PERIOD"
mysql -u root xwiki -e "set @MONTH=$PERIOD; source $(dirname "$0")/avis_migrate_finalmonth.sql;"
mysql -u root xwiki -e "set @MONTH=$PERIOD; source $(dirname "$0")/avis_migrate_bymonth.sql;"
