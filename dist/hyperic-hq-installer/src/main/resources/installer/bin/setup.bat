@echo off

SET INSTALLBASE=%~dp0
cd "%INSTALLBASE%"
call "%INSTALLBASE%\installer-@@@VERSION@@@\bin\hq-setup.bat" %*
