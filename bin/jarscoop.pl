#scoop just enough classes out of given jars to compile

use strict;

my %modules = (
    'weblogic-jmx' => {
        classes => [qw{
            weblogic/health/HealthFeedback\.class
            weblogic/health/HealthState\.class
            weblogic/jndi/Environment\.class
            weblogic/management/CompatibilityException\.class
            weblogic/management/MBeanHome\.class
            weblogic/management/ManagementException
            weblogic/management/RemoteMBeanServer\.class
            weblogic/management/RemoteNotificationListener\.class
            weblogic/management/WebLogicMBean\.class
            weblogic/management/WebLogicObjectName\.class
            weblogic/management/configuration/.*MBean\.class
            weblogic/management/configuration/ConfigurationException\.class
            weblogic/management/deploy/DeployerRuntime\.class
            weblogic/management/deploy/DeploymentData\.class
            weblogic/management/descriptors/BaseDescriptor\.class
            weblogic/management/descriptors/WebDescriptorMBean\.class
            weblogic/management/descriptors/WebElementMBean\.class
            weblogic/management/descriptors/TopLevelDescriptorMBean\.class
            weblogic/management/descriptors/XMLDeclarationMBean\.class
            weblogic/management/descriptors/XMLElementMBean\.class
            weblogic/management/descriptors/webapp/WebAppDescriptorMBean\.class
            weblogic/management/descriptors/webapp/FilterMBean\.class
            weblogic/management/descriptors/webapp/ParameterMBean\.class
            weblogic/management/info/.*\.class
            weblogic/management/internal/DynamicMBeanImpl.*.class
            weblogic/management/runtime/.*MBean\.class
            weblogic/management/runtime/ServerStates\.class
            weblogic/management/runtime/RuntimeMBeanDelegate\.class
            weblogic/management/tools/AttributeInfo\.class
            weblogic/rmi/extensions/NonTransactionalRemote\.class
            weblogic/utils/NestedException\.class
            weblogic/utils/NestedThrowable\.class
            weblogic/security/Security\.class
            weblogic/security/auth/callback/URLCallback\.class
        }],
        jars => [qw{server/lib/weblogic.jar}],
    },
    'websphere-jmx' => {
        classes => [qw{
            com/ibm/websphere/management/AdminClient
            com/ibm/websphere/management/AdminClientFactory
            com/ibm/websphere/management/exception/ConnectorException
            com/ibm/ws/exception/WsException
            com/ibm/ws/exception/WsNestedException
        }],
        jars => [qw{lib/admin.jar lib/wsexception.jar}],
    },
    'websphere-ejs' => {
        classes => [qw{
            com.ibm.ejs.sm.beans.Attribute
            com.ibm.ejs.sm.beans.ClientAccess
            com.ibm.ejs.sm.beans.BinaryAttrSerialization
            com.ibm.ejs.sm.beans.ClientAccessHome
            com.ibm.ejs.sm.beans.CloneOnlyAwareConfig
            com.ibm.ejs.sm.beans.DataSource
            com.ibm.ejs.sm.beans.EnterpriseApp
            com.ibm.ejs.sm.beans.EJBServer
            com.ibm.ejs.sm.beans.EJBServerAttributes
            com.ibm.ejs.sm.beans.J2EEResourceConfig
            com.ibm.ejs.sm.beans.Module
            com.ibm.ejs.sm.beans.Node
            com.ibm.ejs.sm.beans.Server
            com.ibm.ejs.sm.beans.Type
            com.ibm.ejs.sm.beans.Model
            com.ibm.ejs.sm.beans.Relation
            com.ibm.ejs.sm.beans.Resource
            com.ibm.ejs.sm.beans.LiveRepositoryObject
            com.ibm.ejs.sm.beans.LiveObjectAttributes
            com.ibm.ejs.sm.beans.RepositoryObject
            com.ibm.ejs.sm.beans.RepositoryObjectName
            com.ibm.ejs.sm.beans.RepositoryObjectNameElem
            com.ibm.ejs.sm.beans.TransportConfig
            com.ibm.ejs.sm.beans.WebContainerConfig
            com.ibm.ejs.sm.exception.AttributeDoesNotExistException
            com.ibm.ejs.sm.exception.AttributeNotSetException
            com.ibm.ejs.sm.exception.OpException
            com.ibm.ejs.sm.util.act.Act
            com.ibm.ejs.sm.active.ActiveObject
            com.ibm.ejs.sm.active.ActiveEJBServerConfig
            com.ibm.ejs.sm.active.ActiveModuleConfig
            com.ibm.ejs.sm.active.ActiveServerConfig
            com.ibm.ejs.sm.agent.Containment
        }],
        jars => [qw{lib/repository.jar lib/utils.jar lib/admin.jar}],
    },
    'websphere-pmi' => {
        classes => [qw{
            com/ibm/websphere/pmi/Pmi.*\.class
            com/ibm/websphere/pmi/client/(Cpd|Perf|Pmi).*\.class
            com/ibm/websphere/pmi/client/event/Cpd.*\.class
        }],
        jars => [qw{lib/pmiclient.jar lib/pmi.jar}],
    },
    'silverstream-37' => {
        classes => [qw{
            com.sssw.rt.util.AgRuntime
            com.sssw.rt.util.AgiUserLogin
            com.sssw.rt.util.AgiParentedException
            com.sssw.rt.util.AgiRemoteServerSessionList
            com.sssw.rt.util.AgoException
            com.sssw.rt.util.AgoApiException
            com.sssw.rt.util.AgoRsrcMgr
            com.sssw.rt.util.AgoSecurityException
            com.sssw.rt.util.AgoSystemException
            com.sssw.rt.util.AgrServerSession
            com.sssw.rt.util.AgoUnrecoverableSystemException
            com.sssw.rt.util.AgoUserLoginInfo
            com.sssw.rts.adminclient.AgAdmin
            com.sssw.rts.acl.AgiPrincipalFactory
            com.sssw.rts.acl.AgoPermission
            com.sssw.rts.adminapi.AgiAdmChanges
            com.sssw.rts.adminapi.AgiAdmContainer
            com.sssw.rts.adminapi.AgiAdmContainerBase
            com.sssw.rts.adminapi.AgiAdmElement
            com.sssw.rts.adminapi.AgiAdmElementBase
            com.sssw.rts.adminapi.AgiAdmPropertyBag
            com.sssw.rts.adminapi.AgiAdmServer
            com.sssw.rts.adminapi.AgiAdmStatistics
            com.sssw.rts.adminapi.AgiAdmSession
            com.sssw.rts.adminapi.AgiAdmStatContainer
            com.sssw.rts.adminapi.AgiAdmStatElement
            com.sssw.rts.adminapi.AgiAdmStatSet
            com.sssw.rts.adminapi.AgoAdmStatInfo
        }],
        jars => [qw{lib/SilverServerAll.zip}],
    },
);

unless (@ARGV == 2) {
    die "usage: name install-prefix";
}

my($name, $dir) = @ARGV;

my $module;

unless (($module = $modules{$name})) {
    die "unknown module $name";
}

my $tmpdir = "$name-jar";
mkdir $tmpdir, 0775 or die "mkdir $tmpdir: $!";
chdir $tmpdir;

my @wanted = @{ $module->{classes} };
my $wanted = join '|', @wanted;
my @all_classes;

sub extract {
    my($jar) = @_;

    my $cmd = "unzip -l $jar";

    open UNZIP, "$cmd|" or die "$cmd: $!";

    my @classes = ();

    while (<UNZIP>) {
        next unless m:($wanted):o;
        chomp;
        push @classes, (split)[-1];
    }

    close UNZIP;

    system "unzip", $jar, @classes;

    push @all_classes, @classes;
}

for my $jar (@{ $module->{jars} }) {
    extract("$dir/$jar");
}

system "jar", "cvf", "../$name.jar", @all_classes;

chdir "..";
system "rm -rf $tmpdir";

