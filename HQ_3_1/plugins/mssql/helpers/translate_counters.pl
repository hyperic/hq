use strict;
use Encode;

my %lang = ('007' => 'german',
            '009' => 'english');

my %map;

open CTR, 'sqlctr.ini' or die;

while (<CTR>) {
    next unless /^([A-Z_]+_)(00[79])_NAME=(.*)/;
    my $key = $1;
    my $lang = $lang{$2};
    my $name = $3;
    $name =~ s/^SQLServer://;
    $map{ $lang }->{$key} = $name;
}

my %translate;

for my $key (sort keys %{ $map{english} }) {
    my $eng = $map{english}->{$key};
    my $ger = $map{german}->{$key};
    #$ger =~ tr/\0-\xff//UC;
    $translate{$eng} = $ger;
}

my $pat = join '|', map { "\Q$_" } keys %translate;

open FH, "hq-plugin.xml" or die;

while (<FH>) {
    if (/template=/) {
        s/($pat)/$translate{$1}/o;
    }
    print;
}
