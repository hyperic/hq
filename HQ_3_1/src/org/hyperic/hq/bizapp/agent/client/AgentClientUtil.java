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

package org.hyperic.hq.bizapp.agent.client;

import org.hyperic.hq.bizapp.agent.TokenData;
import org.hyperic.hq.bizapp.agent.TokenManager;
import org.hyperic.hq.bizapp.agent.TokenStorer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public class AgentClientUtil {
    public static String getLocalAuthToken(String tokenFile)
        throws FileNotFoundException, IOException
    {
        TokenManager tm;
        int nTries = 20;
        List tokens;

        // Get a token manager with huge outdate -- shouldn't matter though
        tm = new TokenManager(1000 * 1000, null); 
        
        tm.load(new FileInputStream(tokenFile));
        tokens = tm.getTokens();
        if(tokens.size() == 0){
            throw new IOException("Token file contains no valid tokens");
        }
        return ((TokenData)tokens.get(0)).getToken();
    }

    private static class MyStorer 
        implements TokenStorer
    {
        private String tokenFile;

        private MyStorer() { }

        public MyStorer(String tokenFile) {
            this.tokenFile = tokenFile;
        }

        public OutputStream getTokenStoreStream()
            throws IOException
        {
            return new FileOutputStream(this.tokenFile);
        }
    }

    public static void generateNewTokenFile(String tokenFile,
                                            String firstToken)
        throws IOException
    {
        TokenManager tm;

        tm = new TokenManager(1000 * 1000, new MyStorer(tokenFile));
        tm.addToken(new TokenData(firstToken, System.currentTimeMillis(),
                                  true));
        tm.store();
    }
}
