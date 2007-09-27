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

package org.hyperic.hq.measurement.server.express;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import instantj.expression.Expression;
import instantj.compile.CompilationFailedException;
import instantj.compile.CompiledClass;
import instantj.compile.Source;

/**
* ExpressionUtil - utility methods for Expression evaluation package.
* */
public class ExpressionUtil {

    /** This method has been externalized because its a good candidate
     *  for user interfaces that need to test the compilability of an
     *  expression.
    * */
    public static CompiledClass compileExpression (String source) throws
        CompilationFailedException {

        return instantj.compile.Compiler.compile(new Source(source),true);
    }

    /** Rehydrates a serialized Expression back into an instance of Expression.
     * @param byteArray containing the serialized class.
     * */
    public static Expression deSerialize (byte[] byteArray)
        					throws IOException, ClassNotFoundException {
        Expression 				retVal 	= null;
        ByteArrayInputStream 	bais 	= null;
		ObjectInputStream 		ois		= null;
        try {
			bais 	= new ByteArrayInputStream (byteArray);
			ois 	= new ObjectInputStream (bais);
			retVal 	= (Expression)ois.readObject();
        } catch (IOException e) {
            throw e;
        }
        catch (ClassNotFoundException e) {
            throw e;
        }
        finally {
        	if (ois   != null ) try {ois.close(); }catch(Exception ex){}
       		if (bais  != null ) try {bais.close();}catch(Exception ex){}
        }
        return retVal;
    }
    /** Dehydrates an Expression into Serialized bytearray.
     * @param the "to be" serialized expression.
     * */
    public static byte[] serializeExpression (Expression expression)
        					throws IOException {
        byte[] 					retVal 	= null;
        ByteArrayOutputStream 	bout 	= null;
		ObjectOutputStream 		oos		= null;
        try {
			bout 	= new ByteArrayOutputStream ();
			oos 	= new ObjectOutputStream (bout);
			oos.writeObject(expression);
            retVal = bout.toByteArray();
        } catch (IOException e) {
            // to log or not to log?
            throw e;
        }
        finally {
        	if (oos   != null ) try {oos.close(); }catch(Exception ex){}
       		if (bout  != null ) try {bout.close();}catch(Exception ex){}
        }
        return retVal;
    }



    /** Slightly patched version of instantJ's ReflectAccess.calcClassFromName
     *  method that accounts for and handles arrays.
     * */
	public static Class calcClassFromName(String name)
        throws IllegalArgumentException {

		if (Boolean.TYPE.getName().equals(name))
			return Boolean.TYPE;
		if (Integer.TYPE.getName().equals(name))
			return Integer.TYPE;
		if (Short.TYPE.getName().equals(name))
			return Short.TYPE;
		if (Character.TYPE.getName().equals(name))
			return Character.TYPE;
		if ("String".equals(name))
			return String.class;
		if ("Boolean".equals(name))
			return Boolean.class;
		if ("Integer".equals(name)||"java.lang.Integer".equals(name))
			return Integer.class;
		if ("Double".equals(name) || "java.lang.Double".equals(name))
			return Double.class;
        if ("Float".equals(name) || "java.lang.Float".equals(name))
	        return Float.class;
        if ("Long".equals(name) || "java.lang.Long".equals(name))
	        return Long.class;
		if ("Date".equals(name))
			return java.util.Date.class;
        // Handle arrays. Assumes that we'll always use java.reflect.Array
        // to directly manipulate java.lang.Object referencing an Array.
		if (name.indexOf("[")>-1)
            return java.lang.Object.class;
        try {
			return Class.forName(name.toString());
		} catch (Throwable t) {
			throw new IllegalArgumentException("Couldn't resolve type for name " + name);
        }
	}
}
