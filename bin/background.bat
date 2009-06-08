@echo off
rem for use by startup scripts such as jboss which do not background themselves.
rem see ServerControlPlugin

cmd /c start /b "" /MIN %*
