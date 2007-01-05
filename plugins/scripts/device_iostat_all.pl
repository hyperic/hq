#!/usr/bin/perl

use strict;

my(@labels, %values);
my $cmd = "iostat -d -x";
open IOSTAT, "$cmd|" or do {
    warn "'$cmd' failed: $!";
    exit 1;
};

while (<IOSTAT>) {
    chomp;
    if (s/^\s*Device:\s+//) {
        @labels = split;
    }
    elsif (@labels) {
	s/^\s*//;
	s/\s*$//;
	next if /^$/;
        my @values = split;
	my $device = shift @values;

	for (my $i=0; $i<@labels; $i++) {
	    print "${device}_$labels[$i]=$values[$i]\n";
	}
    }
}
close IOSTAT;
