#!/bin/bash

DEBUG=

basedir=`dirname $0`
this_script=`basename $0`

if [ -z "$JAVA_HOME" ]
then
    echo "ERROR: JAVA_HOME not defined"
    exit 1
fi

lflag=1
url="jdbc:postgresql://localhost:5432/hqdb?protocolVersion=2"
uflag=
pflag=
tflag=
iflag=

usage()
{
    printf "Usage: %s -l <url> -u <user> -p <passwd> -t <tables comma separated> -d <dest file>\n"  $this_script
    printf "default url = $url\nexample urls:\nmysql = jdbc:mysql://localhost:3306/hqdb\noracle = jdbc:oracle:thin:@localhost:1521:orcl\n"
}

while getopts 'id:p:l:u:t:' name
do
     case $name in
     i)     iflag=1
            import="--import";;
     p)     pflag=1
            passwd="$OPTARG";;
     l)     lflag=1
            url="$OPTARG";;
     u)     uflag=1
            user="$OPTARG";;
     t)     tflag=1
            tables="$OPTARG";;
     d)     dest="$OPTARG";;
     ?)     usage;
            exit 2;;
     esac
done
if [ ! -z "$iflag" ]; then
   [ "$DEBUG" ] && printf "Option --import specified\n"
fi
if [ ! -z "$lflag" ]; then
   [ "$DEBUG" ] && printf "Option --url $url specified\n"
else
    usage;
    exit 2
fi
if [ ! -z "$uflag" ]; then
   [ "$DEBUG" ] && printf "Option --user $user specified\n"
else
    usage;
    exit 2
fi
if [ ! -z "$tflag" ]; then
   [ "$DEBUG" ] && printf "Option --tables $tables specified\n"
else
    usage;
    exit 2
fi
if [ ! -z "$pflag" ]; then
   [ "$DEBUG" ] && printf "Option --passwd $passwd specified\n"
else
    usage;
    exit 2
fi
[ -z "$dest" ] && dest="$$-$tables"

[ `echo "$url" | grep -i postgresql` ] && DRIVER="org.postgresql.Driver"
[ `echo "$url" | grep -i mysql` ]      && DRIVER="com.mysql.jdbc.Driver"
[ `echo "$url" | grep -i oracle` ]     && DRIVER="oracle.jdbc.OracleDriver"

LIB="$basedir/../dbmigrate/lib/"
DBUNIT_PKGS="${LIB}/dbunit-2.2.jar"
PG_PKGS=${LIB}/oracle_jdbc/oracle12.jar 
ORA_PKGS=${LIB}/postgresql/postgresql-8.2-505.jdbc3.jar 
MYSQL_PKGS=${LIB}/mysql-connector-java-5.0.5-bin.jar
DB_PKGS="${PG_PKGS}:${ORA_PKGS}:${MYSQL_PKGS}"
HQ_SERVER_JAR="$basedir/../../hq-server/target/hq-server-4.6.5.BUILD-SNAPSHOT-tests.jar"
PKGS="${DB_PKGS}:${DBUNIT_PKGS}:${HQ_SERVER_JAR}"
echo $PKGS
ARGS="-Djdbc.drivers=${DRIVER} -cp ${PKGS}"
JAVA="${JAVA_HOME}/bin/java"

${JAVA} ${ARGS} org.hyperic.hq.db.TableExport --url $url --passwd $passwd --user $user --tables $tables --file $dest $import
