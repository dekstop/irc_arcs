#!/bin/sh
APP_HOME=`dirname "$0"`
java -classpath ${APP_HOME}/build/classes/:${APP_HOME}/lib/* fm.last.visualizations.irc.IrcArcs $@
