@echo off

if "%1" == "profile" then set PROFILE=-a yourkit
if "%1" == "manage" then set PROFILE=-a jmx

set CWD=%~p0

rem set JAVA_HOME=/opt/jrockit

if not "%JAVA_OPTIONS%" == "" goto gotJavaOptions
set JAVA_OPTIONS=-Xms32M -Xmx512M
:gotJavaOptions

set JETTY_PORT=8181

set BUTTERFLY_OPTS=-Dbutterfly.name=backstage -Dbutterfly.log4j=%CWD%log4j.properties 
rem set BUTTERFLY_OPTS="-Djavax.activation.debug=true %BUTTERFLY_OPTS%"

if not "%BUTTERFLY_HOME%" == "" goto gotButterflyHome
set BUTTERFLY_HOME=%~p0..\butterfly\
echo set butterfly home
:gotButterflyHome

%BUTTERFLY_HOME%butterfly.bat %PROFILE% /u /backstage /b %CWD%butterfly.properties /c %CWD%modules.properties /p %CWD%modules
