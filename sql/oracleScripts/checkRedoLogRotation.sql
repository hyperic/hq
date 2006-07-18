# Show time between log switches
select b.recid,
	to_char(b.first_time, 'dd-mon-yy hh:mi:ss') start_time, a.recid,
	to_char(a.first_time, 'dd-mon-yy hh:mi:ss') end_time, 
	round(((a.first_time - b.first_time)*24)*60,2) minutes
	from V$LOG_HISTORY a, V$LOG_HISTORY b
	where a.recid = b.recid+1
	order by a.first_time asc
/
