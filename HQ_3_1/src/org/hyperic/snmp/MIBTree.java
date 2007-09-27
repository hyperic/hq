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

package org.hyperic.snmp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * MIB file parser intended ONLY for
 *  OBJECT-TYPE name -> OID conversion
 * For example, converting:
 *  "wwwServiceDescription"
 * to:
 *  "1.3.6.1.2.1.65.1.1.1.1.2"
 * IMPORTS are ignored and parse() order of MIBs does not
 * matter, provided all MIB files required to lookup a given
 * name have been parsed prior to calling getOID(name). 
 */
public class MIBTree {
    public static final String PROP_MIBS_DIR = "snmp.mibs.dir";

    private static Log log =
        LogFactory.getLog(MIBTree.class.getName());

    private static final int INDEX      = 1;
    private static final int IDENTIFIER = 2;
    private static final int NO_ACCESS  = 4;

    private static final int MAX_OID_LEN = 127;
    private static final String QUOTE = "\"";
    private static final String ASSIGN = "::=";
    private static MIBTree instance = null;
    private HashMap parsedFiles = new HashMap();
    private HashMap table = new HashMap(); //parsed MIBs
    private HashMap oids = new HashMap(); //cache OID conversion
    private LineNumberReader reader;
    private List tokens = new ArrayList(); //lexx/yakk/hakk
    private List previous = new ArrayList();
    private StringTokenizer tokenizer;
    private String currentMIB;
    private boolean inDescription = false;
    private String lastLookupFailure;

    private static final int T_NAME   = 0;
    private static final int T_PARENT = 1;
    private static final int T_OID    = 2;

    //every MIB depends on SNMPv2-SMI
    private static final String[][] SNMPv2_SMI = {
        { "org",          "iso",      "3" },
        { "dod",          "org",      "6" },
        { "internet",     "dod",      "1" },
        { "directory",    "internet", "1" },
        { "mgmt",         "internet", "2" },
        { "mib-2",        "mgmt",     "1" },
        { "transmission", "mib-2",    "10" },
        { "experimental", "internet", "3" },
        { "private",      "internet", "4" },
        { "enterprises",  "private",  "1" },
        { "security",     "internet", "5" },
        { "snmpV2",       "internet", "6" },
        { "snmpDomains",  "snmpV2",   "1" },
        { "snmpProxys",   "snmpV2",   "2" },
        { "snmpModules",  "snmpV2",   "3" },
    };

    //commonly used objects from SNMPv2-MIB
    private static final String[][] SNMPv2_MIB = {
        { "sysDescr",    "system", "1" },
        { "sysObjectID", "system", "2" },
        { "sysUpTime",   "system", "3" },
        { "sysContact",  "system", "4" },
        { "sysName",     "system", "5" },
        { "sysLocation", "system", "6" },
        { "sysServices", "system", "7" },
    };

    class MIBNode {
        String parent;
        String oid;
        int flags = 0;

        MIBNode(String oid, String parent) {
            this.oid = oid.intern();
            this.parent = parent;
        }

        MIBNode getParent() {
            return lookup(this.parent);
        }

        String getMIB() {
            return "unknown";
        }

        boolean hasFlag(int flag) {
            return (this.flags & flag) != 0;            
        }

        //to keep the MIBNode objects as small as possible,
        //we work backwards here to compose the full oid
        //only when they are asked for.  parser will cache
        //so this is a one-time expense.
        int[] getOID(String name) {
            int[] scratch = new int[MAX_OID_LEN];
            int ix = scratch.length;
            MIBNode node = this;
            boolean indexApplies =
                !hasFlag(IDENTIFIER) && !hasFlag(NO_ACCESS);
            boolean hasIndex = false;
            boolean isDebug = log.isDebugEnabled();

            while ((node != null) && (ix > 0)) {
                scratch[--ix] = Integer.parseInt(node.oid);
                MIBNode parent = node.getParent();

                if (parent == null) {
                    if (node.getClass() != ISONode.class) {
                        lastLookupFailure = node.parent;
                        return null;
                    }
                    else {
                        break;
                    }
                }
                if (parent.hasFlag(INDEX)) {
                    hasIndex = true;
                }

                node = parent;
            }

            int len = scratch.length - ix;
            boolean addIndex = indexApplies && !hasIndex;
            int alloc = addIndex ? len+1 : len;
            int[] oid = new int[alloc];

            System.arraycopy(scratch, ix, oid, 0, len);

            //sysUpTime.0
            if (addIndex) {
                oid[len] = 0;
                if (isDebug) {
                    log.debug(getMIB() + "." + name +
                              " has no index, appending .0");
                }
            }
            
            return oid;
        }
    }

    class ISONode extends MIBNode {
        ISONode() {
            super("1", null);
            this.flags = IDENTIFIER;
        }
    }

    class DebugMIBNode extends MIBNode {
        String mib; //only useful if log.isDebugEnabled

        DebugMIBNode(String oid, String parent) {
            super(oid, parent);
            if (instance != null) {
                this.mib = instance.currentMIB;
            }
        }

        String getMIB() {
            return this.mib;
        }
    }

    public MIBTree() {
        this.table.put("iso", new ISONode());
    }

    public void init() {
        this.currentMIB = "SNMPv2-SMI";
        add(SNMPv2_SMI, IDENTIFIER);
        
        this.currentMIB = "SNMPv2-MIB";
        add("system", "mib-2", "1", IDENTIFIER);
        add(SNMPv2_MIB, 0);

        this.currentMIB = null;

        String dir = System.getProperty(PROP_MIBS_DIR);
        if (dir != null) {
            File mibs = new File(dir);
            if (!mibs.exists()) {
                log.debug(mibs + " MIB dir does not exist");
            }
            try {
                parse(mibs);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
    public synchronized static MIBTree getInstance() {
        if (instance == null) {
            instance = new MIBTree();
            instance.init();
        }
        return instance;
    }
    
    MIBNode lookup(String name) {
        return (MIBNode)this.table.get(name);
    }

    public static void setMibDir(String dir) {
        System.setProperty(PROP_MIBS_DIR, dir);
    }

    public static String toString(int[] oid) {
        StringBuffer buffer = new StringBuffer(oid.length * 2);
        buffer.append(oid[0]);

        for (int i=1 ; i<oid.length ; i++) {
            buffer.append('.').append(oid[i]);
        }

        return buffer.toString();
    }

    public int[] getOID(String name) {
        int[] oid = (int[])this.oids.get(name);
        if (oid != null) {
            return oid;
        }

        if (name.indexOf('.') != -1) {
            //handle "cpmCPUTotal5sec.1"
            StringTokenizer tok = new StringTokenizer(name, ".");
            int[] scratch = new int[MAX_OID_LEN];
            int ix = 0;

            while (tok.hasMoreTokens()) {
                String node = tok.nextToken();

                if (Character.isDigit(node.charAt(0))) {
                    scratch[ix++] = Integer.parseInt(node);
                }
                else {
                    int[] subnode = getOID(node);
                    if (subnode == null) {
                        return null;
                    }
                    System.arraycopy(subnode, 0, scratch,
                                     ix, subnode.length);
                    ix += subnode.length;
                }
            }
            oid = new int[ix];
            System.arraycopy(scratch, 0, oid, 0, ix);
        }
        else {
            MIBNode mibnode = lookup(name);
            if (mibnode == null) {
                return null;
            }
            oid = mibnode.getOID(name);
            if (oid == null) {
                log.warn(name +
                         " found in tree but unable to resolve OID." +
                         " lastLookupFailure=" + this.lastLookupFailure);
                return null;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(name + " resolved to: " + toString(oid));
        }
        
        this.oids.put(name, oid); //cache result

        return oid;
    }

    private void add(String[][] table, int flags) {
        for (int i=0; i<table.length; i++) {
            String[] entry = table[i];

            add(entry[T_NAME],
                entry[T_PARENT],
                entry[T_OID],
                flags);
        }
    }

    private void add(String name, String parent, String oid, int flags) {
        boolean isDebug = log.isDebugEnabled();
        MIBNode node = (MIBNode)this.table.get(name);

        if (node != null) {
            if (isDebug) {
                log.debug(this.currentMIB + "." + name +
                          " already added by " + node.getMIB());
            }
            return;
        }
        node =
            isDebug ?
            new DebugMIBNode(oid, parent) :
            new MIBNode(oid, parent);

        node.flags = flags;

        this.table.put(name, node);
    }

    //on-demand StringTokenizer.nextToken()
    private String token(int ix) {
        if (ix < this.tokens.size()) {
            return (String)this.tokens.get(ix);
        }
        while (this.tokenizer.hasMoreTokens()) {
            String token = this.tokenizer.nextToken();
            this.tokens.add(token);
            if (ix+1 == this.tokens.size()) {
                return token;
            }
        }
        return ""; //avoid NPE and goofy "CONST".equals(val)
    }

    private String where(int start) {
        return
            " at " +
            this.currentMIB + ":" +
            ((start == 0) ? "" : start + "..") +
            this.reader.getLineNumber();
    }

    private void tokenize(String line) {
        this.previous.clear();
        this.previous.addAll(this.tokens);
        this.tokens.clear();

        if (line == null) {
            line = "";
        }
        this.tokenizer = new StringTokenizer(line);
    }

    private String readToLine(String contains)
        throws IOException {

        int start = this.reader.getLineNumber();
        String line;

        while ((line = readLine()) != null) {
            if (line.indexOf(contains) != -1) {
                return line;
            }
        }

        throw new IOException("Expecting '" + contains + "'" +
                              " not found" + where(start));
    }

    //skip all text within DESCRIPTION "..."
    //since certain MIBs have text which we would
    //otherwise get parsed, which we dont want.
    private String skipDescription(String line)
        throws IOException {

        //flag to prevent recursing on ourselves
        this.inDescription = true;
        try {
            if (line.indexOf(QUOTE) == -1) {
                line = readToLine(QUOTE);
            }
            if (!line.endsWith(QUOTE)) {
                line = readToLine(QUOTE);
            }
            return readLine();
        } finally {
            this.inDescription = false;
        }
    }

    private String readLine() throws IOException {
        String line;

        while ((line = this.reader.readLine()) != null) {
            line = line.trim();
            if ((line.length() == 0) ||
                line.startsWith("--")) //skip comments
            {
                continue;
            }
            int ix = line.indexOf("--");
            if (ix != -1) {
                line = line.substring(0, ix).trim();
            }
            if (line.length() != 0) {
                if (!this.inDescription &&
                    line.startsWith("DESCRIPTION"))
                {
                    //this will recurse.
                    return skipDescription(line);
                }
                else {
                    return line;
                }
            }
        }

        return null;
    }

    private void parseId(String name, String line, int flags)
        throws IOException {

        if (line.endsWith(ASSIGN)) {
            line = readLine();
        }

        int start = line.indexOf('{');
        int end = line.indexOf('}');
        if ((start != -1) && (end == -1)) {
            //e.g. cisco LAN-EMULATION-CLIENT-MIB.my
            //atmfLanEmulation  OBJECT IDENTIFIER ::= {
            //                           enterprises
            //                              atmForum(353)
            //                                 atmForumNetworkManagement(5)
            //                                  3
            //                }
            String nextLine;
            do {
                nextLine = readLine();
                line += " " + nextLine;
            } while ((end = line.indexOf('}')) == -1);
        }

        if ((start == -1) || (end == -1)) {
            throw new IOException("Expecting ::= {...} " +
                                  " in " + line + where(0));
        }
        line = line.substring(start+1, end).trim();
        StringTokenizer tok = new StringTokenizer(line);
        int numTokens = tok.countTokens();
        if (numTokens < 2) {
            throw new IOException("Invalid ID " +
                                  " in " + line + where(0));
        }
        if (numTokens == 2) {
            //common case ::= { wwwServiceEntry 4 }
            String parent = tok.nextToken();
            String number = tok.nextToken();
            add(name, parent, number, flags);            
        }
        else {
            //::= { iso org(3) dod(6) 1 }
            //atmfLanEmulation ... ::= (above)
            String parent = tok.nextToken();
            while (tok.hasMoreTokens()) {
                String next = tok.nextToken();

                int openParen = next.indexOf('(');
                if (openParen != -1) {
                    int closeParen = next.indexOf(')');
                    if (closeParen == -1) {
                        throw new IOException("Expecting ')' " +
                                              " in " + line + where(0));
                    }
                    String subName = next.substring(0, openParen);
                    String subNum  = next.substring(openParen+1, closeParen);
                    add(subName, parent, subNum, IDENTIFIER);
                    parent = subName;
                }
                else {
                    add(name, parent, next, flags);
                }
            }
        }
    }

    private void parseObjectType() throws IOException {
        //:: = { wwwService 65 }
        String name = token(0);
        String line;
        int flags = NO_ACCESS;
        while ((line = readLine()) != null) {
            if (line.indexOf(ASSIGN) != -1) {
                break;
            }
            if (line.startsWith("INDEX")) {
                flags |= INDEX;
            }
            else if (line.startsWith("SYNTAX")) {
                if (line.indexOf("SEQUENCE") != -1) {
                    flags |= INDEX;
                }
            }
            else if (line.startsWith("ACCESS") ||
                     line.startsWith("MAX-ACCESS"))
            {
                if (line.indexOf("not-accessible") == -1) {
                    flags &= ~NO_ACCESS;
                }
            }
        }

        parseId(name, line, flags);
    }

    private void parseObjectIdentifier(String line) throws IOException {
        //wwwMIBObjects     OBJECT IDENTIFIER ::= { wwwMIB 1 }
        String name = token(0);
        if (line.indexOf(ASSIGN) == -1) {
            line = readToLine(ASSIGN);
        }
        parseId(name, line, IDENTIFIER);
    }

    private boolean hasParsedFile(File file) {
        String name = file.getName();
        if (this.parsedFiles.get(name) != null) {
            return true;
        }
        else {
            this.parsedFiles.put(name, Boolean.TRUE);
            return false;
        }
    }

    private boolean parseFile(File file) throws IOException {
        return parse(file.toString(),
                     new FileInputStream(file));
    }

    private class AcceptFilter {
        List filter = null;

        AcceptFilter(String[] accept) {
            if ((accept != null) && (accept.length != 0)) {
                filter = Arrays.asList(accept);
            }
        }

        boolean accept(String name) {
            if (filter != null) {
                return filter.contains(name);
            }
            else {
                return true;
            }
        }
    }
    
    public boolean parse(JarFile jar)
        throws IOException {

        return parse(jar, null);
    }
    
    public boolean parse(JarFile jar, String[] accept)
        throws IOException {

        AcceptFilter filter = new AcceptFilter(accept);

        for (Enumeration e = jar.entries(); e.hasMoreElements();) {
            JarEntry entry = (JarEntry)e.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (!entry.getName().startsWith("mibs/")) {
                continue;
            }
            String name = entry.getName().substring(5);
            if (!filter.accept(name)) {
                continue;
            }
            if (hasParsedFile(new File(name))) {
                continue;
            }

            String where = jar.getName() + "!" + entry.getName();
            parse(where, jar.getInputStream(entry));
        }

        return true;
    }

    public boolean parse(File file)
        throws IOException {

        return parse(file, null);
    }

    public boolean parse(File file, String[] accept)
        throws IOException {

        if (hasParsedFile(file)) {
            return true;
        }
        if (file.isDirectory()) {
            File[] mibs = file.listFiles();
            if ((mibs == null) || (mibs.length == 0)) {
                log.debug("No MIBs in directory: " + file);
                return false;
            }
            AcceptFilter filter = new AcceptFilter(accept);
            log.debug("Loading MIBs in directory: " + file);
            for (int i=0; i<mibs.length; i++) {
                File mib = mibs[i];
                if (mib.isDirectory()) {
                    continue;
                }
                if (!filter.accept(mib.getName())) {
                    continue;
                }
                parseFile(mib);
            }
            return true;
        }
        else if (file.getName().endsWith(".jar")) {
            JarFile jar = new JarFile(file);
            try {
                return parse(jar, accept);
            } finally {
                jar.close();
            }
        }
        else {
            return parseFile(file);
        }
    }

    public boolean parse(URL url) throws IOException {
        if (hasParsedFile(new File(url.getFile()))) {
            return true;
        }
        return parse(url.toString(), url.openStream());
    }

    public boolean parse(String name, InputStream is)
        throws IOException {

        boolean isSuccess = false;
        try {
            isSuccess = parse(is);
        } catch (IOException e) {
            throw new IOException("Failed to load MIB: '" + name +
                                  "': " + e);
        }

        log.debug("Loading MIB: '" + name + "': " +
                  (isSuccess ? "success" : "skipped")); 

        return isSuccess;
    }
    
    public boolean parse(InputStream is) throws IOException {
        this.lastLookupFailure = null;
        try {
            return parseMIB(is);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e + where(0));
        } finally {
            this.tokens.clear();
            this.previous.clear();
            try {
                is.close();
            } catch (IOException e) {}
        }
    }

    public String getLastLookupFailure() {
        return this.lastLookupFailure;
    }
    
    private boolean parseMIB(InputStream is) throws IOException {
        String line;
        this.reader =
            new LineNumberReader(new InputStreamReader(is));
        this.currentMIB = "";

        tokenize(readLine());
        if (!token(1).equals("DEFINITIONS")) {
            return false;
        }

        this.currentMIB = token(0);
        int size = this.table.size();

        while ((line = readLine()) != null) {
            tokenize(line);
            String first = token(0);

            if (first.equals("IMPORTS") ||
                first.equals("EXPORTS"))
            {
                readToLine(";");
                continue;
            }

            if (first.equals("SYNTAX")) {
                continue;
            }

            String second = token(1);
            if (second == null) {
                continue;
            }

            if ((line.indexOf("SEQUENCE {") != -1) ||
                (line.indexOf("CHOICE {") != -1))
            {
                readToLine("}");
            }
            else if (second.equals("OBJECT") &&
                token(2).equals("IDENTIFIER"))
            {
                parseObjectIdentifier(line);
            }
            else if ((this.previous.size() == 1) &&
                     first.equals("OBJECT") &&
                     second.equals("IDENTIFIER"))
            {
                //snmpFrameworkAdmin
                //   OBJECT IDENTIFIER ::= { snmpFrameworkMIB 1 }
                Object name = this.previous.get(0);
                line = name + " " + line;
                this.tokens.add(0, name);
                parseObjectIdentifier(line);
            }       
            else if (second.equals("OBJECT-TYPE") ||
                     second.equals("MODULE-IDENTITY") ||
                     second.equals("OBJECT-IDENTITY"))
            {
                parseObjectType();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(this.currentMIB + " added " +
                      (this.table.size() - size) + " entries");
        }
        
        return true;
    }

    public static void main(String[] args) throws Exception {
        ArrayList names = new ArrayList();
        MIBTree tree = MIBTree.getInstance();

        for (int i=0; i<args.length; i++) {
            File file = new File(args[i]);
            if (file.exists()) {
                if (!tree.parse(file)) {
                    System.out.println(args[i] + " is not valid MIB");
                }
                else {
                    System.out.println(args[i] + " parsed");
                }
            }
            else {
                names.add(args[i]);
            }
        }

        if (names.size() == 0) {
            names.addAll(tree.table.keySet());
        }

        for (int i=0; i<names.size(); i++) {
            String name = (String)names.get(i);
            int[] oid = tree.getOID(name);
            if (oid == null) {
                System.out.println("Failed to get oid for: " + name +
                                   " (lastLookupFailure=" +
                                   tree.lastLookupFailure + ")");
            }
            else {
                System.out.println(name + "=" + toString(oid));
            }
        }
    }
}
