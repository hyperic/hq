@echo off
REM
REM Start the built-in database
REM

SET SERVER_HOME=%~dp0..
SET DB_HOME=%SERVER_HOME%\hqdb

cmd /c call "%DB_HOME%\bin\pg_ctl.exe" start -s -o -i -l "%SERVER_HOME%\logs\hqdb.log" -D "%DB_HOME%\data"
