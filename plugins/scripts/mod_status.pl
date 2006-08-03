#!/usr/bin/perl

use strict;
use IO::Socket ();

my %state = (
   '_' => 'Waiting',
   'S' => 'Starting',
   'R' => 'Reading',
   'W' => 'Sending',
   'K' => 'Keepalive',
   'D' => 'DNS',
   'C' => 'Closing',
   'L' => 'Logging',
   'G' => 'Finishing',
   '.' => 'Free',
);

my $status = get_server_status(@ARGV);

for my $line (@$status) {
    my($key, $val) = split /:\s+/, $line;
    if ($key eq 'Scoreboard') {
        process_scoreboard($val);
    }
    else {
        print "$key=$val\n";
    }
}

sub process_scoreboard {
    my(%scoreboard) = map { $_ => 0 } keys %state;
    local $_ = shift;
    ++$scoreboard{$1} while /(.)/g;
    while (my($key, $val) = each %scoreboard) {
        print "$state{$key}=$val\n";
    }
}

sub get_server_status {
    my($host, $port, $path, $timeout) = @_;

    $host    ||= 'localhost';
    $port    ||= 80;
    $path    ||= '/server-status';
    $timeout ||= 5;

    my $client =
      IO::Socket::INET->new(Proto => 'tcp',
                            PeerAddr => $host,
                            PeerPort => $port,
                            Timeout => $timeout);

    unless ($client) {
        print "Connection to $host:$port failed: $@\n";
        exit 1;
    }

    $client->autoflush(1);

    my $EOL = "\r\n";
    print $client "GET $path?auto HTTP/1.0" . ($EOL x 2);

    while (<$client>) {
        last if $_ eq $EOL;
    }
    chomp(my(@status) = <$client>);
    close $client;

    return \@status;
}
