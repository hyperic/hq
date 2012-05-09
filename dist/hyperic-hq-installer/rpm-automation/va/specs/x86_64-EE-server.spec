%define HQ_Component_Name       hyperic-hqee-installer
%define HQ_Component_Version    @hq.version@
%define HQ_Component_Edition	EE
%define HQ_Component_Build	x86-64-linux-@hq.version@.@hq.ee.build@
%define HQ_Component_Release   	EE.1 
%define HQ_Component_Build_Type @hq.build.type@

%define HQ_User			hyperic
%define HQ_Group		hyperic
%define HQ_User_Home		/opt/hyperic

AutoReqProv:    no
	
Name:           %{HQ_Component_Name}
Version:        %{HQ_Component_Version}
Release:        %{HQ_Component_Release}
Summary:        %{HQ_Component_Name}
Source0:        %{HQ_Component_Name}-%{HQ_Component_Build}.tar.gz
Vendor:		Hyperic, Inc.
License:        Commercial
BuildRoot:      %{_tmppath}/%{name}-%{version}-%{release}-root
Group:          Applications/Monitoring
Packager: 	Hyperic Support <support@hyperic.com>
Prefix:		%{HQ_User_Home}
Url: 		http://www.hyperic.com
ExclusiveArch:	x86_64
ExclusiveOS:	linux

%description

Server for the Hyperic HQ systems management system.

%prep

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%setup -T -b 0 -n %{HQ_Component_Name}-%{HQ_Component_Version}.%{HQ_Component_Build_Type}

%pre

#
# Create a user and group if need be
#

    %{_sbindir}/groupadd %{HQ_Group} 2> /dev/null
    %{__mkdir} -p -m 755 %{HQ_User_Home}
    %{_sbindir}/useradd -g %{HQ_Group} -d %{HQ_User_Home} %{HQ_User} 2> /dev/null
    chown -R %{HQ_User}.%{HQ_Group} %{HQ_User_Home}
exit 0

%preun

%build


%install

%{__install} -d -m 755 $RPM_BUILD_ROOT/etc/init.d
%{__install} -d -m 755 $RPM_BUILD_ROOT%{prefix}/%{HQ_Component_Name}
%{__install} -d -m 755 $RPM_BUILD_ROOT/%{prefix}/hq-plugins
%{__install} -m 755 rcfiles/hyperic-hqee-server.init $RPM_BUILD_ROOT/etc/init.d/hyperic-hq-server

%{__rm} -f installer/lib/sigar-x86-winnt.lib
%{__mv} -f * $RPM_BUILD_ROOT/%{prefix}/%{HQ_Component_Name}

%clean

[ "$RPM_BUILD_ROOT" != "/" ] && rm -rf $RPM_BUILD_ROOT

%post


%postun


%posttrans

%files

%defattr (-, root, root)
/etc/init.d/*
%defattr (-, %{HQ_User}, %{HQ_Group})
%{prefix}/%{HQ_Component_Name}
%config %{prefix}/hq-plugins

