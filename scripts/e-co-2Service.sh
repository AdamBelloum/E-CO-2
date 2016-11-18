#From https://stackoverflow.com/questions/11203483/run-a-java-application-as-a-service-on-linux

#!/bin/sh
SERVICE_NAME=e-co-2_service
PATH_TO_JAR=$HOME/workspace/E-CO-2/rest/target/rest-1.0-SNAPSHOT-jar-with-dependencies.jar
PID_PATH_NAME=/tmp/e-co-2_service-pid
PROPS_PATH=$HOME/workspace/E-CO-2/etc/configure.properties
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            nohup java -jar $PATH_TO_JAR $PROPS_PATH /tmp 2>> /dev/null >> /dev/null & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            nohup java -jar $PATH_TO_JAR $PROPS_PATH /tmp 2>> /dev/null >> /dev/null & echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac 
