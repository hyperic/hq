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
set _REALPATH=%~dp0..\wrapper\sbin

set SERVER_INSTALL_HOME=%~dp0..

rem
rem Detect HQ_JAVA_HOME or JAVA_HOME
rem
cd %_REALPATH%

rem Look for HQ_JAVA_HOME, then built-in JRE, then lastly JAVA_HOME.  If this should need to change, remove and re-install the service to update
if not "%HQ_JAVA_HOME%"=="" goto gothqjava
if EXIST "%SERVER_INSTALL_HOME%\jre" (
    set JAVA_HOME=%SERVER_INSTALL_HOME%\jre
    goto gotjava
)
if not "%JAVA_HOME%"=="" goto gotjava

:nojava
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
for /F %%v in ('echo %1^|findstr "^start$ ^stop$ ^restart$ ^install$ ^query$ ^remove$"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { start : stop : restart : install : query : remove}
    pause
    goto :eof
) else (
    shift
)

rem Check that the correct version of java is being used.
set REQUIRED_VERSION_STRING=1.6
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

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%SERVER_INSTALL_HOME%\conf\wrapper.conf"

rem SERVER_INSTALL_HOME in wrapper.conf needs to be set to an absolute path.
set wrapper_update1=set.SERVER_INSTALL_HOME=%SERVER_INSTALL_HOME%
set wrapper_update2=set.JAVA_HOME=%JAvA_HOME%


rem
rem Run the application.
rem At runtime, the current directory will be that of wrapper.exe
rem
call :%COMMAND%
if errorlevel 1 pause
goto :eof

:start
"%_WRAPPER_EXE%" -t %_WRAPPER_CONF% "%wrapper_update1%" "%wrapper_update2%"
goto :eof

:stop
"%_WRAPPER_EXE%" -p %_WRAPPER_CONF% "%wrapper_update1%" "%wrapper_update2%"
goto :eof

:install
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF% "%wrapper_update1%" "%wrapper_update2%"
goto :eof

:remove
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF% "%wrapper_update1%" "%wrapper_update2%"
goto :eof

:query
"%_WRAPPER_EXE%" -q %_WRAPPER_CONF% "%wrapper_update1%" "%wrapper_update2%"
goto :eof

:restart
call :stop
call :start
goto :eof

:exec
%*
goto :eof
