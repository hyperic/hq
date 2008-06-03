@echo off
setlocal

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _AGENTHOME=%~dp0..\

cd %_AGENTHOME%

set ROLLBACK_PROPS=conf\rollback.properties
set AGENT_BUNDLE_PROP=set.HQ_AGENT_BUNDLE

rem look for the agent bundle property in the rollback properties file
rem and invoke the bundle hq-agent.bat script
for /F "delims== tokens=1*" %%i in (%ROLLBACK_PROPS%) do (
  if "%%i"=="%AGENT_BUNDLE_PROP%" (
    if not EXIST bundles\%%j\bin\hq-agent.bat (
      echo Failed to find bundle script %_AGENTHOME%bundles\%%j\bin\hq-agent.bat.
    ) else (
      call bundles\%%j\bin\hq-agent.bat %*
    )
  )
)
