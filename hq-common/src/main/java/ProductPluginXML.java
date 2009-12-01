package org.hyperic.hq.product;

/**
 * the hq-plugin.xml has obsoleted the need for most plugins
 * to implement a ProductPlugin.  however, a ProductPlugin instance
 * is still required for each plugin and must be loaded by the plugin's
 * ClassLoader.  to deal with this, ProductPluginXML.class is renamed
 * to ProductPluginXML.stub so the plugin parent ClassLoader will not
 * see it.  ProductPluginManager then reads in the .stub bytecode
 * and defines the class using the plugin's PluginLoader.
 *
 * the .stub file is checked into cvs since it should never need
 * to change and otherwise would add more clutter to build.xml
 * along with confusing eclipse.
 * to build:
 * javac -classpath ../../build/classes ProductPluginXML.java
 * mv ProductPluginXML.class ProductPluginXML.stub
 */
public class ProductPluginXML extends ProductPlugin {}
