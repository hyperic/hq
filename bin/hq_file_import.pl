## cam_file_import.pl                                       1/30/2004
## 
## this script will traverse a directory tree and generate xml that can
## be used to import the file and directory resources into CAM.
##
## the script takes one optional arguement which is the directory you wish
## to traverse.  if the argument is not specified, it will traverse cwd.
##
## this script requires Sys::Hostname, File::Find and Cwd, all of which should
## exist in a standard perl installation.
##
## usage:
## perl cam_file_import.pl /path/to/traverse
##
use strict;
use Sys::Hostname;
use File::Find;
use Cwd;

## CAM variables ##
my $cam_shell = '/local0/spider/shell-1.0.5.53/cam-shell.sh';
my $cam_user = 'covalent';
my $cam_pass = 'covalent';
my $cam_host = 'heimdal';

## traverse or not?
## traverse is like find,
## not traverse will just get the the specified directory
my $traverse = 0;

## metric collection interval (in seconds)
my $collect_interval = 7200;

## will split the file and directory resources into a seperate xml file.
my $split_xml_files = 0;

## alert and notification variables ##
## metrics to set alerts on ##
##
## file metrics:
## Availability
## LastAccess
## LastChange
## LastModified
## Gid
## Uid
## Permissions
## Size
my @file_alert_metrics = ('Availability','Uid','Gid','Permissions');

## directory metrics:
## Availability
## Blkdevs
## Chrdevs
## LastAccess
## LastChange
## LastModified
## Gid
## Uid
## Permissions
## Files
## Sockets
## Subdirs
## Symlinks
## Total
my @dir_alert_metrics = ('Availability','Uid','Gid','Permissions','Total');

## alert notification
## notify a CAM user or email (email = 1, cam user = 2, cam role = 3) ##
my $user_or_email = 3;
## who to notify.
## if $user_or_email = 1, this is an email address.
## if $user_or_email = 2, this should be a cam user id.
## if $user_or_email = 3, this should be a cam role.
my $notify_user = 'alerts';
## alert priority (1 = low, 2 med, 3 = high)
my $alert_priority = 2;

## will configure metric collection in the import xml.
## XXX: should be set to 0!
my $enable_metric_collection = 0;

my $collect_interval_var = 'collect.interval';
my $hostname = Sys::Hostname::hostname();
my @resource_name = ();
my @file_resource = ();
my @dir_resource = ();

## attempt to collect host info ##
my ($name, $aliases, $addrtype, $length, @addrs) = gethostbyname $hostname;
my ($a, $b, $c, $d) = unpack('C4', $addrs[0]);
my $os = $^O;

## XXX: support for non-linux OS must be added in here, if necessary.
if ($os =~ /linux/i) {
    $os = 'Linux';
}
elsif ($os =~ /sol/i) {
    $os = 'Solaris';
}
else {
    die "unsupported OS \'$os\'";
}

## open the xml file(s) to be written and prepare to walk the tree ##
my $start_dir = $ARGV[0] || '.';
my $cwd = cwd;
if ($start_dir !=~ /^\//) {
    chdir $start_dir or die "cant chdir to $start_dir: $!";
    $start_dir = cwd;
}
my $file1 = "$cwd/cam_file_import.xml";
my $file2 = "$cwd/cam_file_import_services.xml";
my $metric_enable_script = "$cwd/cam_file_import-metric_enable.script";
my $alert_def_script = "$cwd/cam_file_import-alert_def.script";

my $xml = '';

## get current file and directory CAM resources ##
## XXX: resource names are expected to be in hostname:/path/to/file format
my @current_resource = get_current_resources();

## set up for split files, if necessary ##
if ($split_xml_files) {
    $xml .=<<XML;
<!DOCTYPE inc [
    <!ENTITY services SYSTEM "$file2">
]>
XML
}

$xml .= "<cam>\n";

## set up a collection interval property if metrics are to be collected ##
if ($enable_metric_collection) {
    $xml .=
"<property name=\"$collect_interval_var\" value=\"$collect_interval\" />\n"
}

## platform and server definitions ##
$xml .=<<XML;
    <platform name="$name" type="$os" fqdn="$name">
        <ip_addresses>
            <ip address="$a.$b.$c.$d" netmask="255.255.255.0" />
        </ip_addresses>
        <agentConn address="$a.$b.$c.$d" port="2144" />
        <server name="$name $os FileServer" type="FileServer" installpath="/">
XML
## XXX: this would configure the server, but is causing problems (bug 9247)
##            <resourceConfig />
##            <metric>
##                <metricConfig />
##            </metric>
##XML

if ($split_xml_files) {
    $xml .=<<XML;
            &services;
        </server>
    </platform>
</cam>
XML
}

## if we are splitting the xml file, write the first one ##
if ($split_xml_files) {
    open (XML1, ">$file1") or die "cant write $file1: $!";
    print XML1 $xml;
    close (XML1);

    $xml = '';
}
else {
    $file2 = $file1;
}

## traverse the tree, generate the xml and write the file ##
my ($file_count, $dir_count, $link_count) = (0,0,0);
$xml .= "\n<!-- begin CAM file and directory service definitions -->\n";

if ($traverse) {
    File::Find::find(\&generate_xml, $start_dir);
}
else {
    opendir FIND, $start_dir or die "cant opendir $start_dir: $!";
    my @files = readdir FIND;
    foreach (@files) {
        next if $_ eq '..';
        &generate_xml;
    }
    close (FIND);
}

if ($file_count == 0 and $dir_count == 0) {
    print "no new files or directories found!\n";
    exit;
}

## report the service counts ##
print "generated import xml for $file_count total files\n";
print "    (includes $dir_count directories and $link_count symlinks)\n";

$xml .=<<XML;
<!-- service counts:
    $file_count total files
        (includes $dir_count directories and $link_count symlinks)
-->
XML

if (!$split_xml_files) {
    $xml .=<<XML;
<!-- end CAM file and directory service definitions -->

        </server>
    </platform>
</cam>
XML
}

open (XML2, ">$file2") or die "cant write $file2: $!";
print XML2 $xml;
close (XML2);

## generate the metric enable and alert definition script ##
open (METRIC, ">$metric_enable_script")
    or die "cant write $metric_enable_script: $!";
open (ALERT, ">$alert_def_script")
    or die "cant write $alert_def_script: $!";

print ALERT "login $cam_user:$cam_pass\@$cam_host\n";

## files ##
foreach my $resource (@file_resource) {
    print METRIC "metric configure -service \"$resource\"\nmetric enable -service \"$resource\" -interval $collect_interval\n";
    foreach (@file_alert_metrics) {
        my $alert_name = "$_ change";
        print ALERT "alertdef create -service \"$resource\"\n$alert_name\n$resource $_ change alert\n$alert_priority\n2\n2\n4\n$_\nn\ny\n$user_or_email\n$notify_user\n";
    }
}

## directories ##
foreach my $resource (@dir_resource) {
    print METRIC "metric configure -service \"$resource\"\nmetric enable -service \"$resource\" -interval $collect_interval\n";
    foreach (@dir_alert_metrics) {
        my $alert_name = "$_ change";
        print ALERT "alertdef create -service \"$resource\"\n$alert_name\n$resource $_ change alert\n$alert_priority\n2\n2\n4\n$_\nn\ny\n$user_or_email\n$notify_user\n";
    }
}

close (METRIC);
close (ALERT);


## xml generation subroutine ##
sub generate_xml {
    my $type = 'file';
    my $service_type = 'FileServer File';
    my $fq_name = "$start_dir/$_";
    if ($_ eq '.') {
        $fq_name = $start_dir;
    }
    $fq_name = $File::Find::name if $traverse;
    my $display_name = "$name:$fq_name";

    ## check that the name has not already been entered
    ## resource names in CAM are case insensitive
    ## XXX: this is probably not very optimized and
    ## XXX: could take a while with large datasets
    my $look_for = lc($display_name);
    my $inc = 0;
    foreach (@resource_name) {
        if ($_ eq $look_for) {
            $inc++;
            $look_for = lc($display_name) . "-$inc";
            redo;
        }
    }
    $display_name .= "-$inc" if $inc;

    ## check that the file or directory is not already in CAM ##
    foreach (@current_resource) {
        if (lc($_) eq lc($display_name)) {
            return;
        }
    }

    ## determine service type ##
    if (-d $_) {
        $type = 'directory';
        $service_type = 'FileServer Directory';
        $dir_count++;
    }
    elsif (-l $_) {
        $type = 'symlink';
        #$service_type = 'FileServer File';
        $link_count++;
    } elsif (-S $_) {
        $type = 'socket';
        #$service_type = 'FileServer File';
    }
    elsif (!-f $_) {
        warn "$fq_name: unknown type!  skipped.";
        return;
    }

    $file_count++;

    push (@resource_name, lc($display_name));
    if ($type eq 'directory') {
        push (@dir_resource, lc($display_name));
    }
    else {
        push (@file_resource, lc($display_name));
    }

    ## escape crazy characters ##
    my $config_name = $fq_name;
    foreach ($display_name, $config_name) {
        s/&/&amp;/g;
        s/</&lt;/g;
        s/>/&gt;/g;
        s/%/&#37;/g;
        s/\$/&#36;/g;
        s/\+/&#43;/g;
        #s/-/&#45;/g;
        ## and the list goes on... ##
    }

    $xml .=<<XML;
<service name="$display_name" type="$service_type" description="$_ $type">
    <resourceConfig path="$config_name" />
XML

    if ($enable_metric_collection) {
        $xml .=<<XML;
    <metric>
        <metricConfig />
        <collect metric="all" interval="\${$collect_interval_var}" />
    </metric>
XML
    }

    $xml .=<<XML;
</service>
XML

}

## get current resources subroutine ##
sub get_current_resources {
    my @r = ();
    my ($rid,$rname,$rtype);
    my $tmp_file = "$cwd/.tmp.shell.cmd";
    open (CMD, ">$tmp_file") or die "cant open $tmp_file: $!";
    print CMD <<CMDS;
login $cam_user:$cam_pass\@$cam_host
set page.size -1
resource list -service
quit
CMDS
    close (CMD);

    my $resource_list = `$cam_shell < $tmp_file 2>/dev/null`;
    unlink $tmp_file;

    foreach (split /\n/, $resource_list) {
        ## XXX: assuming no spaces...probably bad! ##
        if (/^(\d+)\s+(.*?)\s+FileServer\s(File|Directory)/) {
            $rid = $1;
            $rname = $2;
            $rtype = $3;

            push (@r, $rname);
        }
    }

    return @r;
}

