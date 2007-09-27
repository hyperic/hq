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

package org.hyperic.util;

import java.util.StringTokenizer;

/**
 * A class which functions similarly to a StringBuffer, except that
 * one can call pushIndent/popIndent to change the indentation level.
 * When the indenter encounters a newline, it automatically positions
 * the cursor at the next line, indented the proper amount.
 */
public class TextIndenter {
    public static final int DEFAULT_INDENT = 4;

    private StringBuffer buf;
    private int          indentAmount;
    private int          curIndent;
    private boolean      lastWasNewline;

    /**
     * Create a new TextIndenter with the specified # of indentation spaces.
     * 
     * @param indentAmount Number of spaces to indent
     */
    public TextIndenter(int indentAmount){
        this.init(indentAmount);
    }

    /**
     * Create a new TextIndenter with the default # of indentation spaces.
     */
    public TextIndenter(){
        this.init(DEFAULT_INDENT);
    }

    private void init(int indentAmount){
        this.buf            = new StringBuffer();
        this.indentAmount   = indentAmount;
        this.curIndent      = 0;
        this.lastWasNewline = false;
    }

    public void append(String str){
        StringTokenizer tokenizer;
        
        tokenizer = new StringTokenizer(str, "\n", true);
        while(tokenizer.hasMoreTokens()){
            String val = tokenizer.nextToken();

            if(lastWasNewline == true){
                this.buf.append(StringUtil.repeatChars(' ', this.curIndent));
            }

            this.buf.append(val);
            if(val.equals("\n")){
                lastWasNewline = true;
            } else {
                lastWasNewline = false;
            }
        }
    }

    public void pushIndent(int amount){
        this.curIndent += amount;
    }

    public void pushIndent(){
        pushIndent(this.indentAmount);
    }

    public void popIndent(int amount){
        if(this.curIndent - amount < 0){
            throw new IllegalStateException("Underflow in indentation " +
                                            "poppage!");
        }
        this.curIndent -= amount;
    }

    public void popIndent(){
        popIndent(this.indentAmount);
    }

    public String toString(){
        return this.buf.toString();
    }

    public static void main(String[] args){
        TextIndenter t = new TextIndenter(3);

        t.append("Header\n");
        t.pushIndent();
        t.append("Test 1\n");
        t.pushIndent();
        t.pushIndent();
        t.append("Super 1\n\nSuper 2\n");
        t.popIndent();
        t.popIndent();
        t.popIndent();
        t.append("Header 2\n");
        t.pushIndent();
        t.append("Test 2\n");
        t.pushIndent();
        t.append("Sub 1\n");
        System.out.println(t.toString());
    }
}
