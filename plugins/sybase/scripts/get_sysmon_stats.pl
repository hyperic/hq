#!/usr/bin/perl -w

use Getopt::Long;
use File::Basename;
use Sys::Hostname;
use strict;

my $debug = 0;

my (@Output);

my $Interval = "00:05:00";
my $Username = "sa";
my $Passwd = "";
my $Server = uc(hostname());
my $ISql = "isql";
my $Sqshrc = "isql";

sub main
{
    getArgs();
    @Output = getSysmonOutput();
    print "Availability=1\n";
    printDeadLocks();
    printAvgLockContention();
    printTotalLockReqs();
    printCacheStats();
    printNetworkStats();
    printEngineStats();
}

main();

sub printEngineStats
{
    for (my $i=0; $i<@Output; $i++)
    {
        my $line = $Output[$i];
        chomp($line);
        if ($line !~ /^\s+Engine Busy Utilization/) {
            next;
        }
        $i+=2;
        while ($Output[$i] =~ /^\s+Engine\s[0-9]+/)
        {
            my $line = $Output[$i++];
            chomp($line);
            my (undef, undef, $engine_num, $cpu_busy, undef, $io_busy, undef) =
                split /\s+/, $line, 6;
            print "engine$engine_num.EngineUtilization=$cpu_busy\n";
        }
        last;
    }
}

sub printCacheStats
{
    my $bool = 0;
    for (my $i=0; $i<@Output; $i++)
    {
        my $line = $Output[$i];
        chomp($line);
        if ($line =~ /^\s+Cache Search Summary/) {
            $bool = 1;
            printSummaryStats(\$i);

        } elsif ($line =~ /^\s+Cache:/) {
            $bool = 1;
            printNamedCacheStats(\$i);

        } elsif ($line !~ /^\s+Cache:/) {
            next;

        } elsif ($bool && $line =~ /^=+/) {
            last;

        } else {
            next;
        }
    }
}

sub printNamedCacheStats
{
    my ($i) = @_;
    my $line = $Output[$$i];
    my (undef, undef, $cache_name) = split /\s+/, $line, 3;
    $cache_name =~ s/\s+$//;
    $cache_name =~ s/\s+/_/g;
    print "$cache_name\n" if $debug;
    if ($Output[$$i+=8] !~ /Cache Hits/) {
        return;
    }
    my @array = split /\s+/, $Output[$$i];
    print "$cache_name.CacheHitsRatio=".($array[6]/100)."\n";
    @array = split /\s+/, $Output[$$i+=2];
    print "$cache_name.CacheMissesRatio=".($array[6]/100)."\n";
}

sub printSummaryStats
{
    my ($i) = @_;
    my $line = $Output[++$$i];
    my @array = split /\s+/, $line;
    print "TotalCacheHitsRatio=".($array[7]/100)."\n";
    $line = $Output[++$$i];
    @array = split /\s+/, $line;
    print "TotalCacheMissesRatio=".($array[7]/100)."\n";
}

sub printNetworkStats
{
    my $buf = "";
    my $num = 0;
    for (my $i=0; $i<@Output; $i++)
    {
        my $line = $Output[$i];
        chomp($line);
        if ($line =~ /^\s+Total TDS Packets Received/) {
            $num++;
            $buf = "TDSPacketsReceived";

        } elsif ($line =~ /^\s+Total TDS Packets Sent/) {
            $num++;
            $buf = "TDSPacketsSent";

        } elsif ($num == 2) {
            last;

        } else {
            next;
        }
        while ($Output[++$i] !~ /^\s+Total/) {
            last if ($i>=@Output);
        }
        my @array = split /\s+/, $Output[$i];
        print "$buf=".$array[7]."\n";
    }
}

sub printDeadLocks
{
    my @array = grep /Deadlock Percentage/, @Output;
    @array = split /\s+/, join("", @array);
    print "Deadlocks=".$array[5]."\n";
}

sub printTotalLockReqs
{
    my @array = grep /Total Lock Requests/, @Output;
    @array = split /\s+/, join("", @array);
    print "TotalLockReqs=".$array[6]."\n";
}

sub printAvgLockContention
{
    my @array = grep /Avg Lock Contention/, @Output;
    @array = split /\s+/, join("", @array);
    print "AvgLockContention=".$array[6]."\n";
}

sub printOutput
{
    open(FILE, ">./sysmon.output");
    foreach my $line (@Output)
    {
        chomp($line);
        print FILE "$line\n";
    }
    close(FILE);
}

sub getSysmonOutput
{
    my $pass = ($Sqshrc =~ /^\s*$/) ? "-P $Passwd" : "-r $Sqshrc";
    my $cmd = "$ISql -Usa $pass -S $Server <<-EOF1\n".
              "use master\n".
              "go\n".
              "sp_sysmon '$Interval'\n".
              "go\n".
              "EOF1\n";
    return execCmd("$cmd");
}

sub execCmd
{
    my ($Cmd) = @_;
    print "$Cmd\n" if $debug;
    return `$Cmd`;
}

#########################
#       name: getArgs
#       gets: nothing
#       returns: nothing
#       function: gets and sets the command line args
#########################
sub getArgs
{
  my ($bool_help, $arg_count);
  $arg_count = @ARGV;
  $Getopt::Long::ignorecase = 1;

  GetOptions("interval=s" => \$Interval,
             "i=s" => \$Interval,
             "username=s" => \$Username,
             "u=s" => \$Username,
             "passwd" => \$Passwd,
             "p=s" => \$Passwd,
             "sqshrc=s" => \$Sqshrc,
             "r=s" => \$Sqshrc,
             "isql" => \$ISql,
             "q=s" => \$ISql,
             "server" => \$Server,
             "s=s" => \$Server,
             "help" => \$bool_help,
             "h" => \$bool_help) || printUsage() && exit(1);

  if ($bool_help) {
    printUsage();
    exit(0);
  }
}

#########################
#       name: printUsage
#       gets: nothing
#       returns: nothing 
#       function: prints synopsis
#########################
sub printUsage
{ 
  my $thisprog = basename($0);
  print "$thisprog [--help]\n";
  print "$thisprog:\n"; 
  print "           -q, --isql, default $ISql\n";
  print "           -r, --sqshrc\n";
  print "           -i, --interval\n";
  print "           -u, --username\n";
  print "           -p, --passwd\n";
  print "           -s, --server\n";
  print "           -h, --help\n\n";
}
