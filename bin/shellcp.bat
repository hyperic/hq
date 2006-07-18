SET _JAR=%1
IF ""%1""=="""" goto gotAllArgs
shift

:argCheck
IF ""%1""=="""" goto gotAllArgs
SET _JAR=%_JAR% %1
shift
goto argCheck

:gotAllArgs
SET SHELL_CP=%_JAR%;%SHELL_CP%

