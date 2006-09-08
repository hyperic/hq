@echo off
REM
REM Connect to the HQ built-in database
REM

SET SERVER_HOME=%~dp0..
SET DB_HOME=%SERVER_HOME%\hqdb

cmd /c call "%DB_HOME%\bin\psql.exe" -U hqadmin -p @@@PGPORT@@@ hqdb
