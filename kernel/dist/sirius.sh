#!/bin/bash
##############################################################################
# Made with all the love in the world
# by scireum in Remshalden, Germany
#
# Copyright by scireum GmbH
# http://www.scireum.de - info@scireum.de
##############################################################################
#
# Start / Stop script for SIRIUS applications
#
# Can be used to start or stop sirius based applications. This is compatible
# with SYSTEM V init.d
#
# A custom configuration can be provided via config.sh as this file should
# not be modified, since it's part of the release.
#
##############################################################################

echo "SIRIUS Launch Utility"
echo "====================="
echo ""

# Contains the java command to execute in order to start the system.
# By default, we assume that java is present in the PATH and can therefore
# be directly started.
JAVA_CMD="java"

# Java options to use. This should probably be customized according to the
# applications heap requirements
JAVA_OPTS="-Xmx1024m -XX:MaxPermSize=128M"

# Shutdown port used to signal the application to shut down. Used different
# ports for different apps or disaster will strike !
SHUTDOWN_PORT="9191"

# File used to pipe all stdout and stderr output to
STDOUT="logs/stdout.txt"

if [ -f config.sh ] 
then
	echo "Loading config.sh..."
	source config.sh
else
	echo "Use a custom config.sh to override the settings listed below"
fi

echo ""
echo "JAVA_CMD:      $JAVA_CMD"
echo "JAVA_OPTS:     $JAVA_OPTS"
echo "SHUTDOWN_PORT: $SHUTDOWN_PORT"
echo "STDOUT:        $STDOUT"

echo ""

case "$1" in
start)
	if [ -f $STDOUT ] 
	then 
		rm $STDOUT 
	fi
	echo "Starting Application..."
	$JAVA_CMD $JAVA_OPTS IPL >> $STDOUT &
        ;;

stop) 
        echo "Stopping Application..."
	$JAVA_CMD -Dkill=true -Dport=$SHUTDOWN_PORT IPL
        ;;

restart)
        echo "Stopping Application..."
        java -Dkill=true -Dport=$SHUTDOWN_PORT IPL
	if [ -f $STDOUT ] 
	then 
		rm $STDOUT 
	fi
        echo "Starting Application..."
        $JAVA_CMD $JAVA_OPTS IPL >> $STDOUT &
	;;

*)
        echo "Usage: sirius.sh start|stop|restart"
        exit 1
        ;;

esac
