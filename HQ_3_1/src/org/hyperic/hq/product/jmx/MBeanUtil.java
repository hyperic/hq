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

package org.hyperic.hq.product.jmx;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MBeanUtil {
    private static Log log =
        LogFactory.getLog(MBeanUtil.class);

    private static Map converters = new HashMap();

    static {
        initConverters();
    }

    //convert a String to common types
    public interface Converter {
        public Object convert(String param);
    }

    public interface ListConverter {
        public Object convert(String[] params);
    }

    public static void addConverter(Class type, Converter converter) {
        converters.put(type.getName(), converter);
    }

    public static void addConverter(Class type, ListConverter converter) {
        converters.put(type.getName(), converter);
    }

    private static void addConverter(Class addType, Class fromType) {
        converters.put(addType.getName(),
                       converters.get(fromType.getName()));
    }

    private static IllegalArgumentException invalid(String param, Exception e) {
        return new IllegalArgumentException("'" + param + "': " +
                                             e.getMessage());
    }

    private static void initConverters() {
        addConverter(Object.class, new Converter() {
            public Object convert(String param) {
                return param;
            }
         });

        addConverter(Short.class, new Converter() {
            public Object convert(String param) {
                return Short.valueOf(param);
            }
        });

        addConverter(Integer.class, new Converter() {
            public Object convert(String param) {
                return Integer.valueOf(param);
            }
        });

        addConverter(Long.class, new Converter() {
            public Object convert(String param) {
                return Long.valueOf(param);
            }
        });

        addConverter(Double.class, new Converter() {
            public Object convert(String param) {
                return Double.valueOf(param);
            }
        });

        addConverter(Boolean.class, new Converter() {
            public Object convert(String param) {
                return Boolean.valueOf(param);
            }
        });

        addConverter(File.class, new Converter() {
            public Object convert(String param) {
                return new File(param);
            }
        });
        
        addConverter(URL.class, new Converter() {
            public Object convert(String param) {
                try {
                    return new URL(param);
                } catch (MalformedURLException e) {
                    throw invalid(param, e);
                }
            }
        });

        addConverter(ObjectName.class, new Converter() {
            public Object convert(String param) {
                try {
                    return new ObjectName(param);
                } catch (MalformedObjectNameException e) {
                    throw invalid(param, e);
                }
            }
        });

        addConverter(List.class, new ListConverter() {
            public Object convert(String[] params) {
                return Arrays.asList(params);
            }
        });

        addConverter(String[].class, new ListConverter() {
            public Object convert(String[] params) {
                return params;
            }
        });

        addConverter(Long[].class, new ListConverter() {
            public Object convert(String[] params) {
                Long[] args = new Long[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Long.valueOf(params[i]);
                }
                return args;
            }
        });

        addConverter(Integer[].class, new ListConverter() {
            public Object convert(String[] params) {
                Integer[] args = new Integer[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Integer.valueOf(params[i]);
                }
                return args;
            }
        });

        addConverter(Double[].class, new ListConverter() {
            public Object convert(String[] params) {
                Double[] args = new Double[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Double.valueOf(params[i]);
                }
                return args;
            }
        });

        addConverter(Short[].class, new ListConverter() {
            public Object convert(String[] params) {
                Short[] args = new Short[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Short.valueOf(params[i]);
                }
                return args;
            }
        });

        addConverter(Boolean[].class, new ListConverter() {
            public Object convert(String[] params) {
                Boolean[] args = new Boolean[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Boolean.valueOf(params[i]);
                }
                return args;
            }
        });

        addConverter(long[].class, new ListConverter() {
            public Object convert(String[] params) {
                long[] args = new long[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Long.parseLong(params[i]);
                }
                return args;
            }
        });

        addConverter(int[].class, new ListConverter() {
            public Object convert(String[] params) {
                int[] args = new int[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Integer.parseInt(params[i]);
                }
                return args;
            }
        });

        addConverter(double[].class, new ListConverter() {
            public Object convert(String[] params) {
                double[] args = new double[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Double.parseDouble(params[i]);
                }
                return args;
            }
        });

        addConverter(short[].class, new ListConverter() {
            public Object convert(String[] params) {
                short[] args = new short[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = Short.parseShort(params[i]);
                }
                return args;
            }
        });

        addConverter(boolean[].class, new ListConverter() {
            public Object convert(String[] params) {
                boolean[] args = new boolean[params.length];
                for (int i=0; i<params.length; i++) {
                    args[i] = params[i].equals("true") ? true : false;
                }
                return args;
            }
        });

        Class[][] aliases = {
            { String.class, Object.class },
            { Short.TYPE, Short.class },
            { Integer.TYPE, Integer.class },
            { Long.TYPE, Long.class },
            { Double.TYPE, Double.class },
            { Boolean.TYPE, Boolean.class },
        };

        for (int i=0; i<aliases.length; i++) {
            addConverter(aliases[i][0], aliases[i][1]);
        }
    }

    private static Object getConverter(String type) {
        Object converter = converters.get(type);
        if (converter == null) {
            converter = converters.get(Object.class.getName());
        }
        return (Object)converter;
    }

    private static boolean hasConverter(String type) {
        return converters.get(type) != null;
    }
    
    private static Object convert(String type, String param) {
        return ((Converter)getConverter(type)).convert(param);
    }

    private static Object convert(String type, String[] params) {
        return ((ListConverter)getConverter(type)).convert(params);
    }

    private static boolean isListType(String type) {
        return getConverter(type) instanceof ListConverter;
    }

    public static class OperationParams {
        public Object[] arguments;
        public String[] signature;
        public boolean isAttribute = false;
    }

    private static PluginException invalidParams(String method,
                                                 HashMap sigs,
                                                 Object[]args) {
        StringBuffer num = new StringBuffer();
        StringBuffer sig = new StringBuffer();

        for (Iterator it=sigs.keySet().iterator(); it.hasNext();) {
            Object o = it.next();
            StringBuffer sb;
            if (o instanceof Integer) {
                sb = num;
            }
            else {
                sb = sig;
            }
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(o);
        }

        String msg =
            "operation '" + method + "' takes (" + num + ")" +
            " arguments, " + args.length + " given. Signature=[" + sig + "]";
        return new PluginException(msg);
    }

    public static OperationParams getAttributeParams(MBeanInfo info,
                                                     String method,
                                                     Object args[])
        throws PluginException {

        if (method.startsWith("set")) {
            method = method.substring(3);
        }

        MBeanAttributeInfo[] attrs = info.getAttributes();
        for (int i=0; i<attrs.length; i++) {
            MBeanAttributeInfo attr = attrs[i];
            if (!attr.getName().equals(method)) {
                continue;
            }
            if (!attr.isWritable()) {
                throw new PluginException("Attribute '" + method +
                                          "' is not writable");
            }

            String sig = attr.getType();
            if (!hasConverter(sig)) {
                String msg =
                    "Cannot convert String argument to " + sig;
                throw new PluginException(msg);
            }

            if (args.length != 1) {
                String msg =
                    "setAttribute(" + method +
                    ") takes [1] argument, [" +
                    args.length + "] given";
                throw new PluginException(msg);
            }

            OperationParams params = new OperationParams();
            Object value;
            try {
                value = convert(sig, (String)args[0]);
            } catch (Exception e) {
                String msg =
                    "Exception converting param '" +
                    args[0] + "' to type '" + sig + "'";
                    throw new PluginException(msg + ": " + e);
            }
            params.arguments =
                new Object[] { value };
            params.isAttribute = true;
            return params;
        }

        return null;
    }

    private static String toString(MBeanParameterInfo[] pinfo) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<pinfo.length; i++) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(pinfo[i].getType());
        }
        sb.insert(0, '(');
        sb.append(')');
        return sb.toString();
    }

    public static OperationParams getOperationParams(MBeanInfo info,
                                                     String method,
                                                     Object args[])
        throws PluginException {

        boolean isDebug = log.isDebugEnabled();
        MBeanOperationInfo[] ops = info.getOperations();
        MBeanParameterInfo[] pinfo = null;
        HashMap sigs = new HashMap();
        String methodSignature = null;

        if (args.length != 0) {
            String arg = (String)args[0];
            if (arg.startsWith("@(") && arg.endsWith(")")) {
                methodSignature = arg.substring(1);
                String[] dst = new String[args.length-1];
                System.arraycopy(args, 1, dst, 0, dst.length);
                args = dst;
            }
        }

        if (isDebug) {
            String msg =
                "Converting params for: " +
                method + Arrays.asList(args);
            if (methodSignature != null) {
                msg += ", using provided signature: " +
                       methodSignature;
            }
            log.debug(msg);
        }

        for (int i=0; i<ops.length; i++) {
            if (ops[i].getName().equals(method)) {
                pinfo = ops[i].getSignature();
                StringBuffer sig = new StringBuffer();
                sig.append("(");
                for (int j=0; j<pinfo.length; j++) {
                    sig.append(pinfo[j].getType());
                    if (j+1 != pinfo.length){
                        sig.append(';');
                    }
                }
                sig.append(')');
                log.debug("Found operation: " + method + sig);
                sigs.put(sig.toString(), pinfo);
                sigs.put(new Integer(pinfo.length), pinfo);
                //XXX might have more than 1 method w/ same
                //number of args but different signature
            }
        }        

        if (sigs.size() == 0) {
            OperationParams op =
                getAttributeParams(info, method, args);
            if (op != null) {
                return op;
            }
            String msg =
                "No MBean Operation or Attribute Info found for: " +
                method;
            throw new PluginException(msg);
        }
        else if (sigs.size() > 1) {
            if (methodSignature == null) {
                //try exact match, else last one wins.
                Object o = sigs.get(new Integer(args.length));
                if (o != null) { 
                    pinfo = (MBeanParameterInfo[])o;
                    if (log.isDebugEnabled()) {
                        log.debug("Using default sig: " + toString(pinfo));
                    }
                }
            }
            else {
                pinfo =
                    (MBeanParameterInfo[])sigs.get(methodSignature);
                if (pinfo == null) {
                    String msg =
                        "No matching Operation signature found for: " +
                        method + methodSignature;
                    throw new PluginException(msg);
                }
                else if (log.isDebugEnabled()) {
                    log.debug("Using matched sig: " + toString(pinfo));
                }
            }
        }

        int len = pinfo.length;
        int nargs = args.length;
        int consumed;
        String[] signature = new String[len];
        List arguments = new ArrayList();

        for (int i=0,j=0; i<len; i++, j+=consumed) {
            consumed = 1;
            String sig = pinfo[i].getType();
            signature[i] = sig;

            if (!hasConverter(sig)) {
                String msg =
                    "Cannot convert String argument to " + sig;
                throw new PluginException(msg);
            }
            
            if (j >= args.length) {
                throw invalidParams(method, sigs, args);
            }

            if (isListType(sig)) {
                String[] listArgs;
                if (len == 1) {
                    listArgs = (String[])args;
                }
                else {
                    int remain = (len-1) - j;
                    consumed = args.length - j - remain;

                    listArgs = new String[consumed];
                    System.arraycopy(args, j, listArgs, 0, consumed);
                }

                nargs -= listArgs.length;
                try {
                    arguments.add(convert(sig, listArgs));
                } catch (Exception e) {
                    String msg =
                        "Exception converting " + Arrays.asList(listArgs) +
                        "' to type '" + sig + "'";
                        throw new PluginException(msg + ": " + e);
                }
            }
            else {
                nargs--;
                try {
                    arguments.add(convert(sig, (String)args[j]));
                } catch (Exception e) {
                    String msg =
                        "Exception converting param[" + j + "] '" +
                        args[j] + "' to type '" + sig + "'";
                        throw new PluginException(msg + ": " + e);
                }
            }

            if (isDebug) {
                Object arg = arguments.get(i);
                if (arg.getClass().isArray()) {
                    arg = Arrays.asList((Object[])arg).toString();
                }
                log.debug(method + "() arg " + i + "=" +
                          arg + ", type=" + sig);
            }
        }

        if (nargs != 0) {
            throw invalidParams(method, sigs, args);
        }

        OperationParams params = new OperationParams();
        params.signature = signature;
        params.arguments = arguments.toArray();
        return params;
    }
}
