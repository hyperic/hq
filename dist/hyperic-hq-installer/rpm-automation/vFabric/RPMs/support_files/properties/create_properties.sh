#!/bin/bash

# $1 = RPM decided Hyperic rrefix and HQ install dir. Default is
# /opt/hyperic/hyperic-hqee-installer. This is separate from the properties
# prefix, an issue of installation dir vs final configured dir where HQ is run
INSTALLER_DIR=$1

VF_HQ_SERVER_PROPS=/etc/vmware/vcenter/hyperic/vcenter_hyperic_server.properties
TMP_HQ_SERVER_PROPS=/tmp/vcenter_hyperic_server.properties
HQ_SERVER_PROPERTIES=install_ee.properties

# remove comments and strip any Windows EOL if they exist
cat ${VF_HQ_SERVER_PROPS} | grep -v ^# | tr -d '\r' > $TMP_HQ_SERVER_PROPS
# source new tmp properties file
. ${TMP_HQ_SERVER_PROPS}

touch ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
chmod 777 ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES

if [[ ! -e $HQ_SERVER_INSTALL_PATH ]]; then
   mkdir  $HQ_SERVER_INSTALL_PATH
fi
chown hyperic:hyperic $HQ_SERVER_INSTALL_PATH

printf "export HQ_SERVER_INSTALL_PATH=$HQ_SERVER_INSTALL_PATH\n" >> /etc/bashrc

printf "accept.eula=$HQ_ACCEPT_EULA\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
printf "server.installdir=$HQ_SERVER_INSTALL_PATH\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
printf "server.mail.sender=$HQ_SENDER_EMAIL_ADDRESS\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
printf "server.admin.username=$HQ_ADMIN_USER\n"  >> ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
printf "server.admin.password=$HQ_ADMIN_PASSWORD\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
printf "server.admin.email=$HQ_ADMIN_EMAIL_ADDRESS\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES

# Check BUILT_IN_POSTGRESQL to see if this will be a built-in db config
# Check if var exists, then if so, check its val
if [ `cat $TMP_HQ_SERVER_PROPS | grep -c BUILT_IN_POSTGRESQL` -gt "0" ] && [ `cat $TMP_HQ_SERVER_PROPS | grep BUILT_IN_POSTGRESQL | awk -F'=' '{print $2}'` = "yes" ]
then
   # Configure for the local built-in postgresql databas    
   # Replace template values in the expect script with values from the
   # properties file
   installer/data/hqdb/tune-os.sh
else 
   rm -rf installer/data/hqdb    
   printf "server.database=$HQ_DB_TYPE\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
   printf "server.database-url=$HQ_DB_URL\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
   printf "server.database-user=$HQ_DB_USERNAME\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
   printf "server.database-password=$HQ_DB_PASSWORD\n"  >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
   printf "install.profile=$HQ_SERVER_INSTALLATION_PROFILE\n" >>  ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
fi
# This is the rpm created installation user, not to be confused with the user in
# the properties file.
chown hyperic:hyperic ${INSTALLER_DIR}/$HQ_SERVER_PROPERTIES
# Clean up the tmp file
rm -f $TMP_HQ_SERVER_PROPS

