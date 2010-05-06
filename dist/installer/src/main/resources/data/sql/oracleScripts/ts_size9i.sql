/*  ts_size9i.sql  */
set echo off
set feedback off
set recsep off
set verify off
set lines 100
set pages 500
set heading off
col dbksz new_value Blk_size noprint
select value dbksz from v$parameter where name='db_block_size';
select 'DB_NAME: '||name||'     TIME:'||to_char(sysdate, 'mm/dd/yy hh24:mi:ss')
from v$database;
set heading on
col T_space  format a14
col fname    format a44
col Use_Pct  format 999 heading Used%
col Total    format 999,999
col Used_Mg  format 999999
col Free_Mg  format 999999

break on T_Space on report
compute sum of Total   on report
compute sum of Used_Mg on report
compute sum of Free_Mg on report

select T_space, Fname, Total, Free_MG, round((1 - (free_mg/total))*100) Use_Pct
from (select tablespace_name T_Space, file_name Fname, file_id,
             sum(blocks*&blk_size)/1024/1024 Total
      from dba_data_files
      group by tablespace_name, file_name, file_id) total_space,
     (select tablespace_name T2_Space, File_ID, sum(Bytes)/1024/1024 Free_MG
      from dba_free_space
      group by tablespace_name, file_id) free_space
where total_space.t_space = free_space.t2_space
and total_space.file_id = free_space.file_id
order by t_space, Fname
/

select T_space, Total, Total-Free_MG Used_MG, Free_MG, round((1 - (free_mg/total))*100) Use_Pct
from
 (select T_space, Sum(Total) Total, Sum(Free_MG) Free_MG
  from (select tablespace_name T_Space, file_name Fname, file_id,
             sum(blocks*&blk_size)/1024/1024 Total
      from dba_data_files
      group by tablespace_name, file_name, file_id) total_space,
     (select tablespace_name T2_Space, File_ID, sum(Bytes)/1024/1024 Free_MG
      from dba_free_space
      group by tablespace_name, file_id) free_space
 where total_space.t_space = free_space.t2_space
 and total_space.file_id = free_space.file_id
 group by T_space)
order by 5
/
clear breaks
tti off
set verify on
set feedback on
set verify on
set echo on
