#!/bin/sh

DB=hq
USER=hqadmin
PG_DUMP=/Users/jtravis/dev/postgres/bin/pg_dump

if [ $# -ne 1 ] ; then
	echo "Syntax:  $0 outDir"
	exit 1
fi

OUTDIR="$1"
FULL_SCHEMA="$OUTDIR/full_schema.sql"
TABLE_LIST="$OUTDIR/table_list"
SEQUENCE_LIST="$OUTDIR/sequence_list"

mkdir "$OUTDIR"
$PG_DUMP -U$USER -s $DB > "$FULL_SCHEMA"

# Dump full schema
cat "$FULL_SCHEMA" | grep "CREATE TABLE" | awk '{ print $3 }' \
	| sort > "$TABLE_LIST"

# Dump sequences
cat "$FULL_SCHEMA" | grep "CREATE SEQUENCE" | awk '{ print $3 }' \
	| sort > "$SEQUENCE_LIST"

# Dump individual tables
echo Found `cat "$TABLE_LIST" | wc -l` tables
for table in `cat "$TABLE_LIST"` ; do
	echo " ... Dumping $table"
	$PG_DUMP -U$USER -s $DB -t $table | egrep -v -- "^--" > "$OUTDIR/${table}_tbl.sql"
done

# Dump individual sequences
echo Found `cat "$SEQUENCE_LIST" | wc -l` sequences
for seq in `cat "$SEQUENCE_LIST"` ; do
	echo " ... Dumping $seq"
	$PG_DUMP -U$USER -s $DB -t $seq | egrep -v -- "^--" > "$OUTDIR/${seq}.sql"

	cat "$OUTDIR/${seq}.sql" | awk '
		{ 
			if ($1 == "CREATE") { 
				started = 1;
			} else if (NF == 0) { 
				started = 0; 
			} 
 			if (started) 
				print $0;
		}' > "$OUTDIR/${seq}.sql.mngl"

done

# Mangle the sql files to sort the columns within the tables
echo Mangling SQL in tables
./mangle_tables.sh "$OUTDIR"
