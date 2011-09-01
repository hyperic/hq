@echo off
setlocal

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
set INSTALL_HOME=%~dp0..

rem Look for HQ_JAVA_HOME, then built-in JRE, then lastly JAVA_HOME.  If this should need to change, remove and re-install the service to update
if not "%HQ_JAVA_HOME%"=="" goto gothqjava
if EXIST "%INSTALL_HOME%\jre" (
    set JAVA_HOME=%INSTALL_HOME%\jre
    goto gotjava
)
if not "%JAVA_HOME%"=="" goto gotjava

echo JAVA_HOME or HQ_JAVA_HOME must be set when invoking the server
goto :eof

:gothqjava
if not EXIST "%HQ_JAVA_HOME%" (
  echo HQ_JAVA_HOME must be set to a valid directory
  goto :eof
) else (
  set JAVA_HOME=%HQ_JAVA_HOME%
)

:gotjava
rem Check that the correct version of java is being used.
set REQUIRED_VERSION_STRING=1.5
"%JAVA_HOME%/bin/java" -version 2> tmp_java_version.txt
set /p JAVA_VERSION= < tmp_java_version.txt
del tmp_java_version.txt
set JAVA_VERSION=%JAVA_VERSION:~14,3%
set REQUIRED_VERSION=%REQUIRED_VERSION_STRING:~0,3%
if %JAVA_VERSION% LSS %REQUIRED_VERSION% (
  echo Java version must be at least %REQUIRED_VERSION_STRING%
  pause
  goto :eof
)

rem Note - Development machine is not supported on windows for now

%JAVA_HOME%\bin\java -jar %INSTALL_HOME%\support\lib\jython.jar %INSTALL_HOME%\support\scripts\support.py %*
