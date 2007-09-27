#!/bin/sh

if [ $# -ne 1 ] ; then
	echo "Syntax: $0 tableDir"
    exit 1
fi

TABLEDIR="$1"

for i in "$TABLEDIR"/*_tbl.sql ; do
    # Fixup the SQL to have sorted columns and also remove the specifics
    # of foreign key names
    ./fixup_sql.py "$i" | \
		sed -E -e 's/[[:alnum:]_]+[[:blank:]]+FOREIGN KEY/FOREIGN KEY/' | \
        sed -E -e 's/DEFAULT[[:blank:]]+[^:blank:]]+//' \
    > "$i".mngl
	echo $i
done
