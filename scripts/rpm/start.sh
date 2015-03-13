#!/bin/sh
nohup java -DSHR_LOG_LEVEL=$SHR_LOG_LEVEL -jar /opt/freeshr-update/lib/freeshr-update.jar > /var/log/freeshr-update/freeshr-update.log &
echo $! > /var/run/freeshr-update/freeshr-update.pid