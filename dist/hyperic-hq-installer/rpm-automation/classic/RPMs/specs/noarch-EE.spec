%define HQ_Component_Name       hyperic-hqee-agent
%define HQ_Component_Version    @hq.version@
%define HQ_Component_Edition	EE
%define HQ_Component_Build	@hq.ee.build@-noJRE
%define HQ_Component_Release   	1
%define HQ_Component_Build_Type @hq.build.type@

%define HQ_User			hyperic
%define HQ_Group		hyperic
%define HQ_User_Home		/opt/hyperic

AutoReqProv:    no

# Requires Sun's Java, which must currently be downloaded directly from Sun
# at http://java.sun.com.
#Requires:	j2re

Name:           %{HQ_Component_Name}
Version:        %{HQ_Component_Version}.%{HQ_Component_Edition}
Release:        %{HQ_Component_Release}
Summary:        %{HQ_Component_Name}
Source0:        %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build}.tar.gz
Vendor:		Hyperic, Inc.
License:        Commercial
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root
Group:          Applications/Monitoring
Packager: 	Hyperic Support <support@hyperic.com>
Prefix:		%{HQ_User_Home}
Url: 		http://www.hyperic.com
BuildArch:	noarch

%description

Agent for the Hyperic HQ systems management system.

%prep

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%setup -T -D -b 0 -n %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build_Type}

%pre

# If hq-agent is already installed and running (whether installed by RPM
# or not), then kill it, but remember that it was running.
%{__rm} -f /tmp/%{name}-was-running-%{version}-%{release}
if [ -f /etc/init.d/%{name} ]; then
    /sbin/service %{name} stop > /dev/null 2> /dev/null
    touch /tmp/%{name}-was-running-%{version}-%{release}
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
if [ -f /etc/init.d/%{name} ]; then
    /sbin/service %{name} stop > /dev/null 2> /dev/null
fi
chkconfig --del %{HQ_Component_Name}

%build

%install

%{__install} -d -m 755 $RPM_BUILD_ROOT/etc/init.d
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/%{HQ_Component_Name}
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/hq-plugins
%{__install} -m 755 rcfiles/%{name}.init.rh $RPM_BUILD_ROOT/etc/init.d/%{name}

%{__rm} -rf background.bat hq-agent.exe rcfiles/ rpm.spec bundles/agent-%{HQ_Component_Version}.%{HQ_Component_Build_Type}/pdk/lib/sigar-x86-winnt.lib wrapper/sbin/wrapper-linux-ppc-64 wrapper/sbin/wrapper-hpux-parisc-64 wrapper/sbin/wrapper-solaris-sparc-32 wrapper/sbin/wrapper-solaris-sparc-64
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

if [ ! -f /etc/init.d/%{name} ] && [ -d %{prefix}/%{HQ_Component_Name} ]; then
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/data
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/log
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/pdk
    %{__rm} -Rf %{prefix}/%{HQ_Component_Name}/tmp
fi
exit 0

%posttrans

if [ -f /etc/init.d/%{name} ]; then
    chkconfig --add %{HQ_Component_Name}
    chkconfig %{HQ_Component_Name} on
fi
if [ -f /etc/init.d/%{name} ] && [ -f /tmp/%{name}-was-running-%{version}-%{release} ]; then
    /sbin/service %{name} start > /dev/null 2> /dev/null
    %{__rm} -f /tmp/%{name}-was-running-%{version}-%{release}
    echo
    echo "The new version of HQ Agent has been started using your existing configuration"
    echo "properties."
    echo
elif [ -f /etc/init.d/%{name} ]; then
    echo
    echo "The HQ Agent has successfully been installed, and the service has been"
    echo "configured to start at boot. Prior to starting the service, be sure to"
    echo "uncomment and modify the agent.setup values in the agent.properties file in"
    echo "%{prefix}/%{HQ_Component_Name}/conf. Instructions for doing so can be found online"
    echo "at http://support.hyperic.com/display/DOC/Configuring+Agent+Startup+Settings+in+its+Properties+File."
    echo "You will also want to check the sanity of the"
    echo "HQ_JAVA_HOME setting in the /etc/init.d/%{name} init script."
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

%changelog
* Wed Mar 17 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar 12 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar 12 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Thu Mar 11 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Thu Mar 11 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Tue Mar  9 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Tue Mar  9 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Tue Mar  9 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Tue Mar  9 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar  5 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar  5 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar  5 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar  5 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Fri Mar  5 2010 Hyperic Support <support@hyperic.com> 4.3.0
* Sat Jan 23 2010 Hyperic Support <support@hyperic.com> 4.2.0.4
* Fri Jan 15 2010 Hyperic Support <support@hyperic.com> 4.2.0.3
* Tue Dec  8 2009 Hyperic Support <support@hyperic.com> 4.2.0.2
* Thu Nov 19 2009 Hyperic Support <support@hyperic.com> 4.2.0.1
* Thu Nov  5 2009 Hyperic Support <support@hyperic.com> 4.2.0
* Thu Jun 25 2009 Hyperic Support <support@hyperic.com> 4.1.2
* Thu Jun 11 2009 Hyperic Support <support@hyperic.com> 4.1.3
* Wed Jun 10 2009 Hyperic Support <support@hyperic.com> 4.1.3
* Tue Jun  9 2009 Hyperic Support <support@hyperic.com> 4.1.3
* Thu May  7 2009 Hyperic Support <support@hyperic.com> 4.1.2
* Mon Apr  6 2009 Hyperic Support <support@hyperic.com> 4.1.1
* Fri Mar  6 2009 Hyperic Support <support@hyperic.com> 4.1.0
* Thu Feb 26 2009 Hyperic Support <support@hyperic.com> 4.0.3
* Thu Jan 29 2009 Hyperic Support <support@hyperic.com> 4.0.3
* Fri Nov  7 2008 Hyperic Support <support@hyperic.com> 4.0.1
* Tue Oct 28 2008 Hyperic Support <support@hyperic.com> 4.0.0
* Sat Aug 30 2008 Hyperic Support <support@hyperic.com> 3.2.5
* Mon Jul  7 2008 Hyperic Support <support@hyperic.com> 3.2.4
* Mon Jun 30 2008 Hyperic Support <support@hyperic.com> 3.2.4
* Thu May 29 2008 Hyperic Support <support@hyperic.com> 3.2.4
* Thu Apr 17 2008 Hyperic Support <support@hyperic.com> 3.2.3-EE-noJRE-1
* Thu Feb 28 2008 Hyperic Support <support@hyperic.com> 3.2.1-EE-noJRE-1
* Thu Feb 21 2008 Hyperic Support <support@hyperic.com> 3.2.0-EE-noJRE-2
* Wed Jan 29 2008 Hyperic Support <support@hyperic.com> 3.2.0-EE-noJRE-1
- Initial release.
