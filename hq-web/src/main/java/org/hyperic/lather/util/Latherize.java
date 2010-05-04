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

package org.hyperic.lather.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.WrapDynaBean;

/**
 * This utility, takes in a class which follows the JavaBean spec, 
 * and writes out .java files which will be LatherValue representations.
 */
public class Latherize {
    public class LatherizeException extends Exception {
        public LatherizeException(String s){
            super(s);
        }
    }

    private boolean isLatherStyleProp(DynaProperty prop){
        if(prop.getName().equals("class") ||
           (prop.isIndexed() && prop.getType() != byte[].class) ||
           prop.isMapped())
        {
            return false;
        } 
        return true;
    }

    private String makePropVar(DynaProperty prop){
        return "PROP_" + prop.getName().toUpperCase();
    }

    private String capName(String name){
        return name.substring(0, 1).toUpperCase() + 
            name.substring(1);
    }

    private String makeGetter(DynaProperty prop){
        return "get" + this.capName(prop.getName());
    }

    private String makeSetter(DynaProperty prop){
        return "set" + this.capName(prop.getName());
    }

    public void latherize(boolean xDocletStyle, String outPackage, 
                          Class fClass, OutputStream os)
        throws LatherizeException, IOException
    {
        final String INDENT = "        ";
        PrintWriter pWriter;
        DynaProperty[] dProps;
        WrapDynaBean dBean;
        DynaClass dClass;
        Object beanInstance;
        String className, lClassName;

        try {
            beanInstance = fClass.newInstance();
        } catch(IllegalAccessException exc){
            throw new LatherizeException("Illegal access trying to create " +
                                         "new instance");
        } catch(InstantiationException exc){
            throw new LatherizeException("Unable to instantiate: " +
                                         exc.getMessage());
        }

        dBean   = new WrapDynaBean(beanInstance);
        dClass  = dBean.getDynaClass();
        dProps  = dClass.getDynaProperties();

        pWriter = new PrintWriter(os);

        className  = fClass.getName();
        className  = className.substring(className.lastIndexOf(".") + 1);
        lClassName = "Lather" + className;

        pWriter.println("package " + outPackage + ";");
        pWriter.println();
        pWriter.println("import " + LatherValue.class.getName() + ";");
        pWriter.println("import " + LatherRemoteException.class.getName() + 
                        ";");
        pWriter.println("import " + LatherKeyNotFoundException.class.getName()+
                        ";");
        pWriter.println("import " + fClass.getName() + ";");
        pWriter.println();
        pWriter.println("public class " + lClassName);
        pWriter.println("    extends LatherValue");
        pWriter.println("{");
        for(int i=0; i<dProps.length; i++){
            pWriter.print("    ");
            if(!this.isLatherStyleProp(dProps[i])){
                pWriter.print("// ");
            }
               
            pWriter.println("private static final String " +
                            this.makePropVar(dProps[i]) + " = \"" + 
                            dProps[i].getName() + "\";");
        }

        pWriter.println();
        pWriter.println("    public " + lClassName + "(){");
        pWriter.println("        super();");
        pWriter.println("    }");
        pWriter.println();
        pWriter.println("    public " + lClassName + "(" + className + " v){");
        pWriter.println("        super();");
        pWriter.println();
        for(int i=0; i<dProps.length; i++){
            String propVar = this.makePropVar(dProps[i]);
            String getter = "v." + this.makeGetter(dProps[i]) + "()";

            if(!this.isLatherStyleProp(dProps[i])){
                continue;
            }

            if(xDocletStyle){
                String lName;

                lName = dProps[i].getName();
                lName = lName.substring(0, 1).toLowerCase() + 
                    lName.substring(1);
                pWriter.println(INDENT + "if(v." + lName + 
                                "HasBeenSet()){");
                pWriter.print("    ");
            }

            if(dProps[i].getType().equals(String.class)){
                pWriter.println(INDENT + "this.setStringValue(" + propVar +
                                ", " + getter + ");");
            } else if(dProps[i].getType().equals(Integer.TYPE)){
                pWriter.println(INDENT + "this.setIntValue(" + propVar +
                                ", " + getter + ");");
            } else if(dProps[i].getType().equals(Integer.class)){
                pWriter.println(INDENT + "this.setIntValue(" + propVar +
                                ", " + getter + ".intValue());");
            } else if(dProps[i].getType().equals(Long.TYPE)){
                pWriter.println(INDENT + "this.setDoubleValue(" + propVar +
                                ", (double)" + getter + ");");
            } else if(dProps[i].getType().equals(Long.class)){
                pWriter.println(INDENT + "this.setDoubleValue(" + propVar +
                                ", (double)" + getter + ".longValue());");
            } else if(dProps[i].getType().equals(Boolean.TYPE)){
                pWriter.println(INDENT + "this.setIntValue(" + propVar +
                                ", " + getter + " ? 1 : 0);");
            } else if(dProps[i].getType().equals(Boolean.class)){
                pWriter.println(INDENT + "this.setIntValue(" + propVar +
                                ", " + getter + ".booleanValue() ? 1 : 0);");
            } else if(dProps[i].getType().equals(Double.TYPE)){
                pWriter.println(INDENT + "this.setDoubleValue(" + propVar +
                                ", " + getter + ");");
            } else if(dProps[i].getType().equals(Double.class)){
                pWriter.println(INDENT + "this.setDoubleValue(" + propVar +
                                ", " + getter + ".doubleValue());");
            } else if(dProps[i].getType().equals(byte[].class)){
                pWriter.println(INDENT + "this.setByteAValue(" + propVar +
                                ", " + getter + ");");
            } else {
                pWriter.println(INDENT + "this.setObjectValue(" +
                                "DONT KNOW HOW TO HANDLE THIS, " + getter +
                                ");");
            }

            if(xDocletStyle){
                pWriter.println(INDENT + "}");
                pWriter.println();
            }
        }
        pWriter.println("    }");

        pWriter.println();
        pWriter.println("    public " + className + " get" + className +"(){");
        pWriter.println(INDENT + className + " r = new " + className + "();");
        pWriter.println();
        for(int i=0; i<dProps.length; i++){
            String propVar = this.makePropVar(dProps[i]);
            String setter = "r." + this.makeSetter(dProps[i]) + "(";

            if(!this.isLatherStyleProp(dProps[i])){
                continue;
            }

            if(xDocletStyle){
                pWriter.println(INDENT + "try {");
                pWriter.print("    ");
            }

            pWriter.print(INDENT + setter);
            if(dProps[i].getType().equals(String.class)){
                pWriter.println("this.getStringValue(" + propVar + "));");
            } else if(dProps[i].getType().equals(Integer.TYPE)){
                pWriter.println("this.getIntValue(" + propVar + "));");
            } else if(dProps[i].getType().equals(Integer.class)){
                pWriter.println("new Integer(this.getIntValue(" + propVar +
                                ")));");
            } else if(dProps[i].getType().equals(Long.TYPE)){
                pWriter.println("(long)this.getDoubleValue(" + propVar + 
                                "));");
            } else if(dProps[i].getType().equals(Long.class)){
                pWriter.println("new Long((long)this.getDoubleValue(" + 
                                propVar + ")));");
            } else if(dProps[i].getType().equals(Boolean.TYPE)){
                pWriter.println("this.getIntValue(" + propVar + ") == 1 ? " +
                                "true : false);");
            } else if(dProps[i].getType().equals(Boolean.class)){
                pWriter.println("this.getIntValue(" + propVar + ") == 1 ? " +
                                "Boolean.TRUE : Boolean.FALSE);");
            } else if(dProps[i].getType().equals(Double.TYPE)){
                pWriter.println("this.getDoubleValue(" + propVar + "));");
            } else if(dProps[i].getType().equals(Double.class)){
                pWriter.println("new Double(this.getDoubleValue(" + propVar + 
                                ")));");
            } else if(dProps[i].getType().equals(byte[].class)){
                pWriter.println("this.getByteAValue(" + propVar + "));");
            } else {
                pWriter.println("DONT KNOW HOW TO HANDLE " + propVar + "));");
            }

            if(xDocletStyle){
                pWriter.println(INDENT + "} catch(LatherKeyNotFoundException "+
                                "exc){}");
                pWriter.println();
            }
        }        

        pWriter.println(INDENT + "return r;");
        pWriter.println("    }");
        pWriter.println();
        pWriter.println("    protected void validate()");
        pWriter.println("        throws LatherRemoteException");
        pWriter.println("    {");
        if(!xDocletStyle){
            pWriter.println("        try { ");
            pWriter.println("            this.get" + className + "();");
            pWriter.println("        } catch(LatherKeyNotFoundException e){");
            pWriter.println("            throw new LatherRemoteException(\"" +
                            "All values not set\");");
            pWriter.println("        }");
        }
        pWriter.println("    }");
        pWriter.println("}");
        pWriter.flush();
    }


    public static void main(String[] args){
        String outPackage;
        Latherize l;

        if(args.length < 2){
            System.err.println("Syntax: Latherize <outPackage> <class1> " +
                               "[class2] ...");
            return;
        }

        l          = new Latherize();
        outPackage = args[0];
        for(int i=1; i<args.length; i++){
            try {
                Class fClass;
                File outFile;

                fClass = Class.forName(args[i]);
                System.err.println("Latherizing: " + args[i]);
                outFile = new File(args[i]);
                l.latherize(true, outPackage, fClass, 
                            new FileOutputStream(outFile));
            } catch(ClassNotFoundException exc){
                System.err.println("Unable to latherize '" + args[i] + 
                                   "': Class not found in classpath");
            } catch(LatherizeException exc){
                System.err.println("Error latherizing class '" + args[i] + 
                                   "': " + exc.getMessage());
            } catch(IOException exc){
                System.err.println("Error writing class '" + args[i] + "': " +
                                   exc.getMessage());
            }
        }
    }
}
