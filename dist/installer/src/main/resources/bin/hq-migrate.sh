INSTALL_DIR=`readlink -f $(dirname "$0")`
INSTALL_DIR="$INSTALL_DIR/.."
export INSTALL_DIR
ANT_HOME=$INSTALL_DIR
export ANT_HOME
export ANT_OPTS="$ANT_OPTS -Xmx5g -XX:MaxPermSize=128m"

cd $INSTALL_DIR

function usage() { 
	cat $INSTALL_DIR/data/reports/migration-usage.txt ; 
	exit ; 
}

if [[ ! $JAVA_HOME ]] ; then
	
	for arg in "$@"
	do
		if [[ "$arg" == "usage" ]]; then 
			usage ; 
	    elif [[ ! "$arg" =~ "=" ]]; then 
			arg="$arg=''"
		fi 
		namePart=${arg%%=*}
		echo name: $namePart
	 	if [[ "$namePart" == "-Dhqserver.install.path" ]]; then 
	 		JAVA_HOME=${arg#*=} ;
			JAVA_HOME="$JAVA_HOME/jre"
		elif [[ "$namePart" == "-Dsetup.file" ]]; then
			JAVA_HOME=`grep -m 1  -Po '(?<=hqserver.install.path=).*' "${arg#*=}"`/jre 				 	 						 								 				 	 						 					
	 	fi 			
	done
	   
	if [[ ! -d "$JAVA_HOME" ]]; then
		JAVA_HOME=$(readlink -f `which java`); 
	fi
	
	if [[ ! -d "$JAVA_HOME" ]]; then
		echo 'no jre'	
	else
		echo 'jre exists in $JAVA_HOME' 
		export JAVA_HOME
	fi
fi 	

echo JAVA_HOME: $JAVA_HOME
echo INSTALL_DIR: $INSTALL_DIR
echo ANT_HOME: $ANT_HOME 


ANT_OPTS="$ANT_OPTS -Djava.net.preferIPv4Stack=true -Dant.logger.defaults=${INSTALL_DIR}/data/ant.logger.defaults.properties -Dinstall.title=HQ-Migration" 
ANT_ARGS="" 
${ANT_HOME}/bin/ant --noconfig  \
    -Dinstall.dir=${INSTALL_DIR} \
    -Dinstall.mode=postgresql \
    -Dinstall.profile=large \
    -logger org.hyperic.tools.dbmigrate.Logger \
    -f ${INSTALL_DIR}/data/hq-migrate.xml \
	"$@"