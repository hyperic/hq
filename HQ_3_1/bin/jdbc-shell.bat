@echo off
setlocal

rem Skip to the real work if we have been called in a sub-shell
if defined SUB_SHELL goto start

rem Turn on delayed environment variable expansion for the for loops
set SUB_SHELL=TRUE
cmd /V /C %~dp0/jdbc-shell.bat %*
goto done

:start
set CAM_HOME=%~dp0..

rem Make sure we have JAVA_HOME
if "%JAVA_HOME%"=="" goto nojava

set JDBC_LIB=%CAM_HOME%\thirdparty\lib
set ORACLE_LIB=%JDBC_LIB%\oracle_jdbc
set JDBC_CLASSPATH=%JDBC_LIB%\henplus.jar
set JDBC_CLASSPATH=%JDBC_CLASSPATH%;%JDBC_LIB%\libreadline-java.jar
set JDBC_CLASSPATH=%JDBC_CLASSPATH%;%JDBC_LIB%\postgresql\postgresql-7.4.3.jar
set JDBC_CLASSPATH=%JDBC_CLASSPATH%;%JDBC_LIB%\mysql_jdbc\mysql-connector-java-5.0.5-bin.jar

for %%f in (%ORACLE_LIB%\*.jar) do set JDBC_CLASSPATH=!JDBC_CLASSPATH!;%ORACLE_LIB%\%%~nf.jar

if defined %1 goto launch
set JDBC_SHELL_ARGS=jdbc:pointbase:server://localhost/covalent_cam pbpublic pbpublic

set JAVA=%JAVA_HOME%\bin\java
set JDBC_CLIENT_CLASS=henplus.HenPlus

%JAVA% -cp %JDBC_CLASSPATH% %JDBC_CLIENT_CLASS% %JDBC_SHELL_ARGS% %*
goto done

:nojava
echo Environment variable JAVA_HOME not defined.

:done
endlocal
