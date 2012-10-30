create table if not exists public.HQ_DROPPED_INDICES(INDEX_NAME text, INDEX_STATEMENT text, TABLE_NAME text, INDISPRIMARY bool) ;
grant all on HQ_DROPPED_INDICES to public ; 

drop function if exists fToggleIndices(text, bool);

create or replace function public.fToggleIndices(text, bool) 
returns setof HQ_DROPPED_INDICES as $$ 
declare 
	tablesClause text ;  
	dropIndexStatement text ;
	createIndexStatement text ; 
	oRecord RECORD ; 
begin 

	if $2 then
	  	
		raise notice 'Recreating indices' ;  
	  	
	  	for oRecord in select * from HQ_DROPPED_INDICES 
		loop 
			begin
				createIndexStatement :=  oRecord.INDEX_STATEMENT ;
				
				raise notice 'Recreating index ''%'' for table ''%'' with statement ''%''', oRecord.INDEX_NAME, oRecord.TABLE_NAME, createIndexStatement ;
				execute createIndexStatement ; 
				
				exception when others then 
				raise notice 'An Error Had occured while Recreating index ''%'' for table ''%'': %,%', oRecord.INDEX_NAME, oRecord.TABLE_NAME, SQLSTATE, SQLERRM; 
			end ; 
		end loop ; 
	else
		raise notice 'dropping indices for tables: %', $1  ;
		
		execute 'insert into HQ_DROPPED_INDICES 
		SELECT c2.relname, (case when i.indisprimary = ''t'' then ''alter table '' || c.relname || '' add '' || pg_get_constraintdef(const.oid) else pg_catalog.pg_get_indexdef(i.indexrelid, 0, true) end), c.relname, i.indisprimary
		FROM pg_catalog.pg_class c inner join pg_catalog.pg_index i on  c.oid = i.indrelid 
		inner join pg_catalog.pg_class c2 on i.indexrelid = c2.oid 
		left outer  join pg_constraint const on (const.conrelid = c.oid and const.contype = ''p'' and i.indisprimary = ''t'') 
		where c.relname in (' || lower('''' ||  replace($1, ',', ''',''') || '''') || ')'  
		'order by c.relname' ; 
			
		for oRecord in select * From HQ_DROPPED_INDICES order by table_name, indisprimary
		loop 
			begin 
				if oRecord.INDISPRIMARY then 
					dropIndexStatement := 'alter table ' || oRecord.TABLE_NAME || ' drop constraint ' || oRecord.INDEX_NAME ; 
				else 
					dropIndexStatement := 'drop index ' || oRecord.INDEX_NAME ;
				end if ; 
				
				raise notice 'dropping index ''%'' with statement ''%''', oRecord.INDEX_NAME, dropIndexStatement ;
				execute dropIndexStatement ; 
				
				exception when others then
				raise notice '' ;
				raise notice 'An Error Had occured while dropping index ''%'': %,%''', oRecord.INDEX_NAME, SQLSTATE, SQLERRM ;
			end ;
		end loop; 
		
		raise notice 'Dropped indices details can be found in HQ_DROPPED_INDICES' ;
		
	end if; 
	
	return query select * from HQ_DROPPED_INDICES order by table_name;
end ;
$$ 
language 'plpgsql' volatile 
security definer 
cost 10; 

/**
 * - iterate ove table table name list ($1)
 *   - split the <table_name>~<0|1 truncate instruction>~<0|1 disable indices instruction> 
 * 	 - disable all triggers for the table 
 *   - truncate if instruction = 1 
 *   - append table name to disableIndicesList  if instruction = 1   
 *   - invoke the indices toggler passing the disableIndicesList
 */
drop function if exists fmigrationPreConfigure(text) ; 

CREATE OR REPLACE FUNCTION public.fmigrationPreConfigure(text) 
returns void as $$ 
declare 
	stmt text ; 
	tablesMetadata text[] ;
	tableMetadata text ; 
	instructions text[] ; 
	tableName text ;
	dropIndicesList text ; 
begin
	tablesMetadata  := string_to_array($1, ',') ;
		 
	dropIndicesList := '' ; 
	foreach tableMetadata in ARRAY tablesMetadata 
    loop 
	  begin
		  --raise notice 'tableMetadata %', tableMetadata ; 
		  
		  instructions := string_to_array(tableMetadata, '~') ;
		  --raise notice 'instructions: %, %, %', instructions[1], instructions[2], instructions[3] ; 
		  
		  tableName := instructions[1] ; 
		  
		  begin 
     	 	raise notice 'Disabling all Triggers for table: %', tableName;
     	 	stmt = 'ALTER TABLE ' || tableName  || ' DISABLE TRIGGER ALL' ; 
     	 	execute stmt ;
     	 	--raise notice 'alter statment %', stmt;
     	  
     	 	--exception when others then 
		 	--raise notice 'An Error Had occured while Disabling triggers for table %: % %', tableName, SQLERRM, SQLSTATE;
		  end ;
		  
		  if instructions[2] = '1' then 
		 	begin 
		 		raise notice 'Truncating table : %', tableName;
				stmt := 'TRUNCATE ' || tableName  || ' CASCADE' ;  
				execute stmt ;
				--raise notice 'truncate statment %', stmt; 
	     	
				exception when others then 
				raise notice 'An Error Had occured while Truncating table %: % %', tableName, SQLERRM, SQLSTATE;
			end ; 
     	 end if ; 
     	 
     	 if instructions[3] = '1' then
     	 	--raise notice 'appending table name to drop indices list %, %', tableName, dropIndicesList ; 
     	 	if dropIndicesList <> '' then 
     	 		dropIndicesList := dropIndicesList || ',' ; 
     	 	end if ; 
			dropIndicesList := dropIndicesList || tableName ; 
     	 end if ; 
     	 
      end ;
    end loop ; 
    
    if dropIndicesList <> '' then 
	    begin 
	 		raise notice 'dropping indices for tables : ''%''', dropIndicesList;
		
	 		perform fToggleIndices(dropIndicesList, false) ;
	 	
			exception when others then 
			raise notice 'An Error Had occured while dropping for tables %: % %', dropIndicesList, SQLERRM, SQLSTATE;
		end ; 
	end if ; 
	
	SET synchronous_commit TO off ;  
     
end;
$$
language 'plpgsql' VOLATILE
SECURITY DEFINER 
cost 10 ;

drop function if exists fmigrationPostConfigure(text) ;

CREATE OR REPLACE FUNCTION public.fmigrationPostConfigure(text)
RETURNS setof HQ_DROPPED_INDICES as $$
declare 
  oRecord RECORD; 
  oMaxValRecord RECORD; 
  updateSequenceQuery text;
  tableName text ; 
begin
	
	SET statement_timeout to 0 ; 
	
    foreach tableName in ARRAY string_to_array($1, ',') 
    loop 
	  begin
     	 raise notice 're-enabling triggers for table: %', tableName; 
      	execute 'ALTER TABLE ' || tableName || ' ENABLE TRIGGER ALL';
	    exception when others then 
		raise notice 'An Error Had occured while re-enabling constraints for table %: % %', tableName, SQLERRM, SQLSTATE; 
      end; 
    end loop;

	for oRecord IN 
		execute 'SELECT c.column_name, tc.table_name, pg_class.relname as sequence_name
		FROM
		information_schema.table_constraints tc
		JOIN information_schema.constraint_column_usage AS ccu USING (constraint_schema, constraint_name) 
		JOIN information_schema.columns AS c ON c.table_schema = tc.constraint_schema AND tc.table_name = c.table_name AND ccu.column_name = c.column_name
		join pg_class on (pg_class.relname =  tc.table_name || ''_id_seq'' or pg_class.relname = substring(tc.table_name, 1, char_length(tc.table_name)-1) || ''_id_seq'') 
		where constraint_type = ''PRIMARY KEY''
		and tc.table_name in (' || lower('''' ||  replace($1, ',', ''',''') || '''') || ')'
		'and pg_class.relkind = ''S'''
		 
    LOOP 
   		begin 
			 updateSequenceQuery := 'select setval(' || quote_literal(oRecord.sequence_name) ||', coalesce(max(' || quote_ident(oRecord.column_name) || '), (select last_value from '|| oRecord.sequence_name ||'))) from ' || quote_ident(oRecord.table_name) || ';' ;

	         RAISE NOTICE 'Executing %', updateSequenceQuery ;
			 execute updateSequenceQuery ;
			exception when others then 
			raise notice 'An Error Had occured while configuring the sequence % for table %: % %', oRecord.sequence_name, oRecord.table_name, SQLERRM, SQLSTATE; 
        end; 
	END LOOP; 
	
	drop function if exists fToggleIndices(text, bool) cascade ; 
	drop function if exists fmigrationPreConfigure(text) cascade ;  
	
	SET synchronous_commit TO ON ;
	RESET statement_timeout ;
	
	return query select * from HQ_DROPPED_INDICES order by table_name;

end;
$$
language 'plpgsql' VOLATILE
SECURITY DEFINER 
cost 10 ;
