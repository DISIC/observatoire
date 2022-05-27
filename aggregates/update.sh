#!/bin/sh
mysql -u root xwiki < $(dirname "$0")/update.sql
