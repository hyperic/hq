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

package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParseResult;


public class ClientShellParseUtil {
    public static String KEY_RESOURCE       = "Resource";
    public static String KEY_RESOURCETYPE   = "ResourceType";

    public static String BLOCK_RESOURCE     = "ResourceBlock";
    public static String BLOCK_RESOURCETYPE = "ResourceTypeBlock";

    /**
     * If the the parseResult is the same as the passed blockName, 
     * look into the blockVals, and pull out the AppdefEntityID which
     * was specified, and throw it into the root ParseResult with
     * a key of KEY_RESOURCE
     */
    public static void bubbleUpResourceBlock(String blockName,
                                             ParseResult parseResult,
                                             FormatParser[] blockVals)
    {
        AppdefEntityTagParser entParser;

        if(!blockName.equals(parseResult.getBlockName()))
            return;

        entParser = (AppdefEntityTagParser)blockVals[1];
            
        parseResult.getRoot().setValue(KEY_RESOURCE, entParser.getID());
    }

    public static void bubbleUpResourceBlock(ParseResult parseResult, 
                                             FormatParser[] blockVals)
    {
        bubbleUpResourceBlock(BLOCK_RESOURCE, parseResult, blockVals);
    }

    public static void bubbleUpResourceTypeBlock(String blockName,
                                                 ParseResult parseResult,
                                                 FormatParser[] blockVals)
    {
        AppdefTypeTagParser entParser;

        if(!blockName.equals(parseResult.getBlockName()))
            return;

        entParser = (AppdefTypeTagParser)blockVals[1];
            
        parseResult.getRoot().setValue(KEY_RESOURCETYPE, 
                                       entParser.getValue());
    }

    public static void bubbleUpResourceTypeBlock(ParseResult parseResult, 
                                                 FormatParser[] blockVals)
    {
        bubbleUpResourceTypeBlock(BLOCK_RESOURCETYPE, parseResult, blockVals);
    }

    /**
     * Make a resource block based on valid resource types.
     *
     * Returns strings like:
     *     "$ResourceBlock <-platform #PlatformTag> <-server #ServerTag>"
     *
     * @param resourceTypes array of ClientShell_resource.PARAM_* 
     * @param blockName     the name that the block should have
     *
     * @return A ParamParser style string which is the contents
     *         of the container __WITHOUT__ the enclosing '<', etc.
     */
    public static String makeResourceBlock(int[] resourceTypes,
                                           String blockName)
    {
        return makeResourceBlock(resourceTypes, blockName, false);
    }

    public static String makeResourceBlock(int[] resourceTypes){
        return makeResourceBlock(resourceTypes, BLOCK_RESOURCE);
    }

    /**
     * Make a resource block based on valid resource type types
     *
     * Returns strings like:
     *     "$ResourceTypeBlock <-server #ServerTypeTag> 
     *      <-service #ServiceTypeTag>"
     *
     * @param resourceTypes array of ClientShell_resource.PARAM_* 
     * @param blockName     the name that the block should have
     *
     * @return A ParamParser style string which is the contents
     *         of the container __WITHOUT__ the enclosing '<', etc.
     */
    public static String makeResourceTypeBlock(int[] resourceTypes,
                                               String blockName)
    {
        return makeResourceBlock(resourceTypes, blockName, true);
    }

    public static String makeResourceTypeBlock(int[] resourceTypes){
        return makeResourceTypeBlock(resourceTypes, BLOCK_RESOURCETYPE);
    }

    private static String makeResourceBlock(int[] resourceTypes,
                                            String blockName,
                                            boolean isEntityType)
    {
        StringBuffer res = new StringBuffer();

        res.append("$");
        res.append(blockName);
        for(int i=0; i<resourceTypes.length; i++){
            int resType = resourceTypes[i];

            res.append(" <");
            res.append(ClientShell_resource.convertParamToString(resType));
            res.append(" ");
            if(isEntityType){
                res.append(entityTypeToTag(resType));
            } else {
                res.append(entityToTag(resType));
            }
            res.append("> ");
            
            if(i != resourceTypes.length - 1){
                res.append(" | ");
            }
        }
        
        return res.toString();
    }

    private static String entityToTag(int entityType){
        switch(entityType){
        case ClientShell_resource.PARAM_APP:
            return "#ApplicationTag";
        case ClientShell_resource.PARAM_GROUP:
            return "#GroupTag";
        case ClientShell_resource.PARAM_PLATFORM:
            return "#PlatformTag";
        case ClientShell_resource.PARAM_SERVER:
            return "#ServerTag";
        case ClientShell_resource.PARAM_SERVICE:
            return "#ServiceTag";
        }

        throw new IllegalArgumentException("Entity type has no tag");
    }

    private static String entityTypeToTag(int entityType){
        switch(entityType){
        case ClientShell_resource.PARAM_PLATFORM:
            return "#PlatformTypeTag";
        case ClientShell_resource.PARAM_SERVER:
            return "#ServerTypeTag";
        case ClientShell_resource.PARAM_SERVICE:
            return "#ServiceTypeTag";
        }

        throw new IllegalArgumentException("Entity type has no tag");
    }
}
