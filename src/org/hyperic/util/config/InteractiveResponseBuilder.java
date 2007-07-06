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

package org.hyperic.util.config;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class InteractiveResponseBuilder implements ResponseBuilder {

    private InteractiveResponseBuilder_IOHandler inout;

    public InteractiveResponseBuilder(InteractiveResponseBuilder_IOHandler io)
    {
        this.inout = io;
    }
    
    public String handleInput(String s) throws IOException, EOFException{
        return inout.handleInput(s);
    }
    
    public String handleHiddenInput(String s) throws IOException, EOFException{
        return inout.handleHiddenInput(s);
    }
    
    public ConfigResponse processConfigSchema(ConfigSchema schema)
        throws EncodingException, IOException, InvalidOptionException, 
               EarlyExitException
    {
        return this.processConfigSchema(schema, new ConfigResponse());
    }
    
    /**
     * Process a configuration schema.
     * @param schema The schema to process.
     * @return The filled-out ConfigResponse.
     */
    public ConfigResponse processConfigSchema(ConfigSchema schema,
                                              ConfigResponse defaults) 
        throws IOException, InvalidOptionException, EarlyExitException
    {
        List options = schema.getOptions();
        int i, nOptions = options.size();
        ConfigResponse res;
        Set defaultKeys;

        defaultKeys = defaults.getKeys();

        res = new ConfigResponse(schema);
        i = 0;
        while(i < nOptions){
            ConfigOption opt = (ConfigOption)options.get(i);
            String val, inputStr, def;
            boolean isSecret;

            def = defaultKeys.contains(opt.getName()) ?
                defaults.getValue(opt.getName()) : opt.getDefault();

            /* If the previous option used is now bogus, use the options
               real default value */
            if(def != null){
                try {
                    opt.checkOptionIsValid(def);
                } catch(InvalidOptionValueException exc){
                    def = opt.getDefault();
                }
            }

            isSecret = false;
            if(opt instanceof StringConfigOption){
                if (((StringConfigOption)opt).isHidden()) {
                    try {
                        res.setValue(opt.getName(), opt.getDefault());
                    } catch(InvalidOptionException exc){
                        // should never happen
                        sendToErrStream("Error setting hidden value: " + exc);
                        throw new IllegalStateException("Error setting hidden "
                                                        +" value, cannot "
                                                        + "continue: " + exc);
                    } catch(InvalidOptionValueException exc){
                        // should never happen
                        sendToErrStream("Error setting hidden value: " + exc);
                        throw new IllegalStateException("Error setting hidden "
                                                        +" value, cannot "
                                                        + "continue: " + exc);
                    }
                    i++;
                    continue;
                }
                isSecret = ((StringConfigOption)opt).isSecret();
            }

            if(isSecret){
                inputStr = getInputString(opt, def == null ? null : "*hidden*");
                val = handleHiddenInput(inputStr + ": ");
            } else {
                inputStr = getInputString(opt, def);
                val = handleInput(inputStr + ": ");
            }

            // Normalize backend input results
            if(val == null || "".equals(val)){
                if (def == null) {
                    if (opt.isOptional())
                        i++;

                    // If there is no default, ask the question again
                    continue;
                }
                else {
                    val = def;
                }
            } else {
                /* If they entered an actual value, and it was a secret
                   input, ask for it again, just to make sure */
                if (isSecret) {
                    String verifyVal;

                    verifyVal = handleHiddenInput("(again): ");
                    if(!verifyVal.equals(val)){
                        sendToErrStream("Values do not match");
                        continue;
                    }
                }
                
                val = val.trim();
                if(opt instanceof EnumerationConfigOption){
                    int index = -1;
                    List values;

                    try {
                        index = Integer.parseInt(val) - 1;
                    } catch(NumberFormatException exc){
                        sendToErrStream("Value must be an integer");
                        continue;
                    }
                    values = ((EnumerationConfigOption) opt).getValues();
                    if(index < 0 || index >= values.size()){
                        sendToErrStream("Value not in range");
                        continue;
                    }
                    val = values.get(index).toString();
                }
                
                if (val.equals(opt.getConfirm())) {
                sendToErrStream(val + " compare with " + opt.getConfirm() + " "
                                + val.equals(opt.getConfirm()));
                
                    // Double check with user
                    YesNoConfigOption confirmOpt =
                        new YesNoConfigOption("confirmation",
                                              "Are you sure (" + val + ")?",
                                              YesNoConfigOption.YES);
                    String confirm =
                        handleInput(getInputString(confirmOpt,
                                                   confirmOpt.getDefault()) +
                                                   ": ");
                    
                    if (!"1".equals(confirm))
                        continue;
                }
            }
            
            try {
                res.setValue(opt.getName(), val);
            } catch (EarlyExitException e) {
                throw e;
            } catch(InvalidOptionException exc){
                // Give the user a chance to reenter something valid
                sendToErrStream("Invalid option, '" + opt.getName() + "'");
                continue;
            } catch(InvalidOptionValueException exc){
                sendToErrStream(exc.getMessage());
                continue;
            }
            i++;
        }
        return res;
    }

    public void sendToErrStream ( String msg ) {
        inout.errOutput(msg);
    }

    /**
     * Assemble the string that will ask the user for an option.
     * @param opt The ConfigOption to generate an input string for.
     * @return The String to use when asking the user for the 
     * value of an option. 
     */
    private String getInputString ( ConfigOption opt, String defaultValue ) {

        String inputStr, desc;

        inputStr = "";

        if(inout.isDeveloper())
            inputStr += "("+opt.getName()+") ";

        desc = opt.getDescription();

        // Treat these special, because we want to display the list
        // of valid options to the user.  
        if ( opt instanceof EnumerationConfigOption ) {
            inputStr += "Choices:";
            List enumValues = ((EnumerationConfigOption) opt).getValues();
            String enumValue;
            int defaultIndex = -1;
            for ( int i=0; i<enumValues.size(); i++ ) {
                enumValue = enumValues.get(i).toString();
                inputStr += "\n\t" + String.valueOf(i+1) + ": " + enumValue;
                if ( enumValue.equals(defaultValue) ) defaultIndex = i;
            }
            if ( defaultIndex != -1 ) {
                inputStr += "\n" + desc + " [default '" 
                    + String.valueOf(defaultIndex+1) + "']";
            } else {
                inputStr += "\n" + desc;
            }
        } else {
            inputStr += desc;
            if( defaultValue != null)
                inputStr += " [default '" + defaultValue + "']";
        }
        return inputStr;
    }
}
