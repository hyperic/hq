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
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hyperic.util.jdbc.JDBC;

/**
 * DBInfo is a tool for printing information about a given JDBC driver and
 * database
 *
 * DBInfo can be run from the command line using the command:
 *  java org.hyperic.tools.db.DBPing
 * The DBInfo class and your JDBC driver should be in your classpath.
 *
 * DBInfo can also be called directly from another class. To do this
 * instantiate the DBInfo class and then call the DBPing.printInfo() method.
 */
public class DBInfo
{
    private static final String HELP = "DBInfo database [username] [password] [-driver <driver class>] [-?]\n" +
                                       "  database   The JDBC connect string (e.g., jdbc:cloudscape:test;create=true)\n" +
                                       "  -driver    The JDBC driver class for the database to connect to.\n" +
                                       "             (e.g., COM.cloudscape.core.JDBCDriver).\n" +
                                       "  -?         Displays help\n" +
                                       "  -help      Displays help\n";

    private static final String TRANSACTION_NONE             = "TRANSACTION_NONE";
    private static final String TRANSACTION_READ_COMMITTED   = "TRANSACTION_READ_COMMITTED";
    private static final String TRANSACTION_READ_UNCOMMITTED = "TRANSACTION_READ_UNCOMMITTED";
    private static final String TRANSACTION_REPEATABLE_READ  = "TRANSACTION_REPEATABLE_READ";
    private static final String TRANSACTION_SERIALIZABLE     = "TRANSACTION_SERIALIZABLE";

    private static final String NONE = "NONE";

    public static void main(String[] args)
    {
        int     iArgLen            = args.length;
        boolean bHelp              = false;
        String  strDriverClassName = null;

        /////////////////////////////////////////////////////////////
        // Look for the command line options

        for(int i = 0;i < iArgLen;i++)
        {
            if(args[i].equalsIgnoreCase("-driver") == true)
            {
                if((i + 1) < iArgLen)
                {
                    strDriverClassName = args[i + 1];
                    i++;
                }
                else
                {
                    System.out.println("Error: A JDBC driver class name must be specified after the -driver option.\n");
                    bHelp = true;
                    break;
                }
            }
            else if(args[i].equals("-?") == true || args[i].equalsIgnoreCase("-help") == true)
            {
                bHelp = true;
                break;
            }
            else if(args[i].charAt(0) == '-')
            {
                System.out.println("Error: Unknown option \'" + args[i] + '\'');
                bHelp = true;
                break;
            }
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

            DBInfo info = new DBInfo();

            try
            {
                info.printInfo(strDriverClassName, args[0], strUsername, strPassword);
            }
            catch(SQLException e)
            {
                JDBC.printSQLException(e);
            }
            catch(ClassNotFoundException e)
            {
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
            System.out.println(DBInfo.HELP);
    }


    /**
     * Loads the specified JDBC driver and prints informatation about the database.
     *
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     *
     * @return boolean
     *      true if the JDBC driver loaded and connected to the database, false
     *      otherwise.
     */
    public void printInfo(String driver, String database) throws ClassNotFoundException, SQLException
    {
        this.printInfo(driver, database, null, null);
    }

    /**
     * Loads the specified JDBC driver and prints information about the database database.
     *
     * @param driver
     *      The Java class name of the JDBC driver (e.g., "COM.cloudscape.core.JDBCDriver").
     * @param database
     *      The JDBC driver connect string (e.g., "jdbc:cloudscape:test")
     * @param username
     *      The username to use when connecting to the database. This value can
     *      be null.
     * @param password
     *      The password to use when connecting to the database. This value can
     *      be null.
     *
     * @return boolean
     *      true if the JDBC driver loaded and connected to the database, false
     *      otherwise.
     */
    public void printInfo(String driver, String database, String username, String password) throws ClassNotFoundException, SQLException
    {
        int         iCnt;
        Connection  conn    = null;
        PrintStream out     = System.out;
        boolean     bResult = true;
        String      strTmp;

        if(driver == null)
            driver = JDBC.getDriverString(database);
        database = JDBC.getConnectionString(driver, database);

        try
        {
            Driver dbdriver = (Driver)Class.forName(driver).newInstance();
            out.println("Successfully loaded the database driver.");

            conn = DriverManager.getConnection(database, username, password);
            out.println("Successfully connected to the database.\n");

            DatabaseMetaData data = conn.getMetaData();

            out.println("JDBC Driver             : " + data.getDriverName());
            out.println("Driver Version          : " + data.getDriverVersion());
            out.println("JDBC Compliant          : " + dbdriver.jdbcCompliant());
            out.println("URL                     : " + data.getURL());
            out.println();
            out.println("Database Product Name   : " + data.getDatabaseProductName());
            out.println("Database Product Version: " + data.getDatabaseProductVersion());
            out.println("Username                : " + data.getUserName());
            out.println();

            out.println("Operations");
            out.println("  Data Definition causes Commit      : " + data.dataDefinitionCausesTransactionCommit());
            out.println("  Data Definition is Ignored         : " + data.dataDefinitionIgnoredInTransactions());
            out.println("  Catalog Seperator                  : " + data.getCatalogSeparator());
            out.println("  Catalog Term                       : " + data.getCatalogTerm());
            out.println("  Default Transaction Isolation      : " + DBInfo.transactionIsolationToString(data.getDefaultTransactionIsolation()));
            out.println("  Identifier Quote String            : " + data.getIdentifierQuoteString());
            out.println("  Schema Term                        : " + data.getSchemaTerm());
            out.println("  Search String Escape Char          : " + data.getSearchStringEscape());
            out.println("  Null Plus Non-Nulls Null           : " + data.nullPlusNonNullIsNull());
            out.println("  Nulls Sorted at End                : " + data.nullsAreSortedAtEnd());
            out.println("  Nulls Sorted at Start              : " + data.nullsAreSortedAtStart());
            out.println("  Nulls Sorted High                  : " + data.nullsAreSortedHigh());
            out.println("  Nulls Sorted Low                   : " + data.nullsAreSortedLow());
            out.println("  Stores Lower-case Identifiers      : " + data.storesLowerCaseIdentifiers());
            out.println("  Stores Upper-case Identifiers      : " + data.storesUpperCaseIdentifiers());
            out.println("  Stores Mixed-case Identifiers      : " + data.storesMixedCaseIdentifiers());
            out.println("  Uses Local File Per Table          : " + data.usesLocalFilePerTable());
            out.println("  Uses Local Files                   : " + data.usesLocalFiles());
            out.println();

            out.println("Limits");
            out.println("  Max Binary Literal Length          : " + data.getMaxBinaryLiteralLength());
            out.println("  Max Catalog Name Length            : " + data.getMaxCatalogNameLength());
            out.println("  Max Char Literal Length            : " + data.getMaxCharLiteralLength());
            out.println("  Max Column Name Length             : " + data.getMaxColumnNameLength());
            out.println("  Max Columns in GROUPBY             : " + data.getMaxColumnsInGroupBy());
            out.println("  Max Columns in Index               : " + data.getMaxColumnsInIndex());
            out.println("  Max Columns in ORDERBY             : " + data.getMaxColumnsInOrderBy());
            out.println("  Max Columns in SELECT              : " + data.getMaxColumnsInSelect());
            out.println("  Max Columns in Table               : " + data.getMaxColumnsInTable());
            out.println("  Max Connections                    : " + data.getMaxConnections());
            out.println("  Max Index Length                   : " + data.getMaxIndexLength());
            out.println("  Max Procedure Name Length          : " + data.getMaxProcedureNameLength());
            out.println("  Max Row Size                       : " + data.getMaxRowSize());
            out.println("  Max Schema Name Length             : " + data.getMaxSchemaNameLength());
            out.println("  Max Statement Length               : " + data.getMaxStatementLength());
            out.println("  Max Statements Open                : " + data.getMaxStatements());
            out.println("  Max Table Name Length              : " + data.getMaxTableNameLength());
            out.println("  Max Tables in SELECT               : " + data.getMaxTablesInSelect());
            out.println("  Max Username Length                : " + data.getMaxUserNameLength());
            out.println();

            out.println("Supports");
            out.println("  Alter Table with Add Column        : " + data.supportsAlterTableWithAddColumn());
            out.println("  Alter Table with Drop Column       : " + data.supportsAlterTableWithDropColumn());
            out.println("  ANSI92 Entry Level SQL             : " + data.supportsANSI92EntryLevelSQL());
            out.println("  ANSI92 Full SQL                    : " + data.supportsANSI92FullSQL());
            out.println("  ANSI92 Intermediate SQL            : " + data.supportsANSI92IntermediateSQL());
            out.println("  Catalogs in Data Manipulation      : " + data.supportsCatalogsInDataManipulation());
            out.println("  Catalogs in Index Definitions      : " + data.supportsCatalogsInIndexDefinitions());
            out.println("  Catalogs in Privilege Def          : " + data.supportsCatalogsInPrivilegeDefinitions());
            out.println("  Catalogs in Procedures Calls       : " + data.supportsCatalogsInProcedureCalls());
            out.println("  Catalogs in Table Definitions      : " + data.supportsCatalogsInTableDefinitions());
            out.println("  Supports Column Aliasing           : " + data.supportsColumnAliasing());
            out.println("  Supports Convert                   : " + data.supportsConvert());
            out.println("  ODBC Core SQL Grammer              : " + data.supportsCoreSQLGrammar());
            out.println("  Correlated Subqueries              : " + data.supportsCorrelatedSubqueries());
            out.println("  Expressions in ORDERBY             : " + data.supportsExpressionsInOrderBy());
            out.println("  Extended SQL Grammer               : " + data.supportsExtendedSQLGrammar());
            out.println("  Full Outer Join                    : " + data.supportsFullOuterJoins());
            out.println("  GROUPBY                            : " + data.supportsGroupBy());
            out.println("  GROUPBY Beyond SELECT              : " + data.supportsGroupByBeyondSelect());
            out.println("  GROUPBY Unrelated                  : " + data.supportsGroupByUnrelated());
            out.println("  Integrity Enhancement Facility     : " + data.supportsIntegrityEnhancementFacility());
            out.println("  LIKE Escape Clauses                : " + data.supportsLikeEscapeClause());
            out.println("  Limited Outer Joins                : " + data.supportsLimitedOuterJoins());
            out.println("  ODBC Minimum SQL Grammer           : " + data.supportsMinimumSQLGrammar());
            out.println("  Mixed Case Identifiers             : " + data.supportsMixedCaseIdentifiers());
            out.println("  Mixed Case Quoted Identifiers      : " + data.supportsMixedCaseQuotedIdentifiers());
            out.println("  Mulitple Result Sets               : " + data.supportsMultipleResultSets());
            out.println("  Multiple Transactions              : " + data.supportsMultipleTransactions());
            out.println("  Non Nullable Columns               : " + data.supportsNonNullableColumns());
            out.println("  Open Cursors across Commit         : " + data.supportsOpenCursorsAcrossCommit());
            out.println("  Open Cursors across Rollback       : " + data.supportsOpenCursorsAcrossRollback());
            out.println("  ORDERBY Unrelated                  : " + data.supportsOrderByUnrelated());
            out.println("  Outer Joins                        : " + data.supportsOuterJoins());
            out.println("  Positioned DELETE                  : " + data.supportsPositionedDelete());
            out.println("  Positioned UPDATE                  : " + data.supportsPositionedUpdate());
            out.println("  Schemas in Data Manipulation       : " + data.supportsSchemasInDataManipulation());
            out.println("  Schemas in Index Definitions       : " + data.supportsSchemasInIndexDefinitions());
            out.println("  Schemas in Privilege Def           : " + data.supportsSchemasInPrivilegeDefinitions());
            out.println("  Schemas in Procedure Calls         : " + data.supportsSchemasInProcedureCalls());
            out.println("  Schemas in Table Definitions       : " + data.supportsSchemasInTableDefinitions());
            out.println("  SELECT for UPDATE                  : " + data.supportsSelectForUpdate());
            out.println("  Stored Procedures                  : " + data.supportsStoredProcedures());
            out.println("  Subqueries in Comparisons          : " + data.supportsSubqueriesInComparisons());
            out.println("  Subqueries in EXISTS               : " + data.supportsSubqueriesInExists());
            out.println("  Subqueries in IN                   : " + data.supportsSubqueriesInIns());
            out.println("  Subqueries in Quantifieds          : " + data.supportsSubqueriesInQuantifieds());
            out.println("  Table Correlation Names            : " + data.supportsTableCorrelationNames());
            out.println("  Transactions                       : " + data.supportsTransactions());
            out.println("  UNION                              : " + data.supportsUnion());
            out.println("  UNION ALL                          : " + data.supportsUnionAll());
            out.println("  Data Manipulation Transactions Only: " + data.supportsDataManipulationTransactionsOnly());
            out.println("  Different Table Correlation Names  : " + data.supportsDifferentTableCorrelationNames());
            out.println("  Data Definition and Data Manipulation Transactions: " + data.supportsDataDefinitionAndDataManipulationTransactions());
            out.println();

            out.println("Numeric Functions:");
            strTmp = data.getNumericFunctions();
            if(strTmp.length() == 0)
                strTmp = DBInfo.NONE;
            out.println(strTmp + '\n');

            out.println("SQL Keywords:");
            strTmp = data.getSQLKeywords();
            if(strTmp.length() == 0)
                strTmp = DBInfo.NONE;
            out.println(strTmp + '\n');

            out.println("System Function:");
            strTmp = data.getSystemFunctions();
            if(strTmp.length() == 0)
                strTmp = DBInfo.NONE;
            out.println(strTmp + '\n');

            out.println("Date/Time Function:");
            strTmp = data.getTimeDateFunctions();
            if(strTmp.length() == 0)
                strTmp = DBInfo.NONE;
            out.println(strTmp + '\n');

            out.println("Table Types:");
            ResultSet results = data.getTableTypes();
            for(iCnt = 0;results.next() == true;iCnt ++)
                out.print(results.getString(1) + ", ");
            if(iCnt == 0)
                out.println(DBInfo.NONE);
            out.println('\n');

            out.println("Column Types:");
            results = data.getTypeInfo();
            for(iCnt = 0;results.next() == true;iCnt ++)
               out.print(results.getString(1) + ", ");
            if(iCnt == 0)
                out.println(DBInfo.NONE);
            out.println('\n');
        }
        catch(SQLException e)
        {
            JDBC.printSQLException(e);
        }
        catch(InstantiationException e)
        {
            // This exception should not occur. The class loader has to be really screwed up.
            System.out.println("Error: " + e);
        }
        catch(IllegalAccessException e)
        {
            // This exception should not occur. The class loader has to be really screwed up.
            System.out.println("Error: " + e);
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

    private static String transactionIsolationToString(int transactionIsolation)
    {
        String  strResult;

        switch(transactionIsolation) {
        case Connection.TRANSACTION_READ_COMMITTED:
            strResult = DBInfo.TRANSACTION_READ_COMMITTED;
            break;
        case Connection.TRANSACTION_READ_UNCOMMITTED:
            strResult = DBInfo.TRANSACTION_READ_UNCOMMITTED;
            break;
        case Connection.TRANSACTION_REPEATABLE_READ:
            strResult = DBInfo.TRANSACTION_REPEATABLE_READ;
            break;
        case Connection.TRANSACTION_SERIALIZABLE:
            strResult = DBInfo.TRANSACTION_SERIALIZABLE;
            break;
        case Connection.TRANSACTION_NONE:
        default:
            strResult = DBInfo.TRANSACTION_NONE;
            break;
        }

        return strResult;
    }
}