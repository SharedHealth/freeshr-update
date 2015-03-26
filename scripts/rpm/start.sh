#!/bin/sh
nohup java -DSHR_UPDATE_LOG_LEVEL=$SHR_UPDATE_LOG_LEVEL -jar /opt/freeshr-update/lib/freeshr-update.jar >  /dev/null 2>&1 &
echo $! > /var/run/freeshr-update/freeshr-update.pid