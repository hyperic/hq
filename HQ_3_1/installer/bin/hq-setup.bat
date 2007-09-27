@echo off
REM
REM Setup HQ on a system.
REM

IF "%OS%"=="Windows_NT" @setlocal

REM %~dp0 is expanded pathname of the current script under NT
SET INSTALL_DIR=%~dp0..
cd %INSTALL_DIR%
SET USE_BUILTIN_JRE=0

SET CLASSPATH=
SET HASJRE=

if exist "%INSTALL_DIR%\jres" goto haveBuiltinJRE
if not "%JAVA_HOME%"=="" goto haveJavaHome
goto noJavaHome

:haveJavaHome
if exist "%JAVA_HOME%\bin\java.exe" goto basicAntSetup
goto noJavaExe

:haveBuiltinJRE:
"%INSTALL_DIR%\jres\x86-win32-jre.exe" -d "%TEMP%" > nul
SET JAVA_HOME=%TEMP%\jre
SET USE_BUILTIN_JRE=1
goto basicAntSetup

:basicAntSetup
SET ANT_HOME=%INSTALL_DIR%

FOR /F "delims=~" %%i IN ('date /T') do SET INSTALL_DATE=%%i
FOR /F %%i IN ('time /T') do SET INSTALL_TIME=%%i

:startEcho
REM echo INSTALL_DIR=%INSTALL_DIR%
REM echo ANT_HOME=%ANT_HOME%

if exist "%ANT_HOME%\bin\ant.bat" goto installHQ
if not exist "%ANT_HOME%\bin\ant.bat" goto noAnt

:installHQ
SET INSTALL_MODE=quick
:handleSetupParam
if "%1"=="" goto startSetup
if "%1"=="-upgrade" SET INSTALL_MODE=upgrade
if "%1"=="-oracle" SET INSTALL_MODE=oracle
if "%1"=="-postgresql" SET INSTALL_MODE=postgresql
if "%1"=="-mysql" SET INSTALL_MODE=mysql
if "%1"=="-full" SET INSTALL_MODE=full
rem Didn't match an option, assume it if the file we use
if not "%INSTALL_MODE%"=="quick" goto finishArgs
if not "%1"=="" set SETUP_FILE=%1
:finishArgs
shift
goto handleSetupParam

:startSetup
if "%SETUP_FILE%"=="" goto defaultSetup
echo "using setup with file=%SETUP_FILE%"
call "%ANT_HOME%\bin\ant" -Dinstall.dir="%INSTALL_DIR%" -Dinstall.mode="%INSTALL_MODE%" -Dsetup="%SETUP_FILE%" -f "%INSTALL_DIR%\data\setup.xml" -logger org.hyperic.tools.ant.installer.InstallerLogger
goto setupDone

:defaultSetup
call "%ANT_HOME%\bin\ant" -Dinstall.dir="%INSTALL_DIR%" -Dinstall.nowrap=false -Dinstall.mode="%INSTALL_MODE%" -logger org.hyperic.tools.ant.installer.InstallerLogger -f "%INSTALL_DIR%\data\setup.xml"
goto setupDone

:noJavaHome
echo No JAVA_HOME environment variable is defined.
goto setupDone

:noJavaExe
echo JAVA_HOME\bin\java.exe does not exist
goto setupDone

:badJava
echo Java version must be 1.3 or greater
goto setupDone

:noAnt
echo There was an error in the installation script: ant could not be found.
goto setupDone

:setupDone
if not "%USE_BUILTIN_JRE%"=="1" goto bottom
echo Deleting temporary JRE
rd /q /s "%TEMP%\jre"

:bottom
if "%OS%"=="Windows_NT" @endlocal
