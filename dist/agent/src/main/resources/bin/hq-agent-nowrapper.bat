@echo off
if "%OS%" == "Windows_NT" setlocal
setLocal EnableDelayedExpansion

set PDK_LIB=..\pdk\lib
set AGENT_LIB=..\lib
set AGENT_BUNDLE_HOME=..
set AGENT_INSTALL_HOME=..\..\..
set AGENT_BUNDLE_HOME_PROP=agent.bundle.home
set AGENT_INSTALL_HOME_PROP=agent.install.home
set WRAPPER_PATH=%LD_LIBRARY_PATH%;..\..\..\wrapper\lib
set CLIENT_CLASSPATH_1=%AGENT_LIB%
set CLIENT_CLASSPATH_2=

rem # ------------- 
rem # Shouldn't need to change anything below this
rem # -------------

SET RUNDIR=%~dp0

cd %RUNDIR%
if not "%HQ_JAVA_HOME%"=="" (
    set JAVA_HOME=%HQ_JAVA_HOME%
    goto gotjava
)
if EXIST %AGENT_BUNDLE_HOME%\jre\nul (
    set JAVA_HOME=%AGENT_BUNDLE_HOME%\jre
    goto gotjava
)
if EXIST %RUNDIR%\jre\nul (
    set JAVA_HOME=%RUNDIR%\jre
    goto gotjava
)
if "%JAVA_HOME%"=="" goto nojava

if not exist "%JAVA_HOME%\bin\java.exe" goto nojavaExe

:gotjava
set JAVA=%JAVA_HOME%\bin\java


for /F  %%a in ('dir /b %AGENT_LIB%\*.jar') do (
  set CLIENT_CLASSPATH_1=!CLIENT_CLASSPATH_1!;%AGENT_LIB%\%%a
)
set CLIENT_CLASSPATH_1=!CLIENT_CLASSPATH_1!

for /F  %%a in ('dir /b %PDK_LIB%\*.jar') do (
  set CLIENT_CLASSPATH_2=!CLIENT_CLASSPATH_2!;%PDK_LIB%\%%a
)
set CLIENT_CLASSPATH_2=!CLIENT_CLASSPATH_2!

for /F  %%a in ('dir /b %PDK_LIB%\jdbc\*.jar') do (
  set CLIENT_CLASSPATH_3=!CLIENT_CLASSPATH_3!;%PDK_LIB%\%%a
)
set CLIENT_CLASSPATH_3=!CLIENT_CLASSPATH_3!

for /F  %%a in ('dir /b %PDK_LIB%\mx4j\*.jar') do (
  set CLIENT_CLASSPATH_4=!CLIENT_CLASSPATH_4!;%PDK_LIB%\%%a
)
set CLIENT_CLASSPATH_4=!CLIENT_CLASSPATH_4!

set CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

set CLIENT_CMD="%JAVA%" -Dwrapper.native_library=wrapper -Djava.library.path=%java.library.path%;%WRAPPER_PATH% -D%AGENT_INSTALL_HOME_PROP%="%AGENT_INSTALL_HOME%" -D%AGENT_BUNDLE_HOME_PROP%="%AGENT_BUNDLE_HOME%" -classpath "%CLIENT_CLASSPATH_1%;%CLIENT_CLASSPATH_2%;%CLIENT_CLASSPATH_3%;%CLIENT_CLASSPATH_4%;" %CLIENT_CLASS%

set START_CMD=%CLIENT_CMD% start
set PING_CMD=%CLIENT_CMD% ping
set SETUP_CMD=%CLIENT_CMD% setup
set DIE_CMD=%CLIENT_CMD% die
set SET_PROP_CMD=%CLIENT_CMD% %*

if /i "%1"=="start" (
    echo Starting agent
    %START_CMD%
    goto done
)
if /i "%1"=="run" (
    echo Running agent
    echo %START_CMD%
    %START_CMD%
    goto done
)
if /i "%1"=="stop" (
    %DIE_CMD% 10
    goto done
)
if /i "%1"=="ping" (
    echo Pinging ...
    %PING_CMD%
    if %errorlevel% EQU 0 (
        echo Success!
    ) else (
        echo Failure!
    )
    goto done
)
if /i "%1"=="setup" (
    %SETUP_CMD%
    goto done
)
if /i "%1"=="set-property" (
    %SET_PROP_CMD%
    goto done
)
echo Syntax %0 ^<start ^| stop ^| ping ^| setup^>
goto done

:nojava
  echo JAVA_HOME or HQ_JAVA_HOME must be set when invoking the agent
  goto done

:nojavaExe
  echo HQ_JAVA_HOME\bin\java.exe does not exist
  goto done

:done
if "%OS%"=="Windows_NT" @endlocal
