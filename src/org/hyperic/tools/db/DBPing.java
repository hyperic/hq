/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.tools.db;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.lang.Math;
import java.util.Random;
import org.hyperic.util.jdbc.JDBC;

/**
 * DBPing is a tool for testing that a given JDBC driver can be loaded an a
 * connection to a database established. In addition, DBPing can run a
 * simple performance test which does the following:
 *  1. Creates a table
 *  2. Inserts records (1000 records by default; this can be overriden)
 *  3. Commits
 *  4. Deletes the records
 *  5. Commits
 *  6. Drops the table
 *
 * DBPing can be run from the command line using the command:
 *  java org.hyperic.tools.db.DBPing
 * Your JDBC driver should be in your classpath, as well as the DBPing class.
 *
 * DBPing can also be called directly from another class. To do this
 * instantiate the DBPing class and then call the DBPing.ping() method. The
 * DBPing.speedTest() method call also be used to run a performance test.
 */
public class DBPing
{
    private static final String COPYRIGHT = "\nDBPing, Copyright (C) 2002, Covalent Technologies, Inc., All Rights Reserved.\n";

    private static final String HELP = "DBPing database [username] [password] [-?] [-driver <driver class>][-op]\n" +
                                       "                [-quiet][-speed [records]]\n" +
                                       "  database   The JDBC connect string (e.g., jdbc:cloudscape:test;create=true)\n" +
                                       "  -driver    The JDBC driver class for the database to connect to.\n" +
                                       "             (e.g., COM.cloudscape.core.JDBCDriver).\n" +
                                       "  -op        Runs an operation test to check that the database account has\n" +
                                       "             privileges to create tables, create sequences, create triggers,\n" +
                                       "             insert data, update data and delete data\n" +
                                       "  -quiet     Doesn't display any messages\n" +
                                       "  -speed     Runs a performance test on the database.\n" +
                                       "             The test inserts and deletes database records. The default number\n" +
                                       "             of records is 1000. The default can be overridden by following\n" +
                                       "             this option with a space and the number of records to test with.\n" +
                                       "  -threads   Specifies the number of treads to use for a speed test.\n" +
                                       "  -?         Displays help\n" +
                                       "  -help      Displays help";

    private static final int    DEFAULT_NUMBER_SPEED_RECORDS = 1000;

    private boolean m_bQuiet;   // Defaults to false

    public static void main(String[] args)
    {
        /////////////////////////////////////////////////////////////
        // Look for the command line options

        int     iArgLen            = args.length;
        String  strDriverClassName = null;
        String  strError           = null;
        boolean bHelp              = false;
        boolean bOp                = false;
        boolean bQuiet             = false;
        boolean bResult            = false;
        boolean bSetNumRecs        = false;
        boolean bSetNumThreads     = false;
        boolean bSpeed             = false;
        int     iNumSpeedRecs      = DBPing.DEFAULT_NUMBER_SPEED_RECORDS;
        int     iThreads           = 0;

        for(int i = 0;i < iArgLen;i++)
        {
            if(args[i].equals("-?") == true || args[i].equalsIgnoreCase("-help"))
            {
                bHelp  = true;
                bQuiet = false; // Just in case the -quiet option was previously specified,
                                // we disable it to display help
                break;
            }
            else if(args[i].equalsIgnoreCase("-driver") == true)
            {
                if((i + 1) < iArgLen)
                {
                    strDriverClassName = args[i + 1];
                    i++;
                }
                else
                {
                    strError = "A JDBC driver class name must be specified after the -driver option.";
                    bHelp = true;
                    break;
                }
            }
            else if(args[i].equalsIgnoreCase("-op") == true)
            {
                bOp = true;
            }
            else if(args[i].equalsIgnoreCase("-quiet") == true)
            {
                bQuiet = true;
            }
            else if(args[i].equalsIgnoreCase("-speed") == true)
            {
                bSpeed = true;

                if((i + 1) < iArgLen && Character.isDigit(args[i + 1].charAt(0)))
                {
                    iNumSpeedRecs = Integer.parseInt(args[i + 1]);
                    bSetNumRecs   = true;
                    i++;
                }
            }
            else if(args[i].equalsIgnoreCase("-threads"))
            {
                iThreads = 2;

                if((i + 1) < iArgLen && Character.isDigit(args[i + 1].charAt(0)))
                {
                    iThreads       = Integer.parseInt(args[i + 1]);
                    bSetNumThreads = true;
                    i++;
                }
            }
            else if(args[i].charAt(0) == '-')
            {
                strError = "Unknown option \'" + args[i] + '\'';
                bHelp    = true;
                break;
            }
        }

        if(bQuiet == true)
            iArgLen --;

        if(bSpeed == true)
        {
            iArgLen --;

            if(bSetNumRecs == true)
                iArgLen --;
        }

        if(iThreads > 0)
        {
            iArgLen --;

            if(bSetNumThreads == true)
                iArgLen --;
        }

        /////////////////////////////////////////////////////////////
        // Display the program copyright notice

        if(bQuiet == false || bHelp == true)
        {
            System.out.println(DBPing.COPYRIGHT);

            if(strError != null)
                System.out.println("Error : " + strError + '\n');
        }

        /////////////////////////////////////////////////////////////
        // Run the ping with the command line arguments

        if(bHelp == false && iArgLen >= 1)
        {
            String  strUsername = null;
            String  strPassword = null;

            if(iArgLen >= 2)
                strUsername = args[1];

            if(iArgLen >= 3)
                strPassword = args[2];

            DBPing ping = new DBPing(bQuiet);
            
            try
            {
                ping.ping(strDriverClassName, args[0], strUsername, strPassword);

                // Runs an Op Test
                if(bOp == true)
                {
                    if(bQuiet == false)
                        System.out.println();
                    ping.operationTest(strDriverClassName, args[0], strUsername, strPassword);
                }
                
                // Runs a Speed Test
                if(bSpeed == true)
                {
                    if(bQuiet == false)
                        System.out.println();
                    ping.speedTest(strDriverClassName, args[0], strUsername, strPassword, iNumSpeedRecs, iThreads);
                }
            }
            catch(SQLException e)
            {
                if(bQuiet == false)
                    JDBC.printSQLException(e);
            }
            catch(Exception e)
            {
                if(bQuiet == false)
                    System.out.println("Error: " + e);
            }
        }
        else
        {
            bHelp = true;
        }

        //////////////////////////////////////////////////////////////////
        // Display help if appropriate. Normally either the -? option or
        // command line arguments.

        if(bHelp == true)
            System.out.println(DBPing.HELP);

        System.exit((bResult == true) ? 0 : -1);
    }

    /**
     * Constructs a DBPing class with quiet mode turned on.
     */
    public DBPing()
    {
    }

    /**
     * Constructs a DBPing class with quiet mode turned on or off.
     *
     * @param quiet
     *      Status messages are sent to std out if true, no message otherwise.
     */
    public DBPing(boolean quiet)
    {
        this.setQuiet(quiet);
    }

    /**
     * Runs an operation test by creating a table, creating an index, creating
     * a sequence, creating a trigger, inserting a row of data, updating a row
     * of data and deleting a row of data. All of the created objects are
     * dropped from the database before the method completes.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     */
    public void operationTest(String database) throws ClassNotFoundException, SQLException
    {
        this.operationTest(null, database, null, null);
    }

    /**
     * Runs an operation test by creating a table, creating an index, creating
     * a sequence, creating a trigger, inserting a row of data, updating a row
     * of data and deleting a row of data. All of the created objects are
     * dropped from the database before the method completes.
     *
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     *      This paramater can be null in which case the proper driver is loaded
     *      automatically, based on the specified database.
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     */
    public void operationTest(String driver, String database) throws ClassNotFoundException, SQLException
    {
        this.operationTest(driver, database, null, null);
    }
    
    /**
     * Runs an operation test by creating a table, creating an index, creating
     * a sequence, creating a trigger, inserting a row of data, updating a row
     * of data and deleting a row of data. All of the created objects are
     * dropped from the database before the method completes.
     *
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     *      This paramater can be null in which case the proper driver is loaded
     *      automatically, based on the specified database.
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     */
    public void operationTest(String driver, String database, String username, String password) throws ClassNotFoundException, SQLException
    {
        String      strCmd;
        Connection  conn         = null;
        boolean     bDropTable   = false;
        boolean     bDropSeq     = false;
        Statement   stmt         = null;
        String      strTableName = "DBPING_" + Math.abs((new Random()).nextLong());
        String      strSeqName   = strTableName + "_SEQ";
        String      strTest      = "";
        
        this.println("Operation Test:");
        
        try
        {
            if(driver == null)
                JDBC.loadDriver(database);

            conn = DriverManager.getConnection(database, username, password);
            conn.setAutoCommit(false);

            stmt = conn.createStatement();
            
            // CREATE A TABLE
            strCmd  = "CREATE TABLE " + strTableName + " (ID INT, NAME CHAR(5))";
            this.print(strTest = "Create Table Test...");
            stmt.executeUpdate(strCmd);
            this.printSuccess();
            bDropTable = true;
            
            // CREATE AN INDEX
            strCmd = "CREATE INDEX " + "DBPING_INDEX ON " + strTableName + "(ID)";
            this.print(strTest = "Create Index Test...");
            stmt.executeUpdate(strCmd);
            this.printSuccess();

            // PERFORM SEQUENCE & TRIGGER TEST ONLY FOR ORACLE
            int iDbType = JDBC.toType(database);
            if(iDbType == JDBC.ORACLE_TYPE || iDbType == JDBC.ORACLE_THIN_TYPE)
            {
                // CREATE A SEQUENCE
                strCmd = "CREATE SEQUENCE " + strSeqName;
                this.print(strTest = "Create Sequence Test...");
                stmt.executeUpdate(strCmd);
                bDropSeq = true;
                this.printSuccess();
            
                // SELECT FROM A SEQUENCE
                strCmd = "SELECT " + strSeqName + ".NEXTVAL FROM DUAL";
                this.print(strTest = "Query Sequence Test...");
                stmt.executeQuery(strCmd);
                this.printSuccess();
            
                // CREATE A TRIGGER
                strCmd = "CREATE TRIGGER " + strTableName + "_T " +
                          "BEFORE INSERT ON " + strTableName + " " +
                          "FOR EACH ROW " +
                          "BEGIN " +
                          "SELECT " + strSeqName + ".NEXTVAL FROM DUAL; " +
                          "END;";
                this.print(strTest = "Create Trigger Test...");
                stmt.executeUpdate(strCmd);
                this.printSuccess();
            
                // DROP THE TRIGGER (This is not one of the test. The trigger will interfere
                // with the insert and updates if we don't drop it now.
                strCmd = "DROP TRIGGER " + strTableName + "_T";
                stmt.executeUpdate(strCmd);
            }
            
            // INSERT A ROW OF DATA
            strCmd = "INSERT INTO " + strTableName + " VALUES(1, 'Test1')";
            this.print(strTest = "Insert Test...");
            stmt.executeUpdate(strCmd);
            this.printSuccess();
            
            // SELECT A ROW OF DATA
            strCmd = "SELECT ID FROM " + strTableName + " WHERE ID = 1";
            this.print(strTest = "Query Test...");
            stmt.executeQuery(strCmd);
            this.printSuccess();

            // UPDATE A ROW OF DATA
            strCmd = "UPDATE " + strTableName + " SET NAME = 'Test2' WHERE ID = 1";
            this.print(strTest = "Update Test...");
            stmt.executeUpdate(strCmd);
            this.printSuccess();
            
            // DELETE A ROW OF DATA
            strCmd = "DELETE FROM " + strTableName + " WHERE ID = 1";
            this.print(strTest = "Delete Test...");
            stmt.executeUpdate(strCmd);
            this.printSuccess();
        }
        catch(SQLException e)
        {
            // Re-throw the SQLException with the name of the test that was in
            // in progress when the exception occurred.
            strTest         = strTest.substring(0, strTest.indexOf('.'));
            SQLException se = new SQLException(strTest + ": " + e.getMessage(), e.getSQLState(), e.getErrorCode());
            
            SQLException ne;
            while((ne = e.getNextException()) != null)
                se.setNextException(ne);
                
            throw se;
        }
        finally
        {
            // clean-up the statement object
            try
            {
                if(bDropTable == true)
                    stmt.executeUpdate("DROP TABLE " + strTableName);

                if(bDropSeq == true)
                    stmt.executeUpdate("DROP SEQUENCE " + strTableName + "_SEQ");

                if(stmt != null)
                    stmt.close();
                    
                if(conn != null)
                    conn.close();
            }
            catch(SQLException e)
            {
            }
        }
    }

    /**
     * Loads the specified JDBC driver and connects to the specified database.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     */
    public void ping(String database) throws ClassNotFoundException, SQLException
    {
        this.ping(null, database, null, null);
    }

    /**
     * Loads the specified JDBC driver and connects to the specified database.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     */
    public void ping(String driver, String database) throws ClassNotFoundException, SQLException
    {
        this.ping(driver, database, null, null);
    }

    /**
     * Loads the specified JDBC driver and connects to the specified database.
     *
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     *      This paramater can be null in which case the proper driver is loaded
     *      automatically, based on the specified database.
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     */
    public void ping(String driver, String database, String username, String password) throws ClassNotFoundException, SQLException
    {
        Connection  conn    = null;
        boolean     bResult = true;

        this.println("Ping Test:");

        if(driver == null)
            driver = JDBC.getDriverString(database);
        database = JDBC.getConnectionString(driver, database);
        
        try
        {
            Class.forName(driver);
            this.println("Successfully loaded the database driver.");

            conn = DriverManager.getConnection(database, username, password);
            this.println("Successfully connected to the database.");
        }
        finally
        {
            // Close the Database Connection
            if(conn != null)
            {
                try
                {
                    conn.close();
                }
                catch(SQLException e)
                {
                }
            }
        }
    }
    
    /**
     * Runs a speed test by creating a table, inserting a specified number of
     * rows into a table, committing, deleting the rows committing and then
     * dropping the table. The created table starts with the phrase DBPING_ and
     * ends with a random number.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     * @param numrecs
     *      The number of records to create and delete.
     *
     * @return long
     *      The elapsed time in milliseconds that the speed test required.
     */
    public long speedTest(String driver, String database, String username, String password) throws ClassNotFoundException, SQLException
    {
        return this.speedTest(driver, database, username, password, DBPing.DEFAULT_NUMBER_SPEED_RECORDS);
    }

    /**
     * Runs a speed test by creating a table, inserting a specified number of
     * rows into a table, committing, deleting the rows committing and then
     * dropping the table. The created table starts with the phrase DBPING_ and
     * ends with a random number.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     * @param numrecs
     *      The number of records to create and delete.
     *
     * @return long
     *      The elapsed time in milliseconds that the speed test required.
     */
    public long speedTest(String driver, String database, String username, String password, int numrecs) throws ClassNotFoundException, SQLException
    {
        String     strCmd;
        Connection conn              = null;
        boolean    bDropTable        = false;
        Statement  stmt              = null;
        String     strTableName      = "DBPING_" + Math.abs((new Random()).nextLong());
        long       lTotalElapsedTime = 0;

        this.println("Speed Test:");
        
        try
        {
            if(driver == null)
                JDBC.loadDriver(database);

            conn = DriverManager.getConnection(database, username, password);
            conn.setAutoCommit(false);

            /////////////////////////////////////////////////////////
            // CREATE A TABLE AND INSERT ROWS

            stmt = conn.createStatement();

            long lInsertStart = System.currentTimeMillis();

            strCmd = "CREATE TABLE " + strTableName + " (ID INT PRIMARY KEY, NAME CHAR(30), ADDRESS1 CHAR(30), CITY CHAR(30), STATE CHAR(2), ZIP CHAR(5))";
            stmt.executeUpdate(strCmd);
            conn.commit();
            bDropTable = true;

            //strCmd = "CREATE UNIQUE INDEX " + "DBPING_INDEX ON " + strTableName + "(ID)";
            //stmt.executeUpdate(strCmd);
            //conn.commit();

            this.println("Created test table: " + strTableName);
            this.print("Running a database INSERT performance test...Inserting " + numrecs +  " records...");

            //char ch = 0;

            for(int i = 1;i <= numrecs;i++)
            {
                strCmd = Integer.toString(i);
                this.print(strCmd);
                for(int c = 0;c < strCmd.length();c++)
                    this.print('\b');

                stmt.executeUpdate("INSERT INTO " + strTableName + " VALUES(" + i + ", 'Mark Douglas', '645 Howard Street', 'San Francisco', 'CA', '94105')");
            }

            conn.commit();

            for(int c = 0;c < strCmd.length();c++)
                this.print(' ');
            this.println();

            long lInsertEnd     = System.currentTimeMillis();
            long lInsertElapsed = (lInsertEnd - lInsertStart) / 1000;


            /////////////////////////////////////////////////////////
            // DELETE THE ROWS AND DROP THE TABLE

            this.print("Running a database DELETE performance test...Deleting " + numrecs + " records...");

            long lDeleteStart = System.currentTimeMillis();

            for(int i = 1;i <= numrecs;i++)
            {
                strCmd = Integer.toString(i);
                this.print(strCmd);
                for(int c = 0;c < strCmd.length();c++)
                    this.print('\b');

                stmt.executeUpdate("DELETE FROM " + strTableName + " WHERE ID = " + i);
            }

            conn.commit();

            for(int c = 0;c < strCmd.length();c++)
                this.print(' ');
            this.println();

            stmt.executeUpdate("DROP TABLE " + strTableName);
            conn.commit();
            this.println("Dropped test table: " + strTableName);
            bDropTable = false; // This variable makes sure that the table doesn't get a second drop
                                // attempt in the finally block.

            long lDeleteEnd     = System.currentTimeMillis();
            long lDeleteElapsed = (lDeleteEnd - lDeleteStart) / 1000;

            lTotalElapsedTime = lInsertElapsed + lDeleteElapsed;

            // Print the insert time
            this.println();

            this.print("Insert Performance Time: ");
            this.printTime(lInsertElapsed);
            this.println();

            this.print("Delete Performance Time: ");
            this.printTime(lDeleteElapsed);
            this.println();

            this.print("Total Performance Time : ");
            this.printTime(lTotalElapsedTime);
            this.println();
        }
        finally
        {
            // Drop the table we created and clean-up the statement object
            if(stmt != null)
            {
                try
                {
                    if(bDropTable == true)
                        stmt.executeUpdate("DROP TABLE " + strTableName);

                    stmt.close();
                    conn.close();
                }
                catch(SQLException e)
                {
                }
            }
        }

        return lTotalElapsedTime;
    }


    /**
     * Runs a multi-threaded speed test by creating a table, inserting a
     * specified number of rows into a table, committing, deleting the rows
     * committing and then dropping the table. The created table starts with
     * the phrase DBPING_ and ends with a random number.
     *
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     * @param numrecs
     *      The number of records to create and delete.
     * @param threads
     *      The number of threads to use in the speed test.
     *
     * @return long
     *      The elapsed time in milliseconds that the speed test required.
     */
    public long speedTest(String driver, String database, String username, String password, int numrecs, int threads) throws ClassNotFoundException, SQLException
    {
        long    lElapsed = 0;

        // If threads are 0 run the test on the current thread
        if(threads == 0)
        {
            return this.speedTest(driver, database, username, password, numrecs);
        }
        else
        {
            String     strCmd;
            Connection conn         = null;
            boolean    bDropTable   = false;
            boolean    bResult      = false;
            Statement  stmt         = null;
            String     strTableName = "DBPING_" + Math.abs((new Random()).nextLong());

            this.println("Speed Test:");

            long lStart = System.currentTimeMillis();

            try
            {
                if(driver == null)
                    JDBC.loadDriver(database);

                conn     = DriverManager.getConnection(database, username, password);
                conn.setAutoCommit(false);

                stmt = conn.createStatement();

                /////////////////////////////////////////////////////////
                // Create the Test Table

                strCmd = "CREATE TABLE " + strTableName + " (ID INT PRIMARY KEY, NAME CHAR(30), ADDRESS1 CHAR(30), CITY CHAR(30), STATE CHAR(2), ZIP CHAR(5))";
                stmt.executeUpdate(strCmd);
                conn.commit();
                bDropTable = true;

                //strCmd = "CREATE UNIQUE INDEX " + "DBPING_INDEX ON " + strTableName + "(ID)";
                //stmt.executeUpdate(strCmd);
                //conn.commit();

                this.println("Created test table: " + strTableName);
                this.println("Running a database INSERT and DELETE performance test on " + numrecs +  " records...");


                ////////////////////////////////////////////////////////////
                // Start the test threads

                int iRecsPerThread = numrecs / threads;
                //int iR = numrecs % threads;

                SpeedTestThread[] aThreads = new SpeedTestThread[threads];

                for(int i = 0;i < threads;i++)
                {
                    SpeedTestThread thread = new SpeedTestThread(database, username, password, strTableName, (iRecsPerThread * i) + 1, (iRecsPerThread) * (i + 1));
                    thread.setDaemon(true);
                    thread.start();

                    aThreads[i] = thread;
                }

                // Wait for the threads to die starting with the most recent
                for(int i = threads - 1;i >= 0;i--)
                {
                    SpeedTestThread thread = aThreads[i];

                    if(thread.isAlive() == true)
                    {
                        try
                        {
                            thread.join();
                        }
                        catch(InterruptedException e)
                        {
                            this.println("Error: " + e);
                        }
                    }

                    bResult = thread.getResult();

                    if(bResult == false)
                        break;
                }

                ///////////////////////////////////////////////////////////
                // Drop the Test Table

                stmt.executeUpdate("DROP TABLE " + strTableName);
                conn.commit();
                this.println("Dropped test table: " + strTableName);
                bDropTable = false; // This variable makes sure that the table doesn't get a second drop
                                    // attempt in the finally block.

                if(bResult == true)
                {
                    lElapsed = (System.currentTimeMillis() - lStart) / 1000;

                    this.print("\nTotal Performance Time : ");
                    this.printTime(lElapsed);
                    this.println();
                }
            }
            finally
            {
                // Drop the table we created and clean-up the statement object
                if(stmt != null)
                {
                    try
                    {
                        if(bDropTable == true)
                            stmt.executeUpdate("DROP TABLE " + strTableName);

                        stmt.close();
                        conn.close();
                    }
                    catch(SQLException e)
                    {
                    }
                }
            }
        }

        return lElapsed;
    }

    /**
     * Retrieves whether status messages will be printed to System.out.
     *
     * @return boolean
     *      true if status messages will be printed, false otherwise.
     */
    public boolean isQuiet()
    {
        return this.m_bQuiet;
    }

    /**
     * Sets whether status messages will be printed to System.out.
     *
     * @param quiet
     *      true if status messages should be printed, false otherwise.
     */
    public void setQuiet(boolean quiet)
    {
        this.m_bQuiet = quiet;
    }

    protected void print(char c)
    {
        if(this.isQuiet() == false)
            System.out.print(c);
    }
        
    protected void print(String s)
    {
        if(this.isQuiet() == false)
            System.out.print(s);
    }
    
    protected void println()
    {
        if(this.isQuiet() == false)
            System.out.println();
    }

    protected void println(String s)
    {
        if(this.isQuiet() == false)
            System.out.println(s);
    }

    protected void printSuccess()
    {
        if(this.isQuiet() == false)
            System.out.println("Success");
    }
    
    protected void printTime(long time)
    {
        long    lHours;
        long    lMinutes;
        long    lSeconds;

        lMinutes  = time / 60;
        lHours    = lMinutes / 60;
        lMinutes -= (lHours * 60);  // Subtract the hours. We only need the remaining minutes.
        lSeconds  = time % 60;

        if(lHours > 0)
        {
            this.print(lHours + " hour");

            if(lHours > 1)
                this.print('s');

            this.print(' ');
        }

        if(lMinutes > 0)
            this.print(lMinutes + " minutes ");

        this.print(lSeconds + " seconds");
    }
}
