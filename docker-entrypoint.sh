#!/bin/sh

PORT_VALUE="${PORT:-8080}"

sed -i "s/port=\"8080\"/port=\"$PORT_VALUE\"/" /opt/tomcat/conf/server.xml

exec catalina.sh run