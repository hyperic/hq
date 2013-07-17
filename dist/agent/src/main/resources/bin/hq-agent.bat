@echo off
setlocal

rem Copyright (c) 1999, 2006 Tanuki Software Inc.
rem
rem Java Service Wrapper command based script
rem

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0..\..\..\wrapper\sbin

set AGENT_INSTALL_HOME=%~dp0..\..\..
set AGENT_BUNDLE_HOME=%~dp0..

rem
rem Detect HQ_JAVA_HOME
rem
cd %_REALPATH%

if not "%HQ_JAVA_HOME%"=="" goto gothqjava

if EXIST "%AGENT_BUNDLE_HOME%"\jre (
    set HQ_JAVA_HOME=%AGENT_BUNDLE_HOME%\jre
    goto gotjava
)

if EXIST "%AGENT_INSTALL_HOME%"\jre (
    set HQ_JAVA_HOME=%AGENT_INSTALL_HOME%\jre
    goto gotjava
)

:nojava
  echo HQ_JAVA_HOME must be set when invoking the agent
goto :eof

:gothqjava
if not EXIST "%HQ_JAVA_HOME%" (
  echo HQ_JAVA_HOME must be set to a valid directory
  goto :eof
) else (
  set HQ_JAVA_HOME=%HQ_JAVA_HOME%
)

:gotjava
rem Decide on the wrapper binary.
set _WRAPPER_BASE=wrapper
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto validate
echo Unable to locate a Wrapper executable using any of the following names:
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-32.exe
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-64.exe
echo %_REALPATH%\%_WRAPPER_BASE%.exe
pause
goto :eof

:validate
rem Find the requested command.
for /F %%v in ('echo %1^|findstr "^start$ ^stop$ ^restart$ ^install$ ^remove$ ^query$ ^ping$ ^setup"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { start : stop : restart : install : remove : query : ping : setup : set-property }
    pause
    goto :eof
) else (
    shift
)

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%AGENT_INSTALL_HOME%\conf\wrapper.conf"

rem
rem Set some HQ properties
rem
set AGENT_BUNDLE_HOME_PROP=agent.bundle.home
set AGENT_INSTALL_HOME_PROP=agent.install.home
set AGENT_LIB=%AGENT_BUNDLE_HOME%\lib
set PDK_LIB=%AGENT_BUNDLE_HOME%\pdk\lib

set CLIENT_CLASSPATH=%AGENT_LIB%\hq-agent-core-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\hq-common-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\hq-util-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\hq-pdk-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\ant-1.7.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-logging-1.0.4.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\log4j-1.2.14.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\sigar-${sigar.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\httpclient-4.1.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\httpcore-4.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\spring-core-3.0.5.RELEASE.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-codec-1.2.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%\hq-lather-${project.version}.jar

set CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

set CLIENT_CMD="%HQ_JAVA_HOME%\bin\java" -Djava.net.preferIPv4Stack=true -D%AGENT_INSTALL_HOME_PROP%="%AGENT_INSTALL_HOME%" -D%AGENT_BUNDLE_HOME_PROP%="%AGENT_BUNDLE_HOME%" -cp "%CLIENT_CLASSPATH%" %CLIENT_CLASS%

set PING_CMD=%CLIENT_CMD% ping
set SETUP_CMD=%CLIENT_CMD% setup
set SETUP_IF_NO_PROVIDER_CMD=%CLIENT_CMD% setup-if-no-provider
set wrapper_update1=set.HQ_JAVA_HOME=%HQ_JAVA_HOME%

rem
rem Run the application.
rem At runtime, the current directory will be that of wrapper.exe
rem
call :%COMMAND%
if errorlevel 1 pause
goto :eof

:start
"%_WRAPPER_EXE%" -t %_WRAPPER_CONF% "%wrapper_update1%"
ping -3 XXX 127.0.0.1 >nul
call :setup-if-no-provider
goto :eof

:stop
"%_WRAPPER_EXE%" -p %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:install
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:remove
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:query
"%_WRAPPER_EXE%" -q %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof

rem
rem HQ Agent specific commands
rem

:ping
%PING_CMD%
IF %ERRORLEVEL% NEQ 0 (echo Ping failed!) else echo Ping success!
goto :eof

:setup
%SETUP_CMD%
goto :eof

:setup-if-no-provider
%SETUP_IF_NO_PROVIDER_CMD%
goto :eof
