spool 1
set heading off
select 'alter index '||owner||'.'||index_name||' rebuild ;' from dba_indexes where owner = 'HQADMIN' and table_name not like 'HQ_METRIC%' and index_name not like '%$$';
spool off