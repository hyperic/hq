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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hyperic.util.StrongCollection;
import org.hyperic.util.jdbc.JDBC;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * To Do:
 * - implement abstract factory for tables,columns,views,etc - the "if dbtype"
 * stuff is yucky
 * - The verbose/quiet System.out'ing stuff should go away in favor of
 * commons-logging
 * - hungarian notation must be destroyed
 * - schema diff'ing (semantic diff, not xml file diff or something)
 * - fix how the driver classes are loaded and the relationship to
 * org.hyperic.util.jdbc.JDBC (do you load drivers with just database name
 * or by deriving the drivername from the database name and then loading it,
 * both seem to happening now, which is bad)
 * - fix up terminology ("create" should be "rdbms-export", "setup" should be
 * "rdbms-import" or something else) to more descriptive
 */
public class DBSetup {
    private static final String COPYRIGHT = "DBSetup, Copyright (C) 2005, Hyperic, Inc. All Rights Reserved.";

    private boolean          m_bQuiet;   // Defaults to false
    private boolean          m_bNoninteractive;   // Defaults to false
    private boolean          m_bVerbose; // Defaults to false
    private String           m_logFileName = null;
    private boolean          m_logAppend = false;
    private FileWriter       m_log = null;
    public static boolean    m_bDMLonly = false;
    private String           m_typeMapFile = null;
    private InputStream      m_typeMapStream = null;
    
    private int     m_lCreatedIndexes;
    private int     m_lCreatedTables;
    private int     m_lCreatedViews;
    private int     m_lFailedIndexes;
    private int     m_lFailedTables;
    private int     m_lFailedViews;

    private Connection m_conn = null;

    /**
     * Constructs a DBSetup class with quiet mode turned on.
     */
    public DBSetup()
    {
    }

    /**
     * Constructs a DBSetup object with quiet mode turned on or off.
     *
     * @param quiet
     *      Status messages are sent to std out if true, no message otherwise.
     */
    public DBSetup(boolean quiet)
    {
        this.setQuiet(quiet);
    }

    /**
     * Constructs a DBSetup object with quiet mode turned on or off.
     *
     * @param quiet
     *      Status messages are sent to stdout if true, no messages otherwise.
     * @param verbose
     *      SQL Commands and other verbose information is sent to stdout if
     *      true, no verbose messages otherwise.
     */
    public DBSetup(boolean quiet, boolean verbose)
    {
        if(quiet == true && verbose == true)
            verbose = false;

        this.setQuiet(quiet);
        this.setVerbose(verbose);
    }

    public DBSetup(boolean quiet, boolean verbose, boolean nonInteractive,
                   String logFileName, boolean append, boolean noexec) {
        this(quiet, verbose);
        m_bNoninteractive = nonInteractive;
        m_logFileName = logFileName;
        m_logAppend = append;
    }

    public void create(String file, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        try
        {
            ////////////////////////////////////////////////////////
            // Make sure we can connect to the database
            m_conn = connect(database, username, password);

            ////////////////////////////////////////////////////////
            // Create the DBSetup XML file

            Document doc      = this.newDocument();
            Element  elemRoot = doc.createElement("Covalent.DBSetup");

            elemRoot.setAttribute("name", file);
            elemRoot.setAttribute("notice", DBSetup.COPYRIGHT);
            doc.appendChild(elemRoot);

            Iterator iterTabs = Table.getTables(this, username).iterator();

            ///////////////////////////////////////////////////////
            // Find Tables

            while(iterTabs.hasNext() == true)
            {
                Table tab = (Table)iterTabs.next();
                
                if(this.isQuiet() == false)
                    System.out.println("\nFound Table : " + tab.getName());

                Element elemTab = doc.createElement("table");
                elemTab.setAttribute("name", tab.getName());
                elemRoot.appendChild(elemTab);

                ///////////////////////////////////////////////////
                // Get Columns

                Iterator iterCols = tab.getColumns().iterator();

                while(iterCols.hasNext() == true)
                {
                    Column col = (Column)iterCols.next();

                    if(this.isQuiet() == false)
                        System.out.println("Found Column: " + col.getName());

                    Element elemChild = doc.createElement("column");
                    elemChild.setAttribute("name", col.getName());
                    elemChild.setAttribute("type", col.getType());
                    elemChild.setAttribute("size", String.valueOf(col.getSize()));

                    if(col.isRequired() == true)
                        elemChild.setAttribute("required", String.valueOf(col.isRequired()));

                    elemTab.appendChild(elemChild);
                }

                ///////////////////////////////////////////////////
                // Get Data

                DataSet dataset = tab.getDataSet();

                while(dataset.next() == true)
                {
                    Element elemChild = doc.createElement("data");

                    int iCols = tab.getColumns().size();

                    for(int i = 0;i < iCols;i++)
                    {
                        Data data = dataset.getData(i);
                        elemChild.setAttribute(data.getColumnName(), data.getValue());
                    }

                    elemTab.appendChild(elemChild);
                }

                this.m_lCreatedTables ++;
            }

            boolean bWrite = true;

            if(this.isQuiet() == false)
            {
                File f = new File(file);

                if(f.exists() == true)
                {
                    System.out.print("\nThe file " + file + " already exist. Do you want to overwrite this file? (Y/N): ");
                    char ch = (char)System.in.read();

                    if(Character.toUpperCase(ch) != 'Y')
                    {
                        System.out.println("Aborted.");;
                        bWrite = false;
                    }
                }
            }

            if(bWrite == true)
                this.writeDocument(doc, file);

            //////////////////////////////////////////////////////////////
            // Print Results

            if(this.isQuiet() == false)
            {
                DecimalFormat fmt = new DecimalFormat("#,###");

                System.out.println();
                System.out.println(fmt.format(this.m_lCreatedViews)   +
                                   " views validated.");
                System.out.println(fmt.format(this.m_lFailedViews)    +
                                   " views failed to validate.");
                System.out.println(fmt.format(this.m_lCreatedTables)  +
                                   " tables validated.");
                System.out.println(fmt.format(this.m_lFailedTables)   +
                                   " tables failed to validate.");
                System.out.println(fmt.format(this.m_lCreatedIndexes) +
                                   " indexes validated.");
                System.out.println(fmt.format(this.m_lFailedIndexes)  +
                                   " indexes failed to validate.");
            }
        } catch(SAXException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if(m_conn != null) m_conn.close();
            } catch(SQLException e) {}
        }
    }

    public Document newDocument() throws SAXException
    {
        Document docResult = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler());

            docResult = builder.newDocument();
        }
        catch(ParserConfigurationException e)
        {
            throw new SAXException(e);
        }

        return docResult;
    }

    protected static Node copyNode(Document target, Node source, boolean deep)
    {
        Node     nodeNew;

        String strNodeName  = source.getNodeName();
        String strNodeValue = source.getNodeValue();

        switch(source.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element elem = target.createElement(strNodeName);
            nodeNew      = elem;

            NamedNodeMap map = source.getAttributes();

            for(int iMap = 0;iMap < map.getLength();iMap ++)
            {
                Node attr = map.item(iMap);
                elem.setAttribute(attr.getNodeName(), attr.getNodeValue());
            }

            break;
        case Node.COMMENT_NODE:
            nodeNew = target.createComment(strNodeName);
            break;
        case Node.TEXT_NODE:
            nodeNew = target.createTextNode(strNodeName);
            break;
        default:
            nodeNew = null;
            break;
        }

        nodeNew.setNodeValue(strNodeValue);

        if(deep == true && nodeNew != null)
            DBSetup.importChildNodes(nodeNew, source, deep);

        return nodeNew;
    }

    protected static void importNodeAfter(Node after, Node source, boolean deep)
    {
        Node nodeNew = DBSetup.copyNode(after.getOwnerDocument(), source, deep);

        //////////////////////////////////////////////////
        // Append the node to the target

        Node nodeNext = after.getNextSibling();

        if(nodeNext != null)
            after.getParentNode().insertBefore(nodeNew, after);
        else
            after.getParentNode().appendChild(nodeNew);

        // Get Children
        //if(deep == true)
        //    DBSetup.importChildNodes(nodeNew, source, deep);
    }

    protected static void importChildNodes(Node parent, Node source, boolean deep)
    {
        NodeList listChildren = source.getChildNodes();

        for(int i = 0;i < listChildren.getLength();i ++)
            parent.appendChild(DBSetup.copyNode(parent.getOwnerDocument(), listChildren.item(i), deep));
    }

    public Document readDocument(String file) throws IOException, SAXException
    {
        Document docResult = this.readDocument(file, null);
        return docResult;
    }

    public Document readDocument(InputStream is) throws IOException, SAXException
    {
        Document docResult = this.readDocument(is, null);
        return docResult;
    }

    public Document readDocument(Object source, Node after) throws IOException, SAXException
    {
        Document docResult = null;

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler());

            File f = null;
            if (source instanceof String) {
                f        = new File((String) source);
                docResult     = builder.parse(f);
            } else if (source instanceof InputStream) {
                docResult = builder.parse((InputStream) source);
            }
            Node nodeRoot = docResult.getFirstChild();

            ////////////////////////////////////////////////////////
            // Make sure we have a DBSetup XML file.

            // This is dumb, because there could be an XML comment before
            // the root element.  The root node is not the same thing as the
            // root element.
            if(nodeRoot.getNodeName().equalsIgnoreCase("Covalent.DBSetup") == false) {
                if (source instanceof String) {
                    throw new IOException(source.toString() + " is not a "
                                          + "DBSetup XML file.");
                } else {
                    throw new IOException("Source is not a "
                                          + "DBSetup XML file.");
                }
            }

            /////////////////////////////////////////////////////////
            // Look for include tags

            NodeList listNodes = nodeRoot.getChildNodes();

            for(int iNode = 0;iNode < listNodes.getLength();iNode ++)
            {
                Node node = listNodes.item(iNode);

                if(node.getNodeName().equalsIgnoreCase("include") == true)
                {
                    NamedNodeMap map = node.getAttributes();

                    for(int iAttr = 0;iAttr < map.getLength();iAttr++)
                    {
                        Node nodeMap = map.item(iAttr);

                        if(nodeMap.getNodeName().equalsIgnoreCase("file") == true)
                        {
                            File fileInclude = new File(nodeMap.getNodeValue());

                            if(fileInclude.isAbsolute() == false)
                            {
                                if (!(source instanceof String)) {
                                    throw new IOException("Cannot resolve paths"
                                                          + " relative to a "
                                                          + "stream.");
                                }
                                String strPath = f.getAbsolutePath();
                                int iIndex     = strPath.lastIndexOf(File.separatorChar);
                                strPath        = strPath.substring(0, iIndex);
                                fileInclude    = new File(strPath + File.separatorChar + fileInclude.getPath());
                            }

                            this.readDocument(fileInclude.getAbsolutePath(), node);
                            nodeRoot.removeChild(node);
                        }
                    }
                }
                else if(after != null)
                    DBSetup.importNodeAfter(after, node, true);
            }
        }
        catch(ParserConfigurationException e)
        {
            throw new SAXException(e);
        }

        return docResult;
    }

    public void writeDocument(Document doc, String file) throws IOException, SAXException
    {
        try
        {

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer        trans   = factory.newTransformer();

            trans.setOutputProperty("indent", "yes");
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource    src    = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(file));

            trans.transform(src, result);
        }
        catch(TransformerException e)
        {
            throw new SAXException(e);
        }
    }

    public boolean setup(String file, String database) throws ClassNotFoundException, IOException, SQLException
    {
        return this.doSetup(file, database, null, null);
    }

    public boolean setup(String file, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doSetup(file, database, username, password);
    }

    public boolean setup(String file, String database, String username, String password, 
                         String table, boolean doDelete)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doSetup(file, database, username, password, table, doDelete);
    }

    public boolean setup(InputStream is, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doSetup(is, database, username, password);
    }
    
    protected boolean doSetup(Object source, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doSetup(source, database, username, password, null, false);
    }

    protected boolean doSetup(Object source, String database, String username, String password,
                              String table, boolean doDelete)
        throws ClassNotFoundException, IOException, SQLException
    {
        try
        {
            Document doc = null;
            if (source instanceof String) {
                doc = this.readDocument((String) source);
            } else if (source instanceof InputStream) {
                doc = this.readDocument((InputStream) source);
            }

            ////////////////////////////////////////////////////////
            // Make sure we can connect to the database

            JDBC.loadDriver(database);
            if(this.isQuiet() == false)
                System.out.println("Successfully loaded the database driver.");

            m_conn = connect(database, username, password);
            if(this.isQuiet() == false)
                System.out.println("Successfully connected to the database.");

            Collection collTypeMaps = null;
            if ( m_typeMapFile != null ) {
                collTypeMaps = TypeMap.readTypeMaps(readDocument(m_typeMapFile).getFirstChild());
            } else if ( m_typeMapStream != null ) {
                collTypeMaps = TypeMap.readTypeMaps(readDocument(m_typeMapStream).getFirstChild());
            } else {
                collTypeMaps = TypeMap.readTypeMaps(doc.getFirstChild());
            }

/**
            // check to see if we're dealing with a view or a table
            Node dbSetupNode = doc.getFirstChild();
            // actually, its the first child of the main node in the schema

            NodeList nodeList = dbSetupNode.getChildNodes();
            for(int i=0;i < nodeList.getLength(); i++) {
                Node rootNode = nodeList.item(i);
                if(rootNode.getNodeName().equalsIgnoreCase("table")) {
**/
                    Node rootNode = doc.getFirstChild();
                    Iterator   iterTab      = Table.getTables(rootNode,
                        JDBC.toType(database), this).iterator();

                    ///////////////////////////////////////////////////////////////////
                    // Open the log file if we are supposed to log SQL statements.
                    if ( m_logFileName != null ) {
                        m_log = new FileWriter(m_logFileName, m_logAppend);
                    }

                    while(iterTab.hasNext() == true)
                    {
                        Table tab = (Table)iterTab.next();

                        if (table != null) {
                            if (!table.equals(tab.getName())) {
                                continue;
                            }
                            if (doDelete) tab.clear();
                        }

                        if(this.isVerbose() == true)
                            System.out.println("Processing Table: " + tab.getName());

                        if(m_bDMLonly == false)
                        {
                            //////////////////////////////////////////////////////////////
                            // Create the Table
                            try
                            {
                                // Only attempt to create the table if the table tag has columns

                                if(tab.getColumns().size() > 0)
                                {
                                    tab.create(collTypeMaps);

                                    if(this.isQuiet() == false)
                                        System.out.println("Created Table: " + tab.getName());

                                    this.m_lCreatedTables ++;
                                }
                            }
                            catch(SQLException e)
                            {
                                if(this.isQuiet() == false)
                                {
                                    System.out.println("Error: " +
                                        "Cannot create table " + tab.getName());
                                    JDBC.printSQLException(e);
                                }

                                this.m_lFailedTables ++;
                            }

                            ///////////////////////////////////////////////////////////////
                            // Create the Indexes

                            Iterator iterCol = tab.getIndexes().iterator();

                            while(iterCol.hasNext() == true)
                            {
                                Index idx = (Index)iterCol.next();

                                try
                                {
                                    idx.create();

                                    if(this.isQuiet() == false)
                                        System.out.println("Created Index: " +
                                            idx.getName() + " for Table \'" +
                                            idx.getTable().getName() + '\'');

                                        this.m_lCreatedIndexes ++;
                                }
                                catch(SQLException e)
                                {
                                    if(this.isQuiet() == false)
                                    {
                                        System.out.println("Error: Cannot create index " + idx.getName());
                                        JDBC.printSQLException(e);
                                    }

                                    this.m_lFailedIndexes ++;
                                }
                            }
                        }

                        //////////////////////////////////////////////////////////////
                        // Create the Data

                        DataSet dataset = tab.getDataSet();

                        try
                        {
                            int iRowCnt = dataset.create();

                            if(this.isQuiet() == false)
                                System.out.println("Created " + iRowCnt + " Row(s).");
                        }
                        catch(SQLException e)
                        {
                            if(this.isQuiet() == false)
                            {
                                System.out.println("Error: " + "Cannot create Row.");
                                JDBC.printSQLException(e);

                                if(this.isVerbose() == true)
                                    e.printStackTrace();
                            }
                        }
                    }
                // }

                // if(rootNode.getNodeName().equalsIgnoreCase("view")) {
                    // process views
                    Iterator   iterView      = View.getViews(rootNode,
                        JDBC.toType(database), this).iterator();
                    ////////////////////////////////////////////////////
    ///////////////
                    // Open the log file if we are supposed to log SQL statements.
                    if ( m_logFileName != null ) {
                        m_log = new FileWriter(m_logFileName, m_logAppend);
                    }

                    while(iterView.hasNext() == true)
                    {
                        View view = (View)iterView.next();

                        if(this.isVerbose() == true)
                            System.out.println("Processing view: " + view.getName());

                        if(m_bDMLonly == false)
                        {
                            ////////////////////////////////////////////
    //////////////////
                            // Create the View
                            try
                            {
                                view.create(collTypeMaps);

                                if(this.isQuiet() == false)
                                    System.out.println("Created View: " +
                                        view.getName());

                                this.m_lCreatedViews ++;
                            }
                            catch (SQLException e) {
                                if(this.isQuiet() == false)
                                {
                                    System.out.println("Error: " + "Cannot create View.");
                                    JDBC.printSQLException(e);

                                    if(this.isVerbose() == true)
                                        e.printStackTrace();
                                }
                            }
                        }
                    }
                // }
            // }

            //////////////////////////////////////////////////////////////
            // Print Results

            if(this.isQuiet() == false)
            {
                DecimalFormat fmt = new DecimalFormat("#,###");

                System.out.println();
                System.out.println(fmt.format(this.m_lCreatedTables) + " tables created succesfully.");
                System.out.println(fmt.format(this.m_lCreatedViews) + " views created succesfully.");
                System.out.println(fmt.format(this.m_lCreatedIndexes) + " indexes created successfully.");
                System.out.println(fmt.format(this.m_lFailedTables)  + " tables failed to create.");
                System.out.println(fmt.format(this.m_lFailedIndexes) + " indexes failed to create.");
            }
        } catch(SAXException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        } finally {
            if ( m_log != null ) {
                try { m_log.close(); } catch ( Exception e ) {}
            }

            try {
                if(m_conn != null) m_conn.close();
            } catch(SQLException e) {}
        }

        return true;
    }

    public boolean clear(String file, String database, String username,
                         String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.clear(file, database, username, password, null);
    }
    
    public boolean clear(String file, String database, String username,
                         String password, String table)
        throws ClassNotFoundException, IOException, SQLException
    {
        boolean     bProceed = false;
        Statement   stmt     = null;

        try
        {
            Document doc = this.readDocument(file);

            ////////////////////////////////////////////////////////
            // Make sure we can connect to the database
            m_conn = connect(database, username, password);
            stmt = m_conn.createStatement();

            ////////////////////////////////////////////////////////
            // Verify that the user wants to do this.
            if(this.isQuiet() == false && (!this.isNoninteractive()) )
            {
                System.out.print("DBSetup will permanently delete data. " +
                                 "DO YOU WANT TO PROCEED? (Y/N): ");

                if(Character.toUpperCase((char)System.in.read()) == 'Y')
                {
                    System.out.println();
                    bProceed = true;
                }
                else
                    System.out.println("Aborted.");
            }
            else bProceed = true;   // If they go quiet, delete without asking.

            if(bProceed == true)
            {
                // remove data from tables in reverse order of their creation
                // to bypass dependency constraints
                StrongCollection tables  = (StrongCollection)
                    Table.getTables(doc.getFirstChild(),
                                    JDBC.toType(database),
                                    this);
                tables.reverse();

                Iterator iterTab = tables.iterator();

                while(iterTab.hasNext() == true)
                {
                    Table tab = (Table)iterTab.next();

                    if(table != null &&
                      tab.getName().compareToIgnoreCase(table) != 0)
                        continue;

                    try
                    {
                        tab.clear();

                        if(this.isQuiet() == false)
                            System.out.println("Clear Table: " + tab.getName());

                        this.m_lCreatedTables ++;
                    }
                    catch(SQLException e)
                    {
                        if(this.isQuiet() == false)
                        {
                            System.out.println("Error: Cannot clear table " +
                                               tab.getName());
                            JDBC.printSQLException(e);
                        }

                        this.m_lFailedTables ++;
                    }
                }
            }

            //////////////////////////////////////////////////////////////
            // Print Results

            if(this.isQuiet() == false)
            {
                DecimalFormat fmt = new DecimalFormat("#,###");

                System.out.println();
                System.out.println(fmt.format(this.m_lCreatedTables) +
                                   " tables cleared succesfully.");
                System.out.println(fmt.format(this.m_lFailedTables)  +
                                   " tables failed to clear.");
            }
        } catch(SAXException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if(stmt != null) stmt.close();
                if(m_conn != null) m_conn.close();
            } catch(SQLException e) {}
        }

        return true;
    }

    public boolean uninstall(String file, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doUninstall(file, database, username, password);
    }
    
    public boolean uninstall(InputStream is, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        return this.doUninstall(is, database, username, password);
    }

    protected boolean doUninstall(Object source, String database, String username, String password)
        throws ClassNotFoundException, IOException, SQLException
    {
        boolean     bProceed = false;
        Statement   stmt     = null;

        try
        {
            ///////////////////////////////////////////////////////////////////
            // Open the log file if we are supposed to log SQL statements.
            if ( m_logFileName != null ) {
                m_log = new FileWriter(m_logFileName, m_logAppend);
            }

            Document doc = null;
            if (source instanceof String) {
                doc = this.readDocument((String) source);

            } else if (source instanceof InputStream) {
                doc = this.readDocument((InputStream) source); 
           }

            ////////////////////////////////////////////////////////
            // Make sure we can connect to the database
            m_conn = connect(database, username, password);
            stmt = m_conn.createStatement();

            ////////////////////////////////////////////////////////
            // Verify that the user wants to do this.
            if(this.isQuiet() == false && (!this.isNoninteractive()) )
            {
                System.out.print("DBSetup will permanently drop tables and data. DO YOU WANT TO PROCEED? (Y/N): ");

                if(Character.toUpperCase((char)System.in.read()) == 'Y')
                {
                    System.out.println();
                    bProceed = true;
                }
                else
                    System.out.println("Aborted.");
            }
            else bProceed = true;   // If they go quiet, delete without asking.

            if(bProceed == true)
            {
                // drop the views first
                StrongCollection views  = (StrongCollection)
                    View.getViews(doc.getFirstChild(),
                                  JDBC.toType(database),
                                  this);
                
                Iterator iterTab = views.iterator();

                while(iterTab.hasNext() == true)
                {
                    View view = (View)iterTab.next();

                    try
                    {
                        view.drop();

                        if(this.isQuiet() == false)
                            System.out.println("Dropped View: " +
                                               view.getName());

                        this.m_lCreatedViews ++;
                    }
                    catch(SQLException e)
                    {
                        if(this.isQuiet() == false)
                        {
                            System.out.println("Error: Cannot drop view " +
                                               view.getName());
                            JDBC.printSQLException(e);
                        }

                        this.m_lFailedViews ++;
                    }
                }
                
                // Post Processing
                View.uninstallCleanup(this);

                // remove tables in reverse order of their creation
                // to bypass dependency constraints
                StrongCollection tables  = (StrongCollection)
                    Table.getTables(doc.getFirstChild(),
                                    JDBC.toType(database),
                                    this);
                tables.reverse();

                iterTab = tables.iterator();

                while(iterTab.hasNext() == true)
                {
                    Table tab = (Table)iterTab.next();

                    try
                    {
                        tab.drop();

                        if(this.isQuiet() == false)
                            System.out.println("Dropped Table: " + tab.getName());

                        this.m_lCreatedTables ++;
                    }
                    catch(SQLException e)
                    {
                        if(this.isQuiet() == false)
                        {
                            System.out.println("Error: " + "Cannot drop table " + tab.getName());
                            JDBC.printSQLException(e);
                        }

                        this.m_lFailedTables ++;
                    }
                }
                
                // Post Processing
                Table.uninstallCleanup(this);
            }

            //////////////////////////////////////////////////////////////
            // Print Results

            if(this.isQuiet() == false)
            {
                DecimalFormat fmt = new DecimalFormat("#,###");

                System.out.println();
                System.out.println(fmt.format(this.m_lCreatedViews) +
                                   " views dropped succesfully.");
                System.out.println(fmt.format(this.m_lFailedViews)  +
                                   " views failed to drop.");
                System.out.println(fmt.format(this.m_lCreatedTables) +
                                   " tables dropped succesfully.");
                System.out.println(fmt.format(this.m_lFailedTables)  +
                                   " tables failed to drop.");
                //System.out.println(fmt.format(this.m_lCreatedIndexes) + " indexes dropped successfully.");
                //System.out.println(fmt.format(this.m_lFailedIndexes) + " indexes failed to drop.");
            }
        } catch(SAXException e) {
            throw new IOException(e.getMessage());
        } finally {
            if ( m_log != null ) {
                try { m_log.close(); } catch ( Exception e ) {}
            }
            try {
                if(stmt != null) stmt.close();
                if(m_conn != null) m_conn.close();
            } catch(SQLException e) {}
        }

        return true;
    }

    public boolean validate(String file, String database, String username, String password) throws ClassNotFoundException, IOException, SQLException
    {
        try
        {
            Document doc = this.readDocument(file);

            ////////////////////////////////////////////////////////
            // Make sure we can connect to the database
            m_conn = connect(database, username, password);
            DatabaseMetaData meta = m_conn.getMetaData();

            Iterator iterTab = Table.getTables(doc.getFirstChild(), JDBC.toType(database), this).iterator();

            while(iterTab.hasNext() == true)
            {
                Table tab = (Table)iterTab.next();

                try
                {
                    ResultSet set = meta.getTables(null, null, tab.getName(), null);
                    if(set.next() == true)
                    {
                        if(this.isQuiet() == false)
                            System.out.println("Found Table : " + tab.getName());

                        // Check the Columns
                        Iterator iterCol = tab.getColumns().iterator();

                        while(iterCol.hasNext() == true)
                        {
                            Column col = (Column)iterCol.next();

                            set = meta.getColumns(null, null, tab.getName(), col.getName());
                            if(set.next() == true)
                            {
                                if(this.isQuiet() == false)
                                    System.out.println("Found Column: " + col.getName());
                            }
                        }

                        this.m_lCreatedTables ++;
                    }
                }
                catch(SQLException e)
                {
                    if(this.isQuiet() == false)
                    {
                        System.out.println("Error: " + "Cannot find table " + tab.getName());
                        JDBC.printSQLException(e);
                    }

                    this.m_lFailedTables ++;
                }
            }

            //////////////////////////////////////////////////////////////
            // Print Results

            if(this.isQuiet() == false)
            {
                DecimalFormat fmt = new DecimalFormat("#,###");

                System.out.println();
                System.out.println(fmt.format(this.m_lCreatedTables) + " tables validated.");
                System.out.println(fmt.format(this.m_lFailedTables)  + " tables failed to validate.");
                System.out.println(fmt.format(this.m_lCreatedIndexes) + " indexes validated.");
                System.out.println(fmt.format(this.m_lFailedIndexes) + " indexes failed to validate.");
            }
        } catch(SAXException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if(m_conn != null) m_conn.close();
            } catch(SQLException e) {}
        }

        return true;
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

    /**
     * Retrieves whether questions should be asked on stdin.
     *
     * @return boolean
     *      true if questions will be asked, false otherwise.
     */
    public boolean isNoninteractive()
    {
        return this.m_bNoninteractive;
    }

    /**
     * Sets whether questions should be asked on stdin.
     *
     * @param noninteractive
     *      true if questions should be asked, false otherwise.
     */
    public void setNoninteractive(boolean noninteractive)
    {
        this.m_bNoninteractive = noninteractive;
    }

    /**
     * Retrieves whether status messages will be printed to System.out.
     *
     * @return boolean
     *      true if status messages will be printed, false otherwise.
     */
    public boolean isVerbose()
    {
        return this.m_bVerbose;
    }

    /**
     * Sets whether status messages will be printed to System.out.
     *
     * @param quiet
     *      true if status messages should be printed, false otherwise.
     */
    public void setVerbose(boolean quiet)
    {
        this.m_bVerbose = quiet;
    }

    private class ErrorHandler implements org.xml.sax.ErrorHandler
    {
        public void fatalError(SAXParseException e) throws SAXException
        {
            System.out.println("Fatal Error: " + e);
        }

        public void error(SAXParseException e) throws SAXException
        {
            System.out.println("Error: " + e);
        }

        public void warning(SAXParseException e) throws SAXException
        {
            System.out.println("Warning: " + e);
        }
    }

    protected void doSQL(String cmd) throws SQLException
    {
        this.doSQL(cmd, false);
    }

    /**
     * Thou shalt be responsible for .close()'ing the returned prepared
     * statement when they're through, lest they suffer a cursor flood
     */
    protected Statement doSQL(String cmd, boolean bPrepared) throws SQLException
    {
        Statement stmt;
        
        // Cache the original commit option
        boolean committing = this.getConn().getAutoCommit();
        if (committing)
            this.getConn().setAutoCommit(false);

        if(bPrepared == true)
            stmt = this.getConn().prepareStatement(cmd);
        else
            stmt = this.getConn().createStatement();

        if( m_bVerbose )
            System.out.println("Command: " + cmd);

        if ( bPrepared == false )  {
            try {
                stmt.executeUpdate(cmd);
                this.getConn().commit();
            } catch (SQLException e) {
                try { this.getConn().rollback(); } 
                catch (Exception e2) {
                    // Log this?
                }
                throw e;
            } finally {
                if (stmt != null) stmt.close();
            }
        }
        
        // Reset the commit option
        this.getConn().setAutoCommit(committing);
        
        return stmt;
    }

    protected Connection getConn () { return m_conn; }

    public void setTypeMapFile ( String tmapFile ) {
        m_typeMapFile = tmapFile;
    }
    public void setTypeMapStream ( InputStream is ) {
        m_typeMapStream = is;
    }

    private Connection connect (String database,
                                String user, String password)
        throws ClassNotFoundException, SQLException {
        return connect(null, database, user, password);
    }

    private Connection connect (String driver, String database,
                                String user, String password)
        throws ClassNotFoundException, SQLException {
        if ( driver == null ) {
            driver = JDBC.getDriverString(database);
        }

        JDBC.loadDriver(driver); 
        Connection conn = DriverManager.getConnection(database, user, password);
        // mysql complains if autocomit is true and you try to commit...
        // ddl operations are not transactional anyhow.
        conn.setAutoCommit(false);

        return conn;

    }

    public void init (boolean verbose, boolean sqlonly) {}

    // The implementation of the Statement and PreparedStatement classes
    // in the org.hyperic.util.jdbc.log package will callback this method
    // when they have SQL to execute.
    public void logSQL ( String sql ) {
        if ( m_log != null ) {
            try {
                m_log.write(sql + ";\n");
                m_log.flush();
            } catch ( Exception e ) {
                System.err.println("DBSetup: ERROR writing to log file: " + e);
            }
        }
    }
    
    protected int getDbType() {
    	int	iResult = -1;
    	
    	try {
			iResult = JDBC.toType(this.getConn().getMetaData().getURL());
    	}
    	catch(SQLException e) {}

		return iResult;    	
    }
}
