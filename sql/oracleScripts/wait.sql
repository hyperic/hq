set pagesize 60
set lines 132
rem Sessions waiting
rem for Event other than 'SQL*Net message from client', 'pipe get' and Background processes.
column sid format 999999
column state format a10 trunc
column username format a10
column osuser format a10
column event format a28
column wait_time heading 'Sec.|Wtd' format 999
column wis heading 'Sec.|Wtng' format 999
col    P1_P2_P3_TEXT for a40
set time on timing on
spool waits
select w.sid, to_char(p.spid,'99999') PID,
       substr(w.event, 1, 28) event, substr(s.username,1,10) username,
       substr(s.osuser, 1,10) osuser,
       state,
       wait_time, seconds_in_wait wis
from v$session_wait w, v$session s, v$process p
where s.sid=w.sid
  and p.addr  = s.paddr
  and w.event not in ('SQL*Net message from client', 'pipe get')
  and s.username is not null
/

