================================================================================                   
    Hyperic HQ Server, version ${project.version}, build ${build.number}
================================================================================ 
Starting and Stopping the Hyperic HQ Server on Windows:

On Windows, you have the choice of starting and starting the Hyperic HQ Server
from the Windows Service Manager or from a Windows Command Prompt. The Service
Manager is recommended for starting the Hyperic HQ Server because services remain
running even if you logout of the Windows user session that you're logged-in to
when starting the server.

----------------------------------------------------------------
To start the Hyperic HQ Server from the Windows Service Manager:

1. Start the services application from the Windows Administrative Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the Hyperic HQ Server service.

3. Click the start icon or click the start link.


----------------------------------------------------------------
To stop the Hyperic HQ Server from the Windows Service Manager:

1. Start the services application from the Windows Administrative Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the Hyperic HQ Server service.

3. Click the stop icon or click the stop link.


----------------------------------------------------------------
To start the Hyperic HQ Server from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 bin\hq-server.bat start

Note: Logging out of the Windows user session will shutdown the Hyperic HQ
      Server. You should start the Hyperic HQ Server from the Windows Service
      Manager, if want the Hyperic HQ Server to run after you logout of your
      Windows user session.
      

----------------------------------------------------------------
To stop the Hyperic HQ Server from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 bin\hq-server.bat stop


================================================================================


Starting and Stopping the Hyperic HQ Server on Linux / UNIX:

----------------------------------------------------------------
To start the Hyperic HQ server, execute this command:
 bin/hq-server.sh start


----------------------------------------------------------------
To stop the Hyperic HQ server, execute this command:
 bin/hq-server.sh stop

 
================================================================================


Reading the Hyperic HQ Server Log Files
---------------------------------------
The Hyperic HQ Server log files are located in the 'logs' sub-directory where the
Hyperic HQ Server is installed. The active log file is always named 'server.log'.
The log is a text file and can be opened and read with any text editor or the
Windows Notepad application.
