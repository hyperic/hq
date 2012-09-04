%define HQ_Component_Name       hyperic-hqee-installer
%define HQ_Component_Version    @hq.version@
%define HQ_Component_Edition	EE
%define HQ_Component_Build	@hq.ee.build.installer@-x86-64-linux
%define HQ_Component_Release   	1 
%define HQ_Component_Build_Type @hq.build.type@

%define HQ_User			hyperic
%define HQ_Group		hyperic
%define HQ_User_Home		/opt/hyperic

%define HQ_SERVER_PROPERTIES_FILE	vfabric_hyperic_server.properties
%define HQ_SERVER_PROPERTIES_DIR	/etc/vmware/vfabric/hyperic


AutoReqProv:    no

# Requires Sun's Java, which must currently be downloaded directly from Sun
# at http://java.sun.com.
Requires:	expect, vfabric-eula

Name:           vfabric-hyperic-server
Version:        %{HQ_Component_Version}.%{HQ_Component_Edition}
Release:        %{HQ_Component_Release}
Summary:        VMware vFabric Hyperic Server
Source0:        %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build}.tar.gz
Vendor:		VMware, Inc.
License:        Commercial
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root
Group:          Applications/Monitoring
Prefix:		%{HQ_User_Home}
Url: 		http://www.vmware.com/products/vfabric-hyperic/overview.html
ExclusiveArch:	x86_64
ExclusiveOS:	linux

%description

Server for the vFabric Hyperic HQ systems management system.

%prep

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%setup -T -b 0 -n %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build_Type}

%pre

eula_file="/etc/vmware/vfabric/accept-vfabric-eula.txt"
eula_file_text="I_ACCEPT_EULA_LOCATED_AT"
eula_url="http://www.vmware.com/download/eula/vfabric_app-platform_eula.html"

if [ ! -f "$eula_file" ]
then
   echo
   echo "Cancelling install."
   echo "vFabric EULA acceptance file, $eula_file, not found."
   echo "Please install or reinstall vfabric-eula RPM file."
   exit 1
fi

if [ `grep -c "${eula_file_text}=${eula_url}" "$eula_file"` -eq "0" ]
then
   echo
   echo "Cancelling install."
   echo "Invalid vFabric EULA acceptance file, $eula_file."
   echo "Please install or reinstall vfabric-eula RPM file."
   exit 1
fi

# If hq-server is already installed and running (whether installed by RPM
# or not), then kill it, but remember that it was running.
%{__rm} -f /tmp/hyperic-hq-server-was-running-%{version}-%{release}
if [ -f /etc/init.d/hyperic-hq-server ]; then
    /sbin/service hyperic-hq-server stop > /dev/null 2> /dev/null
    touch /tmp/hyperic-hq-server-was-running-%{version}-%{release}
elif [ -f %{prefix}/server-current ]; then
    %{prefix}/server-current/bin/hq-server.sh stop
fi

#
# Create a user and group if need be
#
if [ ! -n "`/usr/bin/getent group %{HQ_Group}`" ]; then
    # One would like to default the GID, but doing that properly would
    # require thought.
    %{_sbindir}/groupadd %{HQ_Group} 2> /dev/null
fi
if [ ! -n "`/usr/bin/getent passwd %{HQ_User}`" ]; then
    # One would also like to default the UID, but doing that properly would
    # also require thought.
    %{__mkdir} -p -m 755 %{HQ_User_Home}
    %{_sbindir}/useradd -g %{HQ_Group} -d %{HQ_User_Home} %{HQ_User} 2> /dev/null
    chown -R %{HQ_User}.%{HQ_Group} %{HQ_User_Home}
else
    %{__mkdir} -p -m 755 %{prefix}
    chown %{HQ_User}.%{HQ_Group} %{prefix}
fi

exit 0

%preun

# If hq-server is already installed and running (whether installed by RPM
# or not), then kill it, but remember that it was running.
if [ -f /etc/init.d/hyperic-hq-server ]; then
    /sbin/service hyperic-hq-server stop > /dev/null 2> /dev/null
fi
chkconfig --del hyperic-hq-server

%build


%install

%{__install} -d -m 755 $RPM_BUILD_ROOT/etc/init.d
%{__install} -d -m 755 $RPM_BUILD_ROOT%{prefix}/%{HQ_Component_Name}
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/hq-plugins
%{__install} -m 755 rcfiles/hyperic-hq-server.init.rh $RPM_BUILD_ROOT/etc/init.d/hyperic-hq-server

%{__rm} -f installer/lib/sigar-x86-winnt.lib
%{__rm} -f rpm.spec
%{__rm} -f setup.bat
%{__mv} -f * $RPM_BUILD_ROOT/%{prefix}/%{HQ_Component_Name}

#echo "Place custom plug-ins in this directory." > $RPM_BUILD_ROOT/%{prefix}/hq-plugins/README

%clean

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%post

#
# Setup the HQ Server using the Hyperic installer process
#

if [ -d /opt/hyperic/server-current ] && cd /opt/hyperic/server-current; then
   cd %{prefix}/%{HQ_Component_Name}
   /bin/su hyperic -c "expect/upgrade.exp %{prefix}/server-current %{prefix}"
   cd $RPM_BUILD_ROOT/%{prefix}
   /bin/su hyperic -c "/bin/ln -snf server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition} server-current"
else
   cd %{prefix}/%{HQ_Component_Name}
   #installer/data/hqdb/tune-os.sh > /dev/null 2> /dev/null
   # if an answer file exists, use it to populate the expect script to
   # configure HQ
   if [ -f %{HQ_SERVER_PROPERTIES_DIR}/%{HQ_SERVER_PROPERTIES_FILE} ]; then
      /bin/su hyperic -c "%{prefix}/%{HQ_Component_Name}/expect/create_exp.sh %{prefix}/%{HQ_Component_Name}" > /dev/null 2>&1
      /bin/su hyperic -c "%{prefix}/%{HQ_Component_Name}/install_ee.exp"
      cd $RPM_BUILD_ROOT/%{prefix}
      if [ -d %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition} ]
      then
         /bin/su hyperic -c "/bin/ln -snf server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition} server-current"
         %{__cp} %{prefix}/%{HQ_Component_Name}/installer/logs/hq-install.log %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}/hq-install.log 
         # copy the properties file into the setup dir for future ease of use
         %{__cp} %{prefix}/%{HQ_Component_Name}/expect/%{HQ_SERVER_PROPERTIES_FILE} %{prefix}/%{HQ_Component_Name}/
         %{__chown} hyperic:hyperic %{prefix}/%{HQ_Component_Name}/%{HQ_SERVER_PROPERTIES_FILE}
      fi
      # BHester 2012.02.13 - 465 unpacks to a different directory name, but it could just be snapshot speicific
      # so I will leave the above in place also
      if [ -d %{prefix}/server-%{HQ_Component_Version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition} ]
      then
         /bin/su hyperic -c "/bin/ln -snf server-%{HQ_Component_Version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition} server-current"
         %{__cp} %{prefix}/%{HQ_Component_Name}/installer/logs/hq-install.log %{prefix}/server-%{HQ_Component_Version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}/hq-install.log 
         # copy the properties file into the setup dir for future ease of use
         %{__cp} %{prefix}/%{HQ_Component_Name}/expect/%{HQ_SERVER_PROPERTIES_FILE} %{prefix}/%{HQ_Component_Name}/
         %{__chown} hyperic:hyperic %{prefix}/%{HQ_Component_Name}/%{HQ_SERVER_PROPERTIES_FILE}
      fi
   else
      echo 
      echo "Response file, %{HQ_SERVER_PROPERTIES_DIR}/%{HQ_SERVER_PROPERTIES_FILE}"
      echo "not found. HQ will be installed, but not configured."
      echo 
      echo "Please login as user %{HQ_User} and run:"
      echo "%{prefix}/%{HQ_Component_Name}/setup.sh"
      echo "to interactively configure HQ."
      echo 
      echo "Arguments to setup.sh determine the installation mode. The arguements are:"
      echo
      echo 'none 	Quick install; the Hyperic components you choose to install will'
      echo '	be installed with default settings for most configuration options---you'
      echo '	supply installation directories only. If you install the server, it will'
      echo '	be configured to use its built-in PostgreSQL database.'
      echo '	This is the quickest way to install Hyperic for evaluation purposes.'
      echo
      echo '-full 	Full install; installer will prompt you to supply values for all'
      echo '	installation options.'
      echo
      echo '-upgrade 	Server upgrade only; installer will prompt you for the path of'
      echo '	the Hyperic server to upgrade.' 
      echo
      echo '-postgresql 	Quick install when using a standalone (not the Hyperic'
      echo '	built-in) PostgreSQL database; installer will prompt you for database'
      echo '	connection information and use defaults for other configuration settings.'
      echo
      echo '-oracle 	Quick install mode for Oracle; installer will prompt you for'
      echo '	database connection information and use defaults for other configuration'
      echo '	settings.'
      echo
      echo '-mysql 	Quick install mode for MySQL; installer will prompt you for'
      echo '	database connection information and take defaults for everything else.' 
      echo 
      echo 
      echo "vFabric HQ may also be configured by a properties file either at RPM"
      echo "install or post RPM install."
      echo
      echo "To configure HQ from a properties file at RPM install, edit:"
      echo "%{prefix}/%{HQ_Component_Name}/%{HQ_SERVER_PROPERTIES_FILE}."
      echo "Place that file in:"
      echo "%{HQ_SERVER_PROPERTIES_DIR}/ and install the vfabric-hyperic-server RPM."
      echo
      echo "To configure HQ from a properties file after RPM install, edit:"
      echo "%{prefix}/%{HQ_Component_Name}/%{HQ_SERVER_PROPERTIES_FILE}."
      echo "Place that file in:"
      echo "%{HQ_SERVER_PROPERTIES_DIR}/ and run"
      echo "%{prefix}/%{HQ_Component_Name}/setup_from_properties_file.sh"
      echo "as either user root or user %{HQ_User}." 
      echo 
      echo "The properties file supports the equivalent configuration modes of the"
      echo 'above setup.sh arguments of: none, -postgresql, -oracle, and -mysql.'
      echo
      echo "For interactive setup, setup.sh must be run as user %{HQ_User}."
      echo
      %{__cp} %{prefix}/%{HQ_Component_Name}/expect/%{HQ_SERVER_PROPERTIES_FILE} %{prefix}/%{HQ_Component_Name}/
      %{__chown} hyperic:hyperic %{prefix}/%{HQ_Component_Name}/%{HQ_SERVER_PROPERTIES_FILE}
      %{__cp} %{prefix}/%{HQ_Component_Name}/expect/setup_from_properties_file.sh %{prefix}/%{HQ_Component_Name}/
      %{__chown} hyperic:hyperic %{prefix}/%{HQ_Component_Name}/setup_from_properties_file.sh
   fi
fi

%postun

if [ ! -f /etc/init.d/hyperic-hq-server ]; then
	%{__rm} -Rf %{prefix}/server-current %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}
fi

%posttrans

# use the location of the log file to determine if HQ was configured vs just
# having the files dropped
if [ -f %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}/hq-install.log ]; then
   if [ -f /etc/init.d/hyperic-hq-server ]; then
      chkconfig --add hyperic-hq-server
      chkconfig hyperic-hq-server on
   fi
   if [ -f /etc/init.d/hyperic-hq-server ] && [ -f /tmp/hyperic-hq-server-was-running-%{version}-%{release} ]; then
      /sbin/service hyperic-hq-server start > /dev/null 2> /dev/null
      %{__rm} -f /tmp/hyperic-hq-server-was-running-%{version}-%{release}
      echo
      echo "The new version of HQ Server has been started using your existing configuration."
      echo "The installation log can be found in %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}/hq-install.log."
      echo
      echo "Manually starting the HQ Server should be done using the hyperic user."
      echo
      echo "The HQ server may also be started with this init script: /etc/init.d/hyperic-hq-server"
      echo
   elif [ -f /etc/init.d/hyperic-hq-server ]; then
      echo
      echo "The HQ Server has successfully been installed, and the service has been"
      echo "configured to start at boot."
      echo "The installation log can be found in %{prefix}/server-%{version}.%{HQ_Component_Build_Type}-%{HQ_Component_Edition}/hq-install.log."
      echo
      echo "Manually starting the HQ Server should be done using the hyperic user."
      echo
      echo "The HQ server may also be started with this init script: /etc/init.d/hyperic-hq-server"
      echo
   fi
else
   if [ -f /etc/init.d/hyperic-hq-server ]; then
      chkconfig --add hyperic-hq-server
   fi
fi

exit 0

%files

%defattr (-, root, root)
/etc/init.d/hyperic-hq-server
%defattr (-, %{HQ_User}, %{HQ_Group})
%{prefix}/%{HQ_Component_Name}
%config %{prefix}/hq-plugins

