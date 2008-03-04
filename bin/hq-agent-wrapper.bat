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
set _REALPATH=%~dp0..\

rem
rem Detect JAVA_HOME
rem
cd %_REALPATH%

if not "%JAVA_HOME%"=="" goto gotjava

if not "%HQ_JAVA_HOME%"=="" (
    set JAVA_HOME=%HQ_JAVA_HOME%
    goto gotjava
)
if EXIST %_REALPATH%\jre\nul (
    set JAVA_HOME=%_REALPATH%\jre
    goto gotjava
)

:nojava
  echo JAVA_HOME or HQ_JAVA_HOME must be set when invoking the agent
goto :eof

:gotjava
rem Decide on the wrapper binary.
set _WRAPPER_BASE=sbin\wrapper
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto validate
echo Unable to locate a Wrapper executable using any of the following names:
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe
echo %_REALPATH%%_WRAPPER_BASE%.exe
pause
goto :eof

:validate
rem Find the requested command.
for /F %%v in ('echo %1^|findstr "^console$ ^start$ ^stop$ ^restart$ ^install$ ^remove$ ^query$ ^ping$ ^setup"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { console : start : stop : restart : install : remove : query : ping : setup }
    pause
    goto :eof
) else (
    shift
)

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%_REALPATH%\conf\wrapper.conf"

rem
rem Run the application.
rem At runtime, the current directory will be that of wrapper.exe
rem
call :%COMMAND%
if errorlevel 1 pause
goto :eof

:console
"%_WRAPPER_EXE%" -c %_WRAPPER_CONF%
goto :eof

:start
"%_WRAPPER_EXE%" -t %_WRAPPER_CONF%
goto :eof

:stop
"%_WRAPPER_EXE%" -p %_WRAPPER_CONF%
goto :eof

:install
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF%
goto :eof

:remove
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF%
goto :eof

:query
"%_WRAPPER_EXE%" -q %_WRAPPER_CONF%
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
"%JAVA_HOME%\bin\java" -Djava.compiler=NONE -Djava.security.auth.login.config=jaas.config -Dagent.mode=thread -Xmx256M -Xrs -Xmx128m -Djava.net.preferIPv4Stack=true -Djava.library.path="lib" -classpath "lib\AgentClient.jar;lib\AgentServer.jar;lib\lather.jar;pdk\lib\activation.jar;pdk\lib\ant.jar;pdk\lib\backport-util-concurrent.jar;pdk\lib\commons-beanutils.jar;pdk\lib\commons-codec-1.3.jar;pdk\lib\commons-collections.jar;pdk\lib\commons-httpclient-2.0.jar;pdk\lib\commons-logging.jar;pdk\lib\dnsjava-2.0.3.jar;pdk\lib\getopt.jar;pdk\lib\hq-product.jar;pdk\lib\hyperic-util.jar;pdk\lib\jakarta-oro-2.0.7.jar;pdk\lib\jdom_b8.jar;pdk\lib\jsch-0.1.34.jar;pdk\lib\json.jar;pdk\lib\junit.jar;pdk\lib\jxla.jar;pdk\lib\log4j-1.2.14.jar;pdk\lib\sigar.jar;pdk\lib\snmp4j.jar;pdk\lib\tomcat-jk.jar;pdk\lib\ws-commons-util-1.0.2.jar;pdk\lib\xalan.jar;pdk\lib\xml-apis.jar;pdk\lib\xmlrpc-client-3.1.jar;pdk\lib\xmlrpc-common-3.1.jar;pdk\lib\xpp3_min-1.1.3.4.O.jar;pdk\lib\xstream-1.2.1.jar" org.hyperic.hq.bizapp.agent.client.AgentClient ping
IF %ERRORLEVEL% NEQ 0 (echo Ping failed!) else echo Ping success!
goto :eof

:setup
"%JAVA_HOME%\bin\java" -Djava.compiler=NONE -Djava.security.auth.login.config=jaas.config -Dagent.mode=thread -Xmx256M -Xrs -Xmx128m -Djava.net.preferIPv4Stack=true -Djava.library.path="lib" -classpath "lib\AgentClient.jar;lib\AgentServer.jar;lib\lather.jar;pdk\lib\activation.jar;pdk\lib\ant.jar;pdk\lib\backport-util-concurrent.jar;pdk\lib\commons-beanutils.jar;pdk\lib\commons-codec-1.3.jar;pdk\lib\commons-collections.jar;pdk\lib\commons-httpclient-2.0.jar;pdk\lib\commons-logging.jar;pdk\lib\dnsjava-2.0.3.jar;pdk\lib\getopt.jar;pdk\lib\hq-product.jar;pdk\lib\hyperic-util.jar;pdk\lib\jakarta-oro-2.0.7.jar;pdk\lib\jdom_b8.jar;pdk\lib\jsch-0.1.34.jar;pdk\lib\json.jar;pdk\lib\junit.jar;pdk\lib\jxla.jar;pdk\lib\log4j-1.2.14.jar;pdk\lib\sigar.jar;pdk\lib\snmp4j.jar;pdk\lib\tomcat-jk.jar;pdk\lib\ws-commons-util-1.0.2.jar;pdk\lib\xalan.jar;pdk\lib\xml-apis.jar;pdk\lib\xmlrpc-client-3.1.jar;pdk\lib\xmlrpc-common-3.1.jar;pdk\lib\xpp3_min-1.1.3.4.O.jar;pdk\lib\xstream-1.2.1.jar" org.hyperic.hq.bizapp.agent.client.AgentClient setup
goto :eof
