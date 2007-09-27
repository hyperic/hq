#!/bin/sh

if [ $# -lt 2 ] ; then
	echo "Syntax: $0 fromDir toDir [useOrigData]"
	exit 1
fi

FROMDIR="$1"
TODIR="$2"

echo "Analayzing difference between schema from $FROMDIR to $TODIR"
echo ""
echo "Differences in number of tables:"
diff -u "$FROMDIR"/table_list "$TODIR"/table_list
echo ""
echo "Differences in number of sequences:"
diff -u "$FROMDIR"/sequence_list "$TODIR"/sequence_list

echo ""
echo ""

echo "Checking tables in $TODIR for differences from $FROMDIR"
echo ""

if [ $# -eq 3 ] ; then
    SUFFIX=_tbl.sql
else
    SUFFIX=_tbl.sql.mngl
fi

for table in `cat "$TODIR"/table_list` ; do
    echo =============================================================
	echo Table: $table
    echo =============================================================
    
    diff -u "$FROMDIR"/"$table"$SUFFIX \
            "$TODIR"/"$table"$SUFFIX

    echo ""
    echo ""
done
