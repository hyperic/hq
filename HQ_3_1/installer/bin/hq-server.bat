@echo off
REM
REM Start/Stop the HQ server.
REM

IF "%OS%"=="Windows_NT" @setlocal

REM %~dp0 is expanded pathname of the current script under NT
SET SERVER_HOME=%~dp0..
cd %SERVER_HOME%
SET ENGINE_HOME=%SERVER_HOME%\hq-engine
SET SERVER_LOG=%SERVER_HOME%\logs\hq-server.log
SET ANT_HOME=%SERVER_HOME%
SET CLASSPATH=

if not "%HQ_JAVA_HOME%"=="" (
    set JAVA_HOME=%HQ_JAVA_HOME%
    goto gotjava
)
if EXIST %SERVER_HOME%\jre\nul (
    set JAVA_HOME=%SERVER_HOME%\jre
    goto gotjava
)
if "%JAVA_HOME%"=="" goto nojava

if not exist "%JAVA_HOME%\bin\java.exe" goto nojavaExe

REM check java version
for /F %%i IN ('"%JAVA_HOME%\bin\java.exe" -classpath .\lib\hq-installer.jar org.hyperic.util.JavaVersion --atLeast 1.4') do SET JVM_VERSION_OK=%%i
if "%JVM_VERSION_OK%"=="0" goto badJava
if "%JVM_VERSION_OK%"=="" goto badJava

:gotjava
if "%1" == "start" goto runHQ
if "%1" == "stop" goto runHQ
if "%1" == "halt" goto runHQ
goto usage

:runHQ
if not exist "%ANT_HOME%\bin\ant.bat" goto noAnt

start /B cmd /C ""%ANT_HOME%\bin\ant" -q -Dserver.home="%SERVER_HOME%" -Dengine.home="%ENGINE_HOME%" -Dlog="%SERVER_LOG%" -logger org.hyperic.tools.ant.installer.InstallerLogger -f "%SERVER_HOME%\data\server.xml" %1"
goto done

:noAnt
  echo The ant program could not be found.
  goto done

:nojava
  echo Environment variable HQ_JAVA_HOME not defined.
  goto done

:nojavaExe
  echo HQ_JAVA_HOME\bin\java.exe does not exist
  goto done

:badJava
  echo Java version must be 1.4 or greater
  goto done

:usage
  echo Usage: %0 start 1>&2
  echo    or: %0 stop 1>&2
  goto done

:done
  if "%OS%"=="Windows_NT" @endlocal

