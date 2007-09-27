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
    'websphere-pmi' => {
        classes => [qw{
            com/ibm/websphere/pmi/Pmi.*\.class
            com/ibm/websphere/pmi/client/(Cpd|Perf|Pmi).*\.class
            com/ibm/websphere/pmi/client/event/Cpd.*\.class
            com/ibm/websphere/pmi/stat/.*\.class
        }],
        jars => [qw{lib/pmiclient.jar lib/pmi.jar}],
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

