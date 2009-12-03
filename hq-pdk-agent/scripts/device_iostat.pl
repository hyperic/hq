#!/usr/bin/perl

use strict;
my $device = shift;
unless ($device) {
    warn "Usage: $0 device";
    exit 1;
}

my(@labels, @values);
my $cmd = "iostat -d -x $device";
open IOSTAT, "$cmd|" or do {
    warn "'$cmd' failed: $!";
    exit 1;
};

while (<IOSTAT>) {
    chomp;
    if (s/^\s*Device:\s+//) {
        @labels = split;
    }
    elsif (s/^\s*$device\s+//o) {
        @values = split;
        last;
    }
}
close IOSTAT;

if (@values == 0) {
    warn "$device not found";
    exit 1;
}

for (my $i=0; $i<@labels; $i++) {
    print "$labels[$i]=$values[$i]\n";
}
