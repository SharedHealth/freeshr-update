#!/bin/sh

ln -s /opt/freeshr-update/bin/freeshr-update /etc/init.d/freeshr-update
ln -s /opt/freeshr-update/etc/freeshr-update /etc/default/freeshr-update
ln -s /opt/freeshr-update/var /var/run/freeshr-update

if [ ! -e /var/log/freeshr-update ]; then
    mkdir /var/log/freeshr-update
fi

# Add freeshr-update service to chkconfig
chkconfig --add freeshr-update