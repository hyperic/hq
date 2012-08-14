CREATE OR REPLACE FUNCTION public.fmigrationPostConfigure(text)
RETURNS void as $$
declare 
  oRecord RECORD; 
  oMaxValRecord RECORD; 
  updateSequenceQuery text;
  tableName text ; 
begin
	
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
		join pg_class on pg_class.relname =  tc.table_name || ''_id_seq''
		where constraint_type = ''PRIMARY KEY''
		and tc.table_name in (' || lower('''' ||  replace($1, ',', ''',''') || '''') || ')'
		'and pg_class.relkind = ''S'''
		 
    LOOP 
   		begin 
			 updateSequenceQuery := 'select setval(' || quote_literal(oRecord.sequence_name) ||', coalesce(max(' || quote_ident(oRecord.column_name) || '), (select last_value from '|| oRecord.sequence_name ||'))) from ' || quote_ident(oRecord.table_name) || ';' ;

	         RAISE NOTICE 'Executing %', updateSequenceQuery ;
			-- execute updateSequenceQuery ;
			exception when others then 
			raise notice 'An Error Had occured while configuring the sequence % for table %: % %', oRecord.sequence_name, oRecord.table_name, SQLERRM, SQLSTATE; 
        end; 
	END LOOP; 

end;
$$
language 'plpgsql' VOLATILE
SECURITY DEFINER 
cost 10 ;
