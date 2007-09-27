@echo off
if "%OS%" == "Windows_NT" setlocal


SET SHELL_CP=
SET SHELL_HOME=%~dp0
cd %SHELL_HOME%

if not "%HQ_JAVA_HOME%"=="" goto gotCamJavaHome
if EXIST "%SHELL_HOME%\jre\bin\java.exe" goto useShellLocalJre
if not "%JAVA_HOME%"=="" goto gotjava
goto nojava

if not exist "%JAVA_HOME%\bin\java.exe" goto nojavaExe

:gotCamJavaHome
  set JAVA_HOME=%HQ_JAVA_HOME%
  goto gotjava

:useShellLocalJre
  set JAVA_HOME=%SHELL_HOME%\jre
  goto gotjava

:gotjava
for %%i in ("%SHELL_HOME%lib\*.jar") do call "%SHELL_HOME%\shellcp.bat" %%i

REM We need to quote the SHELL_CP variable in case the shell's install
REM path has spaces in it.  We do this after the variable has been set by
REM the previous two for loops.  If the SHELL_CP is ever modified after
REM this, the shell will stop working on Windows.
SET SHELL_CP="%SHELL_CP%"

REM logging stuff NOTE: NoOpLog turns off logging, SimpleLog turns on logging.
SET LOG=org.apache.commons.logging.impl.NoOpLog
REM LOG=org.apache.commons.logging.impl.SimpleLog

SET SYS_PROPS=-Dorg.apache.commons.logging.Log=%LOG% -Dlog4j.rootCategory=ERROR

SET MAIN_CLASS=org.hyperic.hq.bizapp.client.shell.ClientShell

rem Use this when communicating insecurely (regular way) with the server
rem echo cp %CLASSPATH%
"%JAVA_HOME%\bin\java" %SYS_PROPS% -cp %SHELL_CP% %MAIN_CLASS%

rem Use this when communicating securely with the server
rem #%JAVA% %SYS_PROPS% \
rem #   -Djava.security.manager \
rem #   -Djava.security.policy=%SPIDER_HOME%/app.policy \
rem #   -Djavax.net.ssl.trustStore=%SPIDER_HOME%/keys/my.keystore \
rem #   -classpath %CLASSPATH% %MAIN_CLASS%

goto done

:nojava
  echo HQ_JAVA_HOME or JAVA_HOME must be set when invoking the shell
  goto done

:nojavaExe
  echo HQ_JAVA_HOME\bin\java.exe does not exist
  goto done

:done
if "%OS%"=="Windows_NT" @endlocal
