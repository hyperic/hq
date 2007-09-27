================================================================================                   

                 Hyperic HQ Agent, version @@@VERSION@@@

================================================================================


Starting and Stopping the Hyperic HQ Agent on Windows:

On Windows, you have the choice of starting and starting the Hyperic HQ Agent
from the Windows Service Manager or from a Windows Command Prompt. The Service
Manager is recommended for starting the Hyperic HQ Agent because services remain
running even if you logout of the Windows user session that you're logged-in to
when starting the server.

----------------------------------------------------------------
To start the Hyperic HQ Agent from the Windows Service Manager:

1. Start the services application from the Windows Administrive Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the Hyperic HQ Agent service.

3. Click the start icon or click the start link.


----------------------------------------------------------------
To stop the Hyperic HQ Agent from the Windows Service Manager:

1. Start the services application from the Windows Administrive Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the Hyperic HQ Agent service.

3. Click the stopt icon or click the stop link.


----------------------------------------------------------------
To start the Hyperic HQ Agent from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 bin\hq-agent.exe start

Note: Logging out of the Windows user session will shutdown the Hyperic HQ
      Agent. You should start the Hyperic HQ Agent from the Windows Service
      Manager, if want the Hyperic HQ Agent to run after you logout of your
      Windows user session.
      

----------------------------------------------------------------
To stop the Hyperic HQ Agent from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 hq-agent.exe stop


================================================================================


Starting and Stopping the Hyperic HQ Agent on Linux / UNIX:

----------------------------------------------------------------
To start the Hyperic HQ Agent, execute this command:

 hq-agent.sh start


----------------------------------------------------------------
To stop the Hyperic HQ Agent, execute this command:

 hq-agent.sh stop

 
================================================================================


Reading the Hyperic HQ Agent Log Files
---------------------------------------
The Hyperic HQ Agent log files are located in the 'log' sub-directory where the
Hyperic HQ Agent is installed. The active log file is always named 'agent.log'.
The log is a text file and can be openned and read with any text editor or the
Windows Notepad application.
