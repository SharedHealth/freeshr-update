#!/bin/sh

rm -f /etc/init.d/freeshr-update
rm -f /etc/default/freeshr-update
rm -f /var/run/freeshr-update

#Remove freeshr-update from chkconfig
chkconfig --del freeshr-update || true
