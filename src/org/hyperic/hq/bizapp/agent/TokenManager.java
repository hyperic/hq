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

package org.hyperic.hq.bizapp.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * The token manager is a class which knows how to read a properties
 * file which contains a list of tokens with the following information:
 *
 *   1 - The Token    - A simple string 
 *   2 - Create Time  - The time when the token was generated
 *   3 - Locked       - A flag indicating whether the token is verified
 *                      or it is just temporary, in which case it will
 *                      timeout after a certain time after the creation time.
 *
 * Tokens which are pending and exceed the timeout are eliminated
 * automagically.
 */
public class TokenManager {
    private static final String PROP_PREFIX    = "tokenData.";
    private static final String PROP_NUMTOKENS = PROP_PREFIX + "numTokens";
    private static final String TOKEN_LOCKED   = "locked";
    private static final String TOKEN_PENDING  = "pending";

    private TokenStorer storer;
    private Set         tokens;
    private long        timeout;

    private static Set createSet(){
        return Collections.synchronizedSet(new HashSet());
    }

    /**
     * Create a token manager with a timeout for pending tokens of 'timeout'
     * milliseconds.  
     */
    public TokenManager(long timeout, TokenStorer storer){
        this.tokens  = TokenManager.createSet();
        this.timeout = timeout;
        this.storer  = storer;
    }

    public void addToken(TokenData newToken){
        this.tokens.add(newToken);
    }

    public void deleteToken(TokenData token)
        throws TokenNotFoundException
    {
        if(this.tokens.remove(token) == false){
            throw new TokenNotFoundException("Token '" + token + 
                                             "' not found");
        }
    }

    /**
     * Set the token managed by the manager to the given locked
     * mode.
     *
     * @param token  Token to set locked/unlocked
     * @param locked True or false if the token is locked or pending
     */
    public void setTokenLocked(TokenData token, boolean locked)
        throws TokenNotFoundException
    {
        TokenData tData;

        if(!this.tokens.contains(token)){
            throw new TokenNotFoundException("Token '" + token + 
                                             "' not found");
        }

        // Delete the old token
        this.deleteToken(token);
        tData = (TokenData)token.clone();
        tData.setLocked(locked);
        this.tokens.add(tData);
    }

    /**
     * Get a list of all the tokens managed by the TokenManager
     */
    public List getTokens(){
        List res;

        res = new ArrayList();
        for(Iterator i=this.tokens.iterator(); i.hasNext(); ){
            TokenData tData = (TokenData)i.next();

            res.add(tData);
        }
        return res;
    }

    /**
     * Get a token with the specified string
     *
     * @param token The token to look for
     * @return The TokenData object that matches the token
     */
    public TokenData getToken(String token)
        throws TokenNotFoundException
    {
        // this.outdateList();
        for(Iterator i=this.tokens.iterator(); i.hasNext(); ){
            TokenData tData = (TokenData)i.next();

            if(tData.getToken().equals(token)){
                return (TokenData)tData.clone();
            }
        }
        
        throw new TokenNotFoundException("Token not found");
    }

    public void store()
        throws IOException 
    {
        OutputStream os;
        Properties props;

        // this.outdateList();
        props = this.encodeTokens(this.tokens);
        os    = this.storer.getTokenStoreStream();
        try {
            props.store(os, "TokenManager authentication tokens");
            os.flush();
        } finally {
            try {os.close();} catch(IOException exc){}
        }
    }

    public void load(InputStream is)
        throws IOException
    {
        Properties props;

        props = new Properties();
        props.load(is);
        this.tokens = this.decodeTokens(props);
        // this.outdateList();
    }

    /**
     * Iterate through all the tokens, deleting ones which are pending
     * and exceed the timeout.
     */
    private void outdateList(){
        for(Iterator i=this.tokens.iterator(); i.hasNext(); ){
            TokenData tData = (TokenData)i.next();

            if(tData.isLocked()){
                continue;
            }

            if(this.tokenIsOutdated(tData.getCreateTime())){
                i.remove();
            }
        }
    }

    private boolean tokenIsOutdated(long createTime){
        return (createTime + this.timeout) < System.currentTimeMillis();
    }

    private Set decodeTokens(Properties props)
        throws IOException
    {
        Set res;
        int numTokens;

        try {
            numTokens = Integer.parseInt(props.getProperty(PROP_NUMTOKENS));
        } catch(NumberFormatException exc){
            throw new IOException("Unable to locate " + PROP_NUMTOKENS + 
                                  " in properties");
        }

        res = TokenManager.createSet();
        for(int i=0; i<numTokens; i++){
            StringTokenizer st;
            String val, token, sLocked;
            long createTime;
            boolean locked;

            if((val = props.getProperty(PROP_PREFIX + i)) == null){
                throw new IOException("Unable to find " + PROP_PREFIX + i);
            }

            st  = new StringTokenizer(val, ":");
            if(st.countTokens() != 3){
                throw new IOException("Malformed token entry: " + val);
            }

            token = st.nextToken();

            try {
                createTime = Long.parseLong(st.nextToken());
            } catch(NumberFormatException exc){
                throw new IOException("Unable to parse token length(" + i+")");
            }

            sLocked  = st.nextToken();
            if(sLocked.equals(TOKEN_PENDING)){
                locked = false;
            } else if(sLocked.equals(TOKEN_LOCKED)){
                locked = true;
            } else{
                throw new IOException("Unable to parse token locked(" + i+")");
            }               

            res.add(new TokenData(token, createTime, locked));
        }
        return res;
    }

    private Properties encodeTokens(Set tokens){
        Properties res;
        int idx;

        res = new Properties();
        idx = 0;
        for(Iterator i=tokens.iterator(); i.hasNext(); idx++){
            TokenData tData = (TokenData)i.next();
            String val, token;

            token = tData.getToken();
            if(token.indexOf(":") != -1){
                throw new IllegalArgumentException("Unable to encode tokens "+
                                                   "containing the char ':': "+
                                                   token);
            }

            val = tData.getToken() + ":" +
                tData.getCreateTime() + ":" +
                (tData.isLocked() ? TOKEN_LOCKED : TOKEN_PENDING);
            res.setProperty(PROP_PREFIX + idx, val);
        }
        res.setProperty(PROP_NUMTOKENS, Integer.toString(idx));
        return res;
    }
}
