@echo off
REM
REM Start the built-in database
REM

SET SERVER_HOME=%~dp0..
SET DB_HOME=%SERVER_HOME%\hqdb

cmd /c call "%DB_HOME%\bin\pg_ctl.exe" -D "%DB_HOME%\data" -l "%SERVER_HOME%\logs\hqdb.log" start
