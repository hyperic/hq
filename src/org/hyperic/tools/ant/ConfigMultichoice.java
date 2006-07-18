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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

public class ConfigMultichoice extends Task {
    
    private static final String LINE_SEP =
        System.getProperty("line.separator");

    private String itsTitle;
    private List itsOptions = new ArrayList();
    
    public ConfigMultichoice () {}
    
    public void setTitle ( String t ) {
        itsTitle = t;
    }
    
    public CMOption createOption () {
        CMOption opt = new CMOption();
        itsOptions.add(opt);
        return opt;
    }
    
    public void execute () throws BuildException {
        
        validateAttributes();
        
        Project project = getProject();
        InputHandler in = project.getInputHandler();
        CMInputRequest inputRequest = new CMInputRequest(generatePrompt(), 
                                                         itsOptions.size());
        in.handleInput(inputRequest);
        List choices = inputRequest.getChoicesMade();
        Integer idx;
        CMOption opt;
        for ( int i=0; i<choices.size(); i++ ) {
            idx = (Integer) choices.get(i);
            opt = (CMOption) itsOptions.get(idx.intValue()-1);
            opt.perform();
        }
    }
    
    private String generatePrompt () {
        
        StringBuffer prompt = new StringBuffer();
        CMOption opt;
        
        prompt.append(itsTitle);
        for ( int i=0; i<itsOptions.size(); i++ ) {
            opt = (CMOption) itsOptions.get(i);
            prompt.append(LINE_SEP);
            prompt.append(i+1).append(": ").append(opt.getName());
        }
        prompt.append(LINE_SEP);
        prompt.append("You may enter multiple choices, separated by commas.");

        return prompt.toString();
    }
    
    public void validateAttributes () throws BuildException {
        if ( itsTitle == null ) {
            itsTitle = "Choose: ";
        }
        if ( itsOptions.size() == 0 ) {
            throw new BuildException("ConfigMultichoice: No <option> "
                                     + "sub-elements specified.");
        }
    }
    
    public class CMOption extends Task implements TaskContainer {
        
        private String name;
        private List itsTasks = new ArrayList();
        
        public CMOption () {}
        
        public void setName (String name) { this.name = name; }
        public String getName () { return this.name; }
        
        public void addTask ( Task t ) {
            itsTasks.add(t);
        }
        
        public void execute () throws BuildException {
            int size = itsTasks.size();
            for ( int i=0; i<size; i++ ) {
                Task aTask = ((Task) itsTasks.get(i));
                aTask.perform();
            }
        }
    }
    
    public class CMInputRequest extends InputRequest {
        
        private int maxChoice;
        private List choices = new ArrayList();
        
        public CMInputRequest ( String prompt, int maxChoice ) {
            super(prompt);
            this.maxChoice = maxChoice;
        }
        
        public boolean isInputValid () {
            String input = getInput();
            if (input == null) {
                return false;
            }
            StringTokenizer st = new StringTokenizer(input, ", ");
            if ( !st.hasMoreTokens() ) return false;
            choices.clear();
            int choice;
            while ( st.hasMoreTokens() ) {
                try {
                    choice = Integer.parseInt(st.nextToken());
                } catch ( NumberFormatException nfe ) {
                    return false;
                }
                if ( choice < 1 || choice > maxChoice ) return false;
                choices.add(new Integer(choice));
            }
            
            return ( choices.size() > 0 );
        }
        
        public List getChoicesMade () { return choices; }
    }
}
