#!/usr/bin/env perl

use strict;
use Cwd;
use File::Basename;
use File::Find;
use File::Spec;

#perl eclipse/generate_classpath.pl C:\\jboss > .classpath

my(@src);
my(@lib);
my(%libs); #fold dups

my(@dirs);
my $jboss_home = $ENV{JBOSS_HOME};

for (@ARGV) {
    if (-e "$_/jar-versions.xml") {
	$jboss_home = $_;
    }
    else {
	push @dirs, $_;
    }
}

if (@dirs == 0) {
    push @dirs, '.';
    if (-d 'hq') {
	#project extends hq
	push @dirs, './hq';
    }
}

write_project();

open CP, ">.classpath" or die "open >.classpath: $!";

sub add_lib {
    my($lib, $ignore_dups) = @_;
    my $name = basename $lib;
    if ($libs{$name}++) {
	return unless $ignore_dups;
    }
    push @lib, $lib;
}

my(@jar_dirs);

for my $path (@dirs) {
    for my $src (qw(src build/src installer/src tools/txsnatch/src)) {
	my $src_dir = "$path/$src";
	push @src, $src_dir if -d $src_dir;
    }

    my $plugin_dir = "$path/plugins";
    push @jar_dirs, $plugin_dir, "$path/thirdparty/lib", <$path/*_bin>;

    find(sub {
	return unless $_ eq 'src';
	return if $File::Find::dir =~ m,/build/,;
	push @src, "$File::Find::dir/$_";
    }, $plugin_dir);
}

for my $jar_path (@jar_dirs) {
    find(sub {
	my $dir = $File::Find::dir;
	$dir =~ s,^\./,,;
	if ($dir =~ m,(^|/)build/,) {
	    return;
	}
	return if $dir =~ /(xdoclet|tomcat)/;
	return unless /\.jar$/;
	add_lib("$dir/$_", 1);
    }, $jar_path);
}

if ($jboss_home) {
    for my $subdir (qw(client lib server/default/lib)) {
        find(sub {
            my $dir = $File::Find::dir;
            return if /jdom.jar/;
            return unless /\.jar$/;
            add_lib("$dir/$_");
        }, "$jboss_home/$subdir");
    }

    #for NotReadyValve
    my $catalina =
      "$jboss_home/server/default/deploy/jbossweb-tomcat55.sar/catalina.jar";
    if (-e $catalina) {
        add_lib($catalina);
    }
}
else {
    print STDERR "WARNING: JBOSS_HOME not set\n";
}

print CP <<EOF;
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
EOF

for my $path (@src) {
    print CP qq(   <classpathentry kind="src" path="$path"/>\n);
}

print CP qq(   <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>\n);

for my $path (@lib) {
    print CP qq(   <classpathentry kind="lib" path="$path"/>\n);
}

print CP <<EOF;
    <classpathentry kind="output" path="build/classes"/>
</classpath>
EOF

sub write_project {
    open PROJ, ">.project" or die "open >.project: $!";

    print PROJ <<EOF;
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>spider</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
EOF

    close PROJ;
}
