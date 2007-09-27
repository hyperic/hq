#!/usr/bin/env python

import sys

if len(sys.argv) != 2:
	print "Syntax: fixup_sql.py <inFile>"
	sys.exit(1)


lines   = open(sys.argv[1]).readlines()
intable = False
columns = []

for line in lines:
	line = line.rstrip()

	if intable:
		if line.startswith(")"):
			# Dump sorted stuff
			columns.sort()
			print "\n".join(columns)
			print line
			intable = False
		else:
			# Add to the list of columns
			columns.append(line.rstrip(","))
	elif line.startswith("CREATE TABLE"):
		intable = True	
		columns = []
		print line
	else:
		print line
