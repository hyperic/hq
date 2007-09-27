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

package org.hyperic.util.file.match;

import java.io.File;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.BuildException;

class MasterMatchSelector implements FileSelector {

    private MatchSelector[] subSelectors;
    private boolean allowMultipleMatches;

    public MasterMatchSelector ( MatchSelector[] subSelectors,
                                 boolean allowMultipleMatches ) {
        this.subSelectors = subSelectors;
        this.allowMultipleMatches = allowMultipleMatches;
    }

    public void setScanner (MatcherScanner scanner) {
        for ( int i=0; i<subSelectors.length; i++ ) {
            subSelectors[i].setMatcherScanner(scanner);
        }
    }

    public boolean isSelected(File basedir, String filename, File file)
        throws BuildException {

        MatchSelector matched = null;

        for ( int i=0; i<subSelectors.length; i++ ) {
            if ( subSelectors[i].isSelected(basedir, filename, file) ) {

                if ( !allowMultipleMatches && matched != null ) {
                    MatchSelector[] matchers = { matched, subSelectors[i] };
                    MultipleMatchesException mme 
                        = new MultipleMatchesException(file, matchers);
                    throw mme;

                }

                matched = subSelectors[i];
            }
        }
        return (matched != null);
    }
}
