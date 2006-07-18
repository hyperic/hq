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

package org.hyperic.util.paramParser;

import org.hyperic.util.TextIndenter;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class which parses parameter strings based on a parameter 
 * format.  
 *
 * The format is a textual description with a few specific rules:
 *
 * - Literal strings are required literal strings.  
 *    e.g. '-value' means the string '-value' must exist
 *
 * - '|' indicates that a <> container should accept the first
 *       value which matches any of the values.  (I.e. <foo | bar>)
 * 
 * - '#' indicates a class to be called to handle the associated value
 *
 * - Atoms must be surrounded by either '<>' or '[]' indicating
 *   the atom is required or optional, respectively.
 *   If the atom contains a '$String' at the beginning, and if a BlockHandler
 *   is registered, it will be called after that block is processed.
 * 
 * Example:
 *
 *   <$setVal -value #Integer> [<-from #PastDate> <-to #FutureDate>
 *                             [-interval #com.mything.Interval]]
 */

public class ParamParser 
    implements BlockHandler
{
    private ContainerAtom   format;
    private ParserRetriever retriever;
    private BlockHandler    blockHandler;

    public ParamParser(String formatText){
        this.init(new BasicRetriever(), formatText, this);
    }

    /**
     * Setup the ParamParser with the specified format and default
     * parser retriever.
     *
     * @param formatText   Format to use when parsing parameters
     * @param blockHandler Handler to use to process data blocks
     * 
     * @throws FormatException indicating there is an error in the
     *                         formatText
     */
    public ParamParser(String formatText, BlockHandler blockHandler){
        this.init(new BasicRetriever(), formatText, blockHandler);
    }

    /**
     * Setup the ParamParser with the specified format and the
     * user specified parser retriever
     *
     * @param formatText   Format to use when parsing parameters
     * @param retriever    Object to call when attempting to instantiate
     *                     ParserFormat objects to fulfill #class parameters
     * @param blockHandler Handler to use to process data blocks
     * 
     * @throws FormatException indicating there is an error in the
     *                         formatText
     */
    public ParamParser(String formatText, ParserRetriever retriever,
                       BlockHandler blockHandler)
    {
        this.init(retriever, formatText, blockHandler);
    }

    private void init(ParserRetriever retriever, String formatText,
                      BlockHandler blockHandler)
    {
        char[] formatChars;

        this.retriever    = retriever;
        formatChars       = formatText.toCharArray();
        this.format       = this.parseFormat(formatChars, 0, 
                                             formatChars.length);
        this.blockHandler = blockHandler;
        this.format.setRequired(true);
    }

    /**
     * Parse a parameter string.  The validation of the parameter string
     * will be performed, as well as calling of any #class based 
     * format parsers.  
     *
     * @param params Parameters to validate against the format
     *
     * @return An array of FormatParser[] objects which represent the
     *          output from the parsing process.  StringParser objects
     *          are returned for every literal element in the parse string,
     *          else the parser types are defined by the #class elements
     *          in the format string
     *
     * @throws ParseException if there was an error parsing the parameters
     */
    public ParseResult parseParams(String[] params)
        throws ParseException
    {
        ParseResult res;
        List processedElems;

        processedElems = new ArrayList();

        res = new ParseResult("**MAIN**");
        if(this.parseAtom(res, processedElems, this.format, params, 
                          0) != params.length)
        {
            throw new ParseException("All parameters not converted");
        }

        return res;
    }

    /**
     * Attempt to satisfy the conditions of an atom with the given
     * parameters. 
     *
     * @param res      ParseResult representing this block
     * @param procList Location to put resultant argument sent
     * @param atom     Atom to satisfy
     * @param params   Params to use to satisfy the atom
     * @param begIdx   Offset into 'params' to begin parsing
     *
     * @return The # of parameters eaten from 'params'
     */
    private int parseAtom(ParseResult res, List procList, FormatAtom atom, 
                          String[] params, int begIdx)
        throws ParseException
    {
        boolean isRequired = atom.isRequired();

        if(atom instanceof ContainerAtom &&
           ((ContainerAtom)atom).isORed() == false)
        {
            ContainerAtom cAtom = (ContainerAtom)atom;
            ArrayList addList = new ArrayList();
            ParseResult contResult;
            int curIdx;

            contResult = new ParseResult(cAtom.getBlockName());
            res.addChild(contResult);
            curIdx = begIdx;
            for(Iterator i=cAtom.getSubAtoms().iterator(); i.hasNext(); ){
                FormatAtom subAtom = (FormatAtom)i.next();
                ArrayList subList = new ArrayList();
                int nEaten;

                try {
                    nEaten = this.parseAtom(contResult, subList, subAtom, 
                                            params, curIdx);
                } catch(ParseException exc){
                    if(isRequired){
                        res.removeChild(contResult);
                        throw exc;
                    } else {
                        res.removeChild(contResult);
                        return 0;
                    }
                }

                curIdx += nEaten;
                addList.addAll(subList);
            }

            this.callBlockHandler(contResult, addList);
            procList.addAll(addList);
            return curIdx - begIdx;
        } else if(atom instanceof ContainerAtom &&
                  ((ContainerAtom)atom).isORed() == true)
        {
            ContainerAtom cAtom = (ContainerAtom)atom;
            ParseException lastException = null;
            ParseResult contResult;

            contResult = new ParseResult(cAtom.getBlockName());
            res.addChild(contResult);
            for(Iterator i=cAtom.getSubAtoms().iterator(); i.hasNext(); ){
                FormatAtom subAtom = (FormatAtom)i.next();
                ArrayList subList = new ArrayList();
                int nEaten;

                try {
                    nEaten = this.parseAtom(contResult, subList, subAtom, 
                                            params, begIdx);
                } catch(CriticalParseException exc){
                    throw exc;
                } catch(ParseException exc){
                    lastException = exc;
                    continue;
                }

                this.callBlockHandler(contResult, subList);
                procList.addAll(subList);
                return nEaten;
            }

            res.removeChild(contResult);
            if(isRequired)
                throw new ParseException("| condition not met", lastException);
            else
                return 0;
        } else if(atom instanceof LiteralAtom){
            LiteralAtom lAtom = (LiteralAtom)atom;
            String literal;

            literal = lAtom.getLiteral();
            if(begIdx >= params.length){
                throw new ParseException("No params left to handle literal '" +
                                         literal + "'");
            }

            if(params[begIdx].equals(literal)){
                procList.add(new StringParser(literal));
                return 1;
            } else if(isRequired){
                throw new ParseException("Required parameter '" + 
                                         literal + "' not provided");
            } else {
                return 0;
            }
        } else if(atom instanceof ClassAtom){
            ClassAtom cAtom = (ClassAtom)atom;
            FormatParser parser;

            if(begIdx >= params.length){
                throw new ParseException("No params left to handle class '" +
                                      cAtom.getParser().getClass().getName() + 
                                      "'");
            }

            parser = cAtom.getParser();
            parser.parseValue(params[begIdx]);
            procList.add(parser);
            return 1;
        } else {
            throw new IllegalStateException("Unhandled atom type");
        }
    }

    private void dumpAtom(TextIndenter tInd, FormatAtom atom, String txt){
        boolean required = atom.isRequired();

        tInd.append(required ? "<" : "[");
        tInd.append(txt);
        tInd.append(required ? ">\n" : "]\n");
    }

    private void dumpFormat(TextIndenter tInd, ContainerAtom atom){
        String containerStr;

        containerStr = atom.getBlockName();
        if(containerStr != null) 
            containerStr = "Container:" + containerStr;
        else
            containerStr = "Container";

        this.dumpAtom(tInd, atom, containerStr);
        
        tInd.pushIndent();

        for(Iterator i=atom.getSubAtoms().iterator(); i.hasNext(); ){
            FormatAtom subAtom = (FormatAtom)i.next();
            
            if(subAtom instanceof ContainerAtom){
                dumpFormat(tInd, (ContainerAtom)subAtom);
            } else if(subAtom instanceof ClassAtom){
                this.dumpAtom(tInd, subAtom, "class " +
                       ((ClassAtom)subAtom).getParser().getClass().getName());
            } else if(subAtom instanceof LiteralAtom){
                this.dumpAtom(tInd, subAtom, "literal " +
                              ((LiteralAtom)subAtom).getLiteral());
            }
        }

        tInd.popIndent();
    }

    /**
     * Dump the internal format of the textual format which was passed
     * when creating the ParamParser object
     *
     * @param os Stream to print the representation to
     */
    public void dumpFormat(PrintStream os){
        TextIndenter tInd;

        tInd = new TextIndenter();
        this.dumpFormat(tInd, this.format);
        os.print(tInd.toString());
    }

    /**
     * Returns the idx of last character which is part of the class name.
     */
    private int findClassNameEnd(char[] chars, int beginIdx, int endIdx){
        boolean doStart = true;

        for(int i=beginIdx; i<endIdx; i++){
            if(doStart){
                if(!Character.isJavaIdentifierStart(chars[i])){
                    throw new FormatException("Invalid class identifier at " +
                                              "index " + i);
                }
                doStart = false;
                continue;
            }

            if(!(Character.isJavaIdentifierPart(chars[i]) ||
                 chars[i] == '.'))
            {
                return i - 1;
            }
        }

        return endIdx - 1;
    }

    /**
     * Returns the idx of last character which is not whitespace
     */
    private int findLiteralEnd(char[] chars, int beginIdx, int endIdx){
        for(int i=beginIdx; i<endIdx; i++){
            if(Character.isWhitespace(chars[i])){
                return i - 1;
            }
        }

        return endIdx - 1;
    }

    private int findMatchingEnd(char[] chars, char startToken, char endToken,
                                int beginIdx, int endIdx)
    {
        int depth = 0;

        for(int i=beginIdx; i<endIdx; i++){
            if(chars[i] == startToken)
                depth++;
            else if(chars[i] == endToken)
                depth--;

            if(depth == 0)
                return i;
        }
        return -1;
    }

    private int findBlockNameEnd(char[] chars, int beginIdx, int endIdx){
        for(int i=beginIdx; i<endIdx; i++){
            if(!Character.isLetterOrDigit(chars[i])){
                return i - 1;
            }
        }
        
        return endIdx - 1;
    }

    /**
     * Parse the characters in 'formatChars' from start to end - 1, and
     * return the Atom which the characters represent.
     */
    private ContainerAtom parseFormat(char[] formatChars, int start, int end){
        ContainerAtom res;
        int i;

        res = new ContainerAtom();
        res.setBlockName("**MAIN**");
        i = start;
        while(i < end){
            int atomEnd;

            if(formatChars[i] == '<' || formatChars[i] == '['){
                ContainerAtom newAtom;
                char begChar, endChar;
                String blockName;

                begChar = formatChars[i];
                endChar = begChar == '<' ? '>' : ']';

                atomEnd = this.findMatchingEnd(formatChars, begChar, endChar, 
                                               i, end);
                if(atomEnd == -1){
                    throw new FormatException("Could not find closing " +
                                              endChar + " to match " + 
                                              begChar + " at index " + i);
                }

                // Figure out the blockname if it has one
                blockName = null;
                if(i + 1 < end && formatChars[i + 1] == '$'){
                    int blockNameEnd;

                    i++;
                    blockNameEnd = this.findBlockNameEnd(formatChars, i + 1,
                                                         atomEnd);
                    if(blockNameEnd == -1){
                        throw new FormatException("Could not find end of " +
                                                  "block name at index " + i);
                    }

                    blockName = new String(formatChars, i + 1,
                                           blockNameEnd - i);
                    i = blockNameEnd + 1;
                }

                newAtom = parseFormat(formatChars, i + 1, atomEnd);
                newAtom.setRequired(begChar == '<');
                if(blockName == null)
                    blockName = "Block#" + System.identityHashCode(newAtom);

                newAtom.setBlockName(blockName);
                res.addSubAtom(newAtom);
                i = atomEnd + 1;
            } else if(Character.isWhitespace(formatChars[i])){
                // Skipola
                i++;
            } else if(formatChars[i] == '#'){
                ClassAtom newAtom;
                String className;

                atomEnd = this.findClassNameEnd(formatChars, i + 1, end);

                className = new String(formatChars, i + 1, atomEnd - i);
                newAtom = new ClassAtom(this.retriever.getParser(className));
                newAtom.setRequired(true);
                res.addSubAtom(newAtom);
                i = atomEnd + 1;
            } else {
                LiteralAtom newAtom;
                String literal;

                atomEnd = this.findLiteralEnd(formatChars, i, end) + 1;

                literal = new String(formatChars, i, atomEnd - i);
                newAtom = new LiteralAtom(literal);
                newAtom.setRequired(true);
                res.addSubAtom(newAtom);
                i = atomEnd + 1;
            }
        }

        return res;
    }

    private void callBlockHandler(ParseResult result, List blockData){
        this.blockHandler.handleBlock(result, 
                      (FormatParser[])blockData.toArray(new FormatParser[0]));
    }

    public void handleBlock(ParseResult resultBlock, FormatParser[] blockData){
        System.out.println("Processing block '" + 
                           resultBlock.getBlockName() + "'");
        if(resultBlock.getBlockName().equals("TimeRange")){
            resultBlock.getRoot().setValue("Foo", "DoinTimeRange");
        }

        for(int i=0; i<blockData.length; i++){
            System.out.println("    " + blockData[i]);
        }
    }

    public static void main(String[] args) 
        throws Exception 
    {
        String format = "<<-foo #Integer> | <-bar #String>> " +
            "[$TimeRange <-from #PastDate> <-to #FutureDate> " +
            "[-interval #Integer]]";
        ParamParser p = new ParamParser(format);

        p.dumpFormat(System.out);
        System.out.print(p.parseParams(args).toString());
    }
}
