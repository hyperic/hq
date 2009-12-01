select a.member, a.group#, b.thread#, b.bytes, b.members, b.status
from v$logfile a, v$log b
where a.group# = b.group#;
