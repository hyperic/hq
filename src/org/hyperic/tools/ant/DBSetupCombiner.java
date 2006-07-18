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

package org.hyperic.tools.ant;

import org.apache.tools.ant.BuildException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hyperic.util.StringUtil;

/**
 * An ant task to combine multiple DBSetup XML files into
 * a single file.
 */
public class DBSetupCombiner extends BaseFileSetTask {

    private File destFile = null;
    private String order = null;
    private String name = null;
    private String notice = null;
    private boolean debug = false;

    public DBSetupCombiner() {}

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDestfile(File destFile) {
        this.destFile = destFile;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public void execute() throws BuildException {

        validateAttributes();

        List filesToCombine;
        PrintWriter pw = null;

        // First get all the files, unsorted
        filesToCombine = getAllFiles();

        // Reorder according to user-specified order
        if ( order != null ) {
            List orderList = StringUtil.explode(order, ", \n\r\t");
            Collections.sort(filesToCombine, 
                             new OrderComparator(orderList));
        }

        try {
            // Open the destFile
            pw = new PrintWriter(new FileWriter(destFile));

            // Write the prefix
            writePrefix(pw);

            // Concatenate the files
            catFiles(filesToCombine, pw);

            // Write the suffix
            writeSuffix(pw);

        } catch ( IOException ioe ) {
            throw new BuildException("Error combining files: " + ioe);

        } finally {
            if ( pw != null ) {
                try { pw.close(); } catch ( Exception e ) {}
            }
        }
    }

    /**
     * This method concatenates a series of files to a single
     * destination, stripping out XML prefixes and top-level elements.
     *
     * @param files A list of the files to be concatenated
     * @param pw Destination for writes.
     */
    private void catFiles(List files, 
                          PrintWriter pw) throws IOException {

        BufferedReader in = null;
        try {
            for (int i=0; i<files.size(); i++) {
                File currentFile = (File) files.get(i);
                String filename = currentFile.getName();
                in = new BufferedReader
                    (new InputStreamReader
                     (new FileInputStream(currentFile)));

                if (debug) pw.println("<!-- BEGIN: " + filename + " -->");

                String line;
                String tline;
                while ((line = in.readLine()) != null) {

                    tline = line.trim();

                    // Skip XML directive lines, and top-level element lines
                    if ( tline.startsWith("<?") ||
                         tline.startsWith("</Covalent.DBSetup>") ) {
                        continue;

                    } else if ( tline.startsWith("<Covalent.DBSetup") ) {
                        while ( tline.indexOf(">") == -1 ) {
                            tline = in.readLine();
                            if ( tline == null ) break;
                        }
                        continue;
                    }
                    pw.println(line);
                }
                in.close();
                in = null;

                if (debug) pw.println("<!-- END: " + filename + " -->");
            }
        } finally {
            // Close resources.
            if (in != null) {
                try { in.close(); } catch (Exception e) {}
            }
        }
    }

    protected void validateAttributes () throws BuildException {
        super.validateAttributes();
        if (destFile == null) {
            throw new BuildException("DBSetupCombiner: No 'destFile' attribute "
                                     + "specified.");
        }
        if (name == null) {
            throw new BuildException("DBSetupCombiner: No 'name' attribute "
                                     + "specified.");
        }
        if (notice == null) {
            throw new BuildException("DBSetupCombiner: No 'notice' attribute "
                                     + "specified.");
        }
    }

    public static final String XML_PREFIX
        = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private void writePrefix ( PrintWriter pw ) throws IOException {
        pw.println(XML_PREFIX);
        pw.println("<Covalent.DBSetup "
                   + "name=\"" + this.name + "\" "
                   + "notice=\"" + this.notice + "\">");
    }
    private void writeSuffix ( PrintWriter pw ) throws IOException {
        pw.println("</Covalent.DBSetup>");
    }

    class OrderComparator implements Comparator {

        public List order;

        public OrderComparator (List order) {this.order = order;}
        
        public int compare (Object o1, Object o2) {
            // Make sure they are both files, otherwise we'll just say they're 
            // always "equal"
            if ( o1 instanceof File && o2 instanceof File ) {

                // Get basenames for files
                String f1 = ((File) o1).getName();
                String f2 = ((File) o2).getName();
                
                // Find the index at which this basename occurs in the order
                int idx1 = findIndex(f1);
                int idx2 = findIndex(f2);

                // If one was found and the other was not, the one 
                // that was found is automatically "less than" the one
                // that was not.
                if (idx1 == -1 && idx2 != -1) {
                    return Integer.MAX_VALUE;

                } else if (idx2 == -1 && idx1 != -1) {
                    return Integer.MIN_VALUE;
                    
                } else {
                    // If they were both found, or if they were both -1,
                    // just return the difference
                    return (idx1 - idx2);
                }
            }
            return 0;
        }

        private int findIndex ( String fname ) {

            int indexOfLongestMatch = -1;
            int lengthOfLongestMatch = 0;

            String possibleMatch;
            int possibleMatchLen;

            for ( int i=0; i<order.size(); i++ ) {

                possibleMatch = order.get(i).toString();
                possibleMatchLen = possibleMatch.length();

                if (fname.startsWith(possibleMatch) &&
                    possibleMatchLen > lengthOfLongestMatch) {

                    indexOfLongestMatch = i;
                    lengthOfLongestMatch = possibleMatchLen;
                }
            }

            return indexOfLongestMatch;
        }

        public boolean equals(Object o) {
            if (o instanceof OrderComparator) {
                OrderComparator oc = (OrderComparator) o;
                return (this.order == null && oc.order == null) ||
                    this.order.equals(oc.order);
            }
            return false;
        }
    }
}
