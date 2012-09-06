%define HQ_Component_Name       hyperic-hqee-agent 
%define HQ_Component_Version    @hq.version@
%define HQ_Component_Edition	EE
%define HQ_Component_Build	@hq.ee.build.agent@-noJRE
%define HQ_Component_Release   	1
%define HQ_Component_Build_Type @hq.build.type@

%define HQ_User			hyperic
%define HQ_Group		hyperic
%define HQ_User_Home		/opt/hyperic

AutoReqProv:    no

# Requires Sun's Java, which must currently be downloaded directly from Sun
# at http://java.sun.com.
#Requires:	j2re

Name:           vfabric-hyperic-agent
Version:        %{HQ_Component_Version}.%{HQ_Component_Edition}
Release:        %{HQ_Component_Release}
Summary:        VMware vFabric Hyperic Agent
Source0:        %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build}.tar.gz
Vendor:		VMware, Inc.
License:        Commercial
BuildRoot:      %{_tmppath}/%{HQ_Component_Name}-%{version}-%{release}-root
Group:          Applications/Monitoring
Prefix:		%{HQ_User_Home}
Url: 		http://www.vmware.com/products/vfabric-hyperic/
BuildArch:	noarch

Requires:	vfabric-eula

%description

Agent for the vFabric Hyperic HQ systems management system.

%prep

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%setup -T -D -b 0 -n %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build_Type}

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


# If hq-agent is already installed and running (whether installed by RPM
# or not), then kill it, but remember that it was running.
%{__rm} -f /tmp/%{HQ_Component_Name}-was-running-%{version}-%{release}
if [ -f /etc/init.d/%{HQ_Component_Name} ]; then
    /sbin/service %{HQ_Component_Name} stop > /dev/null 2> /dev/null
    touch /tmp/%{HQ_Component_Name}-was-running-%{version}-%{release}
fi
if [ -d %{prefix}/%{HQ_Component_Name} ]; then
    cd %{prefix}/%{HQ_Component_Name} && %{__rm} -Rf data log pdk tmp
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

# If hq-agent is already installed and running (whether installed by RPM
# or not), then kill it, but remember that it was running.
if [ -f /etc/init.d/%{HQ_Component_Name} ]; then
    /sbin/service %{HQ_Component_Name} stop > /dev/null 2> /dev/null
fi
chkconfig --del %{HQ_Component_Name}

%build

%install

# Current pwd is ${RPM_BUILD_ROOT}/%{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build_Type}
%{__install} -d -m 755 $RPM_BUILD_ROOT/etc/init.d
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/%{HQ_Component_Name}
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/hq-plugins
%{__install} -m 755 rcfiles/%{HQ_Component_Name}.init.rh $RPM_BUILD_ROOT/etc/init.d/%{HQ_Component_Name}

# clean up files not related to linux
%{__rm} -f bin/hq-agent.bat
%{__rm} -f bundles/agent-%{HQ_Component_Version}.%{HQ_Component_Build_Type}/background.bat
%{__rm} -rf bundles/agent-%{HQ_Component_Version}.%{HQ_Component_Build_Type}/rcfiles
%{__rm} -f bundles/agent-%{HQ_Component_Version}.%{HQ_Component_Build_Type}/pdk/lib/sigar-x86-winnt.lib
%{__rm} -f bundles/agent-%{HQ_Component_Version}.%{HQ_Component_Build_Type}/bin/*.bat
%{__rm} -f wrapper/sbin/wrapper-windows-x86-32.exe
%{__rm} -f wrapper/sbin/wrapper-aix-ppc-64
%{__rm} -f wrapper/sbin/wrapper-aix-ppc-32
%{__rm} -f wrapper/sbin/wrapper-macosx-universal-32
%{__rm} -f wrapper/sbin/wrapper-hpux-parisc-32
%{__rm} -f wrapper/sbin/wrapper-hpux-parisc-64
%{__rm} -f wrapper/sbin/wrapper-solaris-x86-32
%{__rm} -f wrapper/sbin/wrapper-solaris-sparc-32
%{__rm} -f wrapper/sbin/wrapper-solaris-sparc-64
%{__rm} -f wrapper/sbin/wrapper-freebsd-x86-32
%{__rm} -f wrapper/lib/libwrapper-aix-ppc-32.a
%{__rm} -f wrapper/lib/libwrapper-aix-ppc-64.a
%{__rm} -f wrapper/lib/libwrapper-freebsd-x86-32.so
%{__rm} -f wrapper/lib/libwrapper-hpux-parisc-32.sl
%{__rm} -f wrapper/lib/libwrapper-hpux-parisc-64.sl
%{__rm} -f wrapper/lib/libwrapper-macosx-universal-32.jnilib
%{__rm} -f wrapper/lib/libwrapper-solaris-sparc-32.so
%{__rm} -f wrapper/lib/libwrapper-solaris-sparc-64.so
%{__rm} -f wrapper/lib/libwrapper-solaris-x86-32.so
%{__rm} -f wrapper/lib/wrapper-windows-x86-32.dll
%{__rm} -rf rcfiles


%{__mv} -f * $RPM_BUILD_ROOT/%{prefix}/%{HQ_Component_Name}

#echo "Place custom plug-ins in this directory." > $RPM_BUILD_ROOT/%{prefix}/hq-plugins/README


%clean

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%post

if [ -f %{prefix}/%{HQ_Component_Name}/agent.properties.rpmsave ]; then
    %{__mv} %{prefix}/%{HQ_Component_Name}/agent.properties.rpmsave %{prefix}/%{HQ_Component_Name}/agent.properties
fi
exit 0

%postun

if [ ! -f /etc/init.d/%{HQ_Component_Name} ] && [ -d %{prefix}/%{HQ_Component_Name} ]; then
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/data
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/log
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/pdk
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/tmp
fi
exit 0

%posttrans

if [ -f /etc/init.d/%{HQ_Component_Name} ]; then
    chkconfig --add %{HQ_Component_Name}
    chkconfig %{HQ_Component_Name} on
fi
if [ -f /etc/init.d/%{HQ_Component_Name} ] && [ -f /tmp/%{HQ_Component_Name}-was-running-%{version}-%{release} ]; then
    /sbin/service %{HQ_Component_Name} start > /dev/null 2> /dev/null
    %{__rm} -f /tmp/%{HQ_Component_Name}-was-running-%{version}-%{release}
    echo
    echo "The new version of HQ Agent has been started using your existing configuration"
    echo "properties."
    echo
elif [ -f /etc/init.d/%{HQ_Component_Name} ]; then
    echo
    echo "The HQ Agent has successfully been installed, and the service has been"
    echo "configured to start at boot. Prior to starting the service, be sure to"
    echo "uncomment and modify the agent.setup values in the agent.properties file in"
    echo "%{prefix}/%{HQ_Component_Name}/conf. Instructions for doing so can be found online"
    echo "at http://support.hyperic.com/display/DOC/Configure+Agent+-+Server+Communication+in+Properties+File."
    echo "You will also want to check the sanity of the"
    echo "HQ_JAVA_HOME setting in the /etc/init.d/%{HQ_Component_Name} init script."
    echo
fi
exit 0

%files

%defattr (-, root, root)
/etc/init.d/*
%defattr (-, %{HQ_User}, %{HQ_Group}, 0755)
%{prefix}/%{HQ_Component_Name}
%config %{prefix}/%{HQ_Component_Name}/conf/*
%config %{prefix}/hq-plugins


