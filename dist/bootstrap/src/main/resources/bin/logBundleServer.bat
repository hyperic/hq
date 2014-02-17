@echo off

::SET EXECUTION_DIR=%cd%
SET EXECUTION_DIR=%~dp0

:: Get Hyperic dir
SET INSTALLBASE=%~dp0

ECHO EXECUTION_DIR: %EXECUTION_DIR%
ECHO INSTALLBASE: %INSTALLBASE%

cd %INSTALLBASE%
:: Location of Hyperic server logs.
SET LOGS_DIR="..\logs"
SET CONF_DIR="..\conf"
SET STAT_DIR="..\hq-engine\hq-server\logs\hqstats"
SET WEB_INF="..\hq-engine\hq-server\webapps\ROOT\WEB-INF"

SET HQDATE=%date:~10,4%%date:~4,2%%date:~7,2%
SET PLATNAME=%computername%

SET SP_DIR_NAME=hyperic_server_SP_%PLATNAME%_%HQDATE%
SET SP_TMP_DIR=%TMP%\%SP_DIR_NAME%
SET SP_NAME="%SP_DIR_NAME%.zip"
SET SP_TMP_DIR_SERVER_LOGS=%SP_TMP_DIR%\server-logs
SET SP_TMP_DIR_SERVER_CONF=%SP_TMP_DIR%\server-conf
SET SP_TMP_DIR_SERVER_STAT=%SP_TMP_DIR%\server-stat
SET SP_TMP_DIR_SERVER_WEB_INF=%SP_TMP_DIR%\WEB-INF
SET SP_ERROR_LOG=%SP_TMP_DIR%\supportpackage.log
SET SP_INFO_LOG=%SP_TMP_DIR%\supportpackageInfo.log

ECHO.
ECHO Creating tmp directories...
mkdir %SP_TMP_DIR%
mkdir %SP_TMP_DIR_SERVER_LOGS%
mkdir %SP_TMP_DIR_SERVER_CONF%
mkdir %SP_TMP_DIR_SERVER_STAT%
mkdir %SP_TMP_DIR_SERVER_WEB_INF%

:: Copying logs and files
ECHO.
ECHO Copying files...
:: COPY %LOGS_DIR%\wrapper.log %SP_TMP_DIR_SERVER_LOGS%
:: COPY %LOGS_DIR%\server.log %SP_TMP_DIR_SERVER_LOGS%
:: COPY %LOGS_DIR%\bootstrap.log %SP_TMP_DIR_SERVER_LOGS%
COPY %LOGS_DIR%\*.* %SP_TMP_DIR_SERVER_LOGS%
COPY %CONF_DIR%\wrapper.conf %SP_TMP_DIR_SERVER_CONF%
COPY %CONF_DIR%\hq-server.conf %SP_TMP_DIR_SERVER_CONF%
COPY %CONF_DIR%\server-log4j.xml %SP_TMP_DIR_SERVER_CONF%
COPY %CONF_DIR%\log4j.xml %SP_TMP_DIR_SERVER_CONF%
COPY %WEB_INF%\classes\ehcache.xml %SP_TMP_DIR_SERVER_WEB_INF%
COPY %WEB_INF%\classes\ApplicationResources.properties %SP_TMP_DIR_SERVER_WEB_INF%

:: Copying HQ Stats
ECHO.
ECHO Copying Stats...
COPY %STAT_DIR%\hqstats-*.csv %SP_TMP_DIR_SERVER_STAT%
COPY %STAT_DIR%\hqstats-*.gz %SP_TMP_DIR_SERVER_STAT%
ECHO Finished Copying Stats...

:: Windows Specific System information
ECHO.
:: Copying Hyperic Service Registry Key
ECHO Copying Hyperic Service Registry Key...
> %SP_TMP_DIR_SERVER_CONF%\hyperic-server-reg.txt REG QUERY "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\services\Hyperic HQ Server" 
>> %SP_INFO_LOG% ECHO ############# Hostname ##############
>> %SP_INFO_LOG% HOSTNAME
>> %SP_INFO_LOG% ECHO.
>> %SP_INFO_LOG% ECHO ############# Interface Configuration #############
>> %SP_INFO_LOG% IPCONFIG /all
>> %SP_INFO_LOG% ECHO.
>> %SP_INFO_LOG% ECHO ############# NBTSTAT #############
>> %SP_INFO_LOG% NBTSTAT -n
>> %SP_INFO_LOG% ECHO.
>> %SP_INFO_LOG% ECHO ############# Netstat TCP Info #############
>> %SP_INFO_LOG% NETSTAT -anb -p tcp
>> %SP_INFO_LOG% ECHO.
:: >> %SP_INFO_LOG% ECHO ############# Listing of Dumps #############
:: >> %SP_INFO_LOG% DIR %systemroot%\*.dmp
:: >> %SP_INFO_LOG% ECHO.
>> %SP_INFO_LOG% ECHO ############# Environment #############
>> %SP_INFO_LOG% SET
>> %SP_INFO_LOG% ECHO.

>> %SP_INFO_LOG% ECHO ############# Services Started #############
>> %SP_INFO_LOG% NET START
>> %SP_INFO_LOG% ECHO.

ECHO Finished Copying Hyperic Service Registry Key...

:: Run MSINFO Report if exists
:: IF EXIST "%CommonProgramFiles%\Microsoft Shared\MSInfo\msinfo32.exe" (
::
::	>> %SP_INFO_LOG% ECHO ############# System and Network Summary ##############
::	>> %SP_INFO_LOG% ECHO Available in CONF directory
:: 	start /wait msinfo32.exe /report "%CONF_DIR%\msinfo-sum.txt" /categories +SystemSummary
:: 	start /wait msinfo32.exe /report "%CONF_DIR%\msinfo-net.txt" /categories +ComponentsNetwork
:: 	)
:: ECHO .

:: Compression
:: Create VBS script
    echo Set objArgs = WScript.Arguments > _zipIt.vbs
    echo InputFolder = objArgs(0) >> _zipIt.vbs
    echo ZipFile = objArgs(1) >> _zipIt.vbs
    echo CreateObject("Scripting.FileSystemObject").CreateTextFile(ZipFile, True).Write "PK" ^& Chr(5) ^& Chr(6) ^& String(18, vbNullChar) >> _zipIt.vbs
    echo Set objShell = CreateObject("Shell.Application") >> _zipIt.vbs
    echo Set source = objShell.NameSpace(InputFolder).Items >> _zipIt.vbs
    echo objShell.NameSpace(ZipFile).CopyHere(source) >> _zipIt.vbs
    echo wScript.Sleep 2000 >> _zipIt.vbs

ECHO  Compressing file to %EXECUTION_DIR%%SP_NAME%
START /WAIT CScript  _zipIt.vbs  %SP_TMP_DIR%  %EXECUTION_DIR%%SP_NAME%

:: Cleanup
rmdir /S /Q %SP_TMP_DIR%
del /Q _zipIt.vbs

:: Open Explorer Window after completing script
:: explorer %Temp%
cd %EXECUTION_DIR%
explorer %EXECUTION_DIR%
