@echo off
REM 
REM HQ migration 
REM

setlocal enableDelayedExpansion enableExtensions  

REM %~dp0 is expanded pathname of the current script under NT
SET INSTALL_DIR=%~dp0..
cd %INSTALL_DIR%

SET ANT_HOME=%INSTALL_DIR%

REM echo INSTALL_DIR=%INSTALL_DIR%
REM echo ANT_HOME=%ANT_HOME%

SET ANT_OPTS=-Xmx1024m

REM if there was no java home env var attempt to derive the JAVA_HOME from the hyperic server installation
REM provided as a cmd arg -Dhqserver.install.path   

set next= 
  
FOR %%J IN (%*) DO (
	if "%%J" == "usage" (
		goto USAGE
	)else if "%%J" == "help" (
		goto USAGE
	)else if "%%J" == "-Dhqserver.install.path" (
		set next=hqserver.install.path
	)else if "!next!" == "hqserver.install.path" ( 
	   set JAVA_HOME=%%J\jre
	   set next=""
	)else if "%%J" == "-Dscale" (
		set next=scale
	)else if "!next!" == "scale" (
		
		if "%%J" == "true" ( 
			set ANT_OPTS=-Xmx5000m
		) 
		
		set next=""
	)else if "%%J" == "-Dsetup.file" (
		set next=setup.file
	)else if "!next!" == "setup.file" (
		
		for /F "delims== tokens=1-6" %%A in ('"findstr /r hqserver.install.path=.* %%J"') do ( 
			set JAVA_HOME=%%B\jre
			
			if exist "!JAVA_HOME!" ( 
			  echo "derived JAVA_HOME location from setup file %%J"
			rem  goto JAVA_HOME_FOUND
			) 
		) 
		
		set next=""
					
	)
) 
	
if "%JAVA_HOME%" == "" ( 
 goto JAVA_HOME_UNDEFINED
)else ( 
 goto JAVA_HOME_FOUND
)
  
:USAGE 
type %INSTALL_DIR%\data\reports\migration-usage.txt 
goto :EOF 
	   
:JAVA_HOME_FOUND
set JAVA_HOME=!JAVA_HOME!

echo "Using JAVA_HOME: %JAVA_HOME%" 

set ANT_OPTS=%ANT_OPTS% -XX:MaxPermSize=128m -Djava.net.preferIPv4Stack=true -Dinstall.title=HQ-Migration 
set ANT_ARGS="" 
call %ANT_HOME%\bin\ant -debug -Dinstall.dir=%INSTALL_DIR% -Dinstall.mode=postgresql -Dinstall.profile=large -logger org.hyperic.tools.dbmigrate.Logger -f %INSTALL_DIR%\data\hq-migrate.xml %*
goto :EOF 

:JAVA_HOME_UNDEFINED
echo "JAVA_HOME was undefined and Hyperic server's JRE does not exist, aborting!"

:EOF 