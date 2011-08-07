#!/usr/bin/perl

use strict;
use Date::Parse;
use LWP::UserAgent;
use XML::Simple;
use Data::Dumper;

# user configurable variables

my $hq_server = 'http://hq-server.example.com:7080';
my $username = "hqadmin";
my $password = "hqadmin";

# no configurable variables below this point

my ($do_action_url);
my ($action, $group_name, $start_time, $end_time) = @ARGV;
$group_name =~ s/ +/\%20/g;

if ($start_time) {
      if ($start_time !~ /^\d\d\/\d\d\/\d\d+/) {
            $start_time=get_tomorrow($start_time);
      }

      if ($end_time !~ /^\d\d\/\d\d\/\d\d+/) {
            $end_time=get_tomorrow($end_time);
      }
}

my $start_epoch = str2time($start_time)*1000;
my $end_epoch = str2time($end_time)*1000;
my $group_get_url = "/hqu/hqapi1/group/get.hqu?name=$group_name";
my $data = get_xml($hq_server.$group_get_url);
my $group_id = $data->{Group}->[0]->{id};

if ($group_id) {

      my $schedule_get_url = "/hqu/hqapi1/maintenance/get.hqu?groupId=$group_id";
      my $schedule_set_url = "/hqu/hqapi1/maintenance/schedule.hqu?groupId=$group_id&start=$start_epoch&end=$end_epoch";
      my $unschedule_url = "/hqu/hqapi1/maintenance/unschedule.hqu?groupId=$group_id";

      if ($action eq 'get') {
            $do_action_url = $hq_server.$schedule_get_url;

      } elsif ($action eq 'set') {
            $do_action_url = $hq_server.$schedule_set_url;

      } elsif ($action eq 'unset') {
            $do_action_url = $hq_server.$unschedule_url;
      }

} else {
      $group_name =~ s/\%20/ /g;
      print "No group_id found for group name \"$group_name\"\n";
      exit 1;
}

if ($do_action_url && $group_id) {
      $data = get_xml($do_action_url);
}

# print Dumper($data);

parse_xml($data);

# subroutines

sub get_xml {
      my ($url) = @_;
      my $user_agent = new LWP::UserAgent;
      my $request = HTTP::Request->new(GET => $url);
      $request -> authorization_basic($username, $password);
      my $response = $user_agent->request($request);
      my $xml = new XML::Simple (ForceArray=>1, KeyAttr=>[]);
      my $data = $xml->XMLin($response->content);
      return $data;

}

sub parse_xml {
      my ($data) = @_;
      my $status_msg = $data->{Status}->[0];
      my $error_code = $data->{Error}->[0]->{ErrorCode}->[0];
      my $reason_txt = $data->{Error}->[0]->{ReasonText}->[0];
      my ($maintenance_event_start, $maintenance_event_end) = (0,0);
      if ($data->{MaintenanceEvent}->[0]->{startTime}) {

            $maintenance_event_start = localtime($data->{MaintenanceEvent}->[0]->{startTime}/1000);
            $maintenance_event_end = localtime($data->{MaintenanceEvent}->[0]->{endTime}/1000);

      }

      print "Status: $status_msg\n";

      if ($error_code) {
            print "Error Code: $error_code\n";
            print "Reason Text: $reason_txt\n";
      }

      if ($maintenance_event_start) {
            print "Start time: $maintenance_event_start\n";
            print "End time: $maintenance_event_end\n";
      }
}

sub get_tomorrow {
      my ($tval) = @_;
            my @t = (localtime(time+86400))[3..5];
            my $tom_date=sprintf ("%d/%d/%d", $t[1] + 1, $t[0], $t[2] + 1900);
            $tval="$tom_date $tval";

      return $tval;

}
