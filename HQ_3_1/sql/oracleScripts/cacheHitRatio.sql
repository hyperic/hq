column phys	format 999,999,999 heading 'Physical Reads'
column gets 	format 999,999,999 heading ' DB Block Gets'
column con_gets format 999,999,999 heading 'Consistent Gets'
column hitratio format 999.99 	   heading ' Hit Ratio '
 select sum(decode(name,'physical reads', value, 0))phys,
	sum(decode(name,'db block gets', value, 0))gets,
	sum(decode(name,'consistent gets', value,0)) con_gets,
	(1- (sum(decode(name,'physical reads', value,0)) /
		(sum(decode(name,'db block gets',value,0)) +
		 sum(decode(name,'consistent gets',value,0))))) * 100 hitratio 
 from v$sysstat;
