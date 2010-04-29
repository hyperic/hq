@echo off

SET INSTALLBASE=%~dp0
cd "%INSTALLBASE%"
call "%INSTALLBASE%\installer\bin\hq-setup.bat" %*
