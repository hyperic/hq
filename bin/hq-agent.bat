@echo off
if "%OS%" == "Windows_NT" setlocal

set PDK_LIB=pdk\lib
set AGENT_LIB=lib

rem # ------------- 
rem # Shouldn't need to change anything below this
rem # -------------

SET RUNDIR=%~dp0
cd %RUNDIR%
if not "%HQ_JAVA_HOME%"=="" (
    set JAVA_HOME=%HQ_JAVA_HOME%
    goto gotjava
)
if EXIST %RUNDIR%\jre\nul (
    set JAVA_HOME=%RUNDIR%\jre
    goto gotjava
)
if "%JAVA_HOME%"=="" goto nojava

if not exist "%JAVA_HOME%\bin\java.exe" goto nojavaExe

:gotjava
set JAVA=%JAVA_HOME%\bin\java

set CLIENT_CLASSPATH=%AGENT_LIB%\AgentClient.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%\AgentServer.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%\lather.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-logging.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\log4j-1.2.14.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\jdom-1.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\hyperic-util.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\sigar.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\bcel-5.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\hq-product.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\ant.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\jakarta-oro-2.0.7.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\jxla.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-httpclient-3.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-codec-1.3.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\groovy-all-1.5.jar

set CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

set CLIENT_CMD="%JAVA%" -classpath "%CLIENT_CLASSPATH%" -Dagent.mode=thread %CLIENT_CLASS%

set START_CMD=%CLIENT_CMD% start
set PING_CMD=%CLIENT_CMD% ping
set SETUP_CMD=%CLIENT_CMD% setup
set DIE_CMD=%CLIENT_CMD% die

if /i "%1"=="start" (
    echo Starting agent
    %START_CMD%
    goto done
)
if /i "%1"=="run" (
    echo Running agent
    echo %START_CMD%
    %START_CMD%
    goto done
)
if /i "%1"=="stop" (
    %DIE_CMD% 10
    goto done
)
if /i "%1"=="ping" (
    echo Pinging ...
    %PING_CMD%
    if %errorlevel% EQU 0 (
        echo Success!
    ) else (
        echo Failure!
    )
    goto done
)
if /i "%1"=="setup" (
    %SETUP_CMD%
    goto done
)
echo Syntax %0 ^<start ^| stop ^| ping ^| setup^>
goto done

:nojava
  echo JAVA_HOME or HQ_JAVA_HOME must be set when invoking the agent
  goto done

:nojavaExe
  echo HQ_JAVA_HOME\bin\java.exe does not exist
  goto done

:done
if "%OS%"=="Windows_NT" @endlocal
