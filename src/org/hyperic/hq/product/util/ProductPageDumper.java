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

package org.hyperic.hq.product.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.VelocityContext;

import org.hyperic.hq.product.*;
import org.hyperic.util.ArraySet;
import org.hyperic.util.PropertyUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.VelocityUtil;

public class ProductPageDumper extends PluginDumper {

    public static final String FILE_SUFFIX = "-management.htm";

    public static final String[] REMAP_BY_FIRST_WORD
        = { "servlet" };
    public static final String[] EXCLUDE_SERVICES 
        = { // skip dotnet because the only service is 'application' and all 
            // the metrics are the same as for the server.
            "dotnet",
            // similar situation with apache, server has all service metrics
            "apache",
            // all coldfusion services only have 'availability'
            "coldfusion",
            // iis is kinda like apache, server has all service metrics
            "iis" };
    
    private static final Comparator METRIC_COMPARATOR 
        = new Comparator () {
                public int compare(Object o1,
                                   Object o2) {
                    if (o1 instanceof MeasurementInfo && 
                        o2 instanceof MeasurementInfo ) {
                        return ((MeasurementInfo) o1).getName()
                            .compareTo(((MeasurementInfo) o2).getName());
                    }
                    return 0;
                }
                public boolean equals(Object obj) { return false; }
            };
    
    public ProductPageDumper(String pdkDir, String pluginDir) {
        super(pdkDir, pluginDir);
    }

    public static void main (String[] args) {

        // arg[0] is a complete agent dir, including the pdk dir with plugins
        if (args.length != 3) {
            System.err.println("Usage: java "
                               + ProductPageDumper.class.getName()
                               + " <agent.home> <template-name> <output-dir>");
            return;
        }
        File agentHome = new File(args[0]);
        File template = new File(args[1]);
        String outDir = args[2];
        try {
            File pdkDir = new File(agentHome, "pdk");
            File pluginDir = new File(pdkDir, "plugins");

            ProductPageDumper.dumpPages(template,
                                        pdkDir.getCanonicalPath(),
                                        pluginDir.getCanonicalPath(),
                                        outDir);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error generating pages: " + e);
        }
    }

    public static void dumpPages(File template,
                                 String pdkDir, String pluginDir,
                                 String outputDir)
        throws Exception {
        
        ProductPageDumper pd = new ProductPageDumper(pdkDir, pluginDir);
        pd.init();

        pd.dumpPages(template, outputDir);
    }

    private static String getFirstWord(String source) {
        int ix = source.indexOf(' ');
        if (ix == -1) {
            return source;
        }
        return source.substring(0, ix);
    }
    
    public void dumpPages (File template, 
                           String outputDir) throws Exception {

        initVelocity(template);
        
        for (int n=0; n<this.pPlugins.length; n++) {
            ProductPlugin pp = this.pPlugins[n];

            TypeInfo[] entities = pp.getTypes();
            if ((entities == null) || (entities.length == 0)) {
                continue;
            }

            if (containsValue(pp.getName(), REMAP_BY_FIRST_WORD)) {
                // This complex case is for plugins like "servlet" that
                // actually support multiple products.  We don't want
                // a page announcing to the world that "we support servlet
                // management!"  We want a page for "tomcat management," a 
                // page for "jrun management," etc., etc.
                String firstWord;
                HashMap entityMap = new HashMap();
                for (int i=0; i<entities.length; i++) {
                    TypeInfo entity = entities[i];
                    firstWord = getFirstWord(entities[i].getName());
                    List matchingEntities = (List) entityMap.get(firstWord);
                    if (matchingEntities == null) {
                        matchingEntities = new ArrayList();
                        entityMap.put(firstWord, matchingEntities);
                    }
                    matchingEntities.add(entity);
                }

                Iterator iter = entityMap.keySet().iterator();
                while (iter.hasNext()) {
                    firstWord = (String) iter.next();
                    List entityList = (List) entityMap.get(firstWord);
                    generatePage(template, outputDir, 
                                 firstWord.toLowerCase(), entityList);
                }

            } else {
                // Regular plugins that support one product (although
                // probably multiple version of it) are handled here.
                String pluginName = pp.getName();
                List entityList = new ArrayList();
                for (int i=0; i<entities.length; i++) {
                    entityList.add(entities[i]);
                }
                generatePage(template, outputDir, 
                             pluginName.toLowerCase(), entityList);
            }
        }
    }

    private void generatePage ( File template, 
                                String outputDir,
                                String pluginName, 
                                List entities ) throws Exception {
        System.out.println("Generating page for plugin: " + pluginName);
        VelocityContext vctx
            = new VelocityContext(loadPropDrivenContext(template,
                                                        pluginName));

        vctx.put("pluginName", StringUtil.capitalize(pluginName));
        
        Set supportedServerVersions = new ArraySet();
        Set controlActions = new ArraySet();
        Map metricsByGroup = new HashMap();
        List metricGroups = new HappyList();
        List serviceMetricGroups = new ArrayList();
        for (int i=0; i<entities.size(); i++) {
            TypeInfo entity = (TypeInfo) entities.get(i);
            int type = entity.getType();
            String entityName = entity.getName();
            if (type == TypeInfo.TYPE_SERVER) {
                supportedServerVersions.add(entityName);
                try {
                    controlActions.addAll(cpm.getActions(entityName));
                } catch (PluginNotFoundException e) {}
                MeasurementPlugin measPlugin = getMeasurementPlugin(entity);
                MeasurementInfo[] metrics;
                if (measPlugin == null) {
                    metrics = new MeasurementInfo[0];
                } else {
                    metrics = measPlugin.getMeasurements(entity);
                }
                if (metrics != null) {
                    for (int j=0; j<metrics.length; j++) {
                        String group = metrics[j].getGroup();
                        if ( group == null || group.length() == 0 ) {
                            group = getDefaultGroupName(pluginName);
                        }
                        Set metricsInGroup = (Set) metricsByGroup.get(group);
                        if (metricsInGroup == null) {
                            metricsInGroup = new ArraySet(METRIC_COMPARATOR);
                            // System.err.println("Adding server metric group: " + group);
                            metricGroups.add(group);
                                    metricsByGroup.put(group, metricsInGroup);
                        }
                        metricsInGroup.add(metrics[j]);
                    }
                }
            } else if (type == TypeInfo.TYPE_SERVICE) {
                // System.err.println("Adding metrics for service: " + type);
                if (containsValue(pluginName, EXCLUDE_SERVICES)) continue;
                try {
                    controlActions.addAll(cpm.getActions(entityName));
                } catch (PluginNotFoundException e) {}
                MeasurementPlugin measPlugin = getMeasurementPlugin(entity);
                MeasurementInfo[] metrics;
                if (measPlugin == null) {
                    metrics = new MeasurementInfo[0];
                } else {
                    metrics = measPlugin.getMeasurements(entity);
                }
                if (metrics != null) {
                    // service metrics are grouped by service
                    String group = entityName;
                    
                    Set metricsInGroup = (Set) metricsByGroup.get(group);
                    if (metricsInGroup == null) {
                        // System.err.println("Adding service metric group: " + group);
                        metricsInGroup = new ArraySet(METRIC_COMPARATOR);
                        serviceMetricGroups.add(group);
                        metricsByGroup.put(group, metricsInGroup);
                        for (int j=0; j<metrics.length; j++) {
                            metricsInGroup.add(metrics[j]);
                        }
                    }
                }
            }
        }
        
        // Condense service metric groups -- if there are groups that 
        // contain the same set of metrics, combine them into one group 
        // and rename it accordingly
        condense(serviceMetricGroups, metricsByGroup);
        metricGroups.addAll(serviceMetricGroups);
        
        // Sort metric groups, roughly by size, with a couple of other rules:
        // the "default/general" metric group is always listed first
        // service groups are listed after server groups
        Comparator metricSorter = new MetricGroupSorter(serviceMetricGroups, 
                                                        metricsByGroup,
                                                        pluginName);
        Collections.sort(metricGroups, metricSorter);

        // Finally, append " Metrics" to all group names
        String groupName, newName;
        for (int i=0; i<metricGroups.size(); i++) {
            groupName = (String) metricGroups.get(i);
            newName = getMetricGroupName(groupName);
            int loc = metricGroups.indexOf(groupName);
            metricGroups.set(loc, newName);
            metricsByGroup.put(newName, metricsByGroup.get(groupName));
        }

        vctx.put("supportedVersions", supportedServerVersions);
        vctx.put("controlActions", controlActions);
        vctx.put("numMetricGroups", new Integer(metricGroups.size()));
        vctx.put("metricsByGroup", metricsByGroup);
        vctx.put("metricGroups", metricGroups);
        
        generatePage(template, outputDir, pluginName, vctx);
    }

    private void generatePage (File template,
                               String outputDir, 
                               String pluginName, 
                               VelocityContext vctx) throws Exception {
        // Chain everything behind the override context
        vctx = loadOverrideContext(template, pluginName, vctx);
        FileWriter w = null;
        try {
            w = new FileWriter(outputDir + File.separator +
                               pluginName + FILE_SUFFIX);
            try {
                Velocity.mergeTemplate(template.getName(), vctx, w);
            } catch (Exception e) {
                System.err.println("Problem merging template : " + e);
                throw e;
            }
        } finally {
                if (w != null) w.close();
        }
    }

    private VelocityContext loadPropDrivenContext (File template, 
                                                   String pluginName) {
        return loadContext(template, pluginName, "", null);
    }

    private VelocityContext loadOverrideContext (File template, 
                                                 String pluginName,
                                                 VelocityContext ctx) {
        return loadContext(template, pluginName, ".override", ctx);
    }

    private VelocityContext loadContext (File template, 
                                         String pluginName,
                                         String suffix,
                                         VelocityContext existingCtx) {
        // Find the product-specific properties file to merge with the 
        // autogenerated context.  This should be in the same file as
        // the template
        Properties props = null;
        VelocityContext ctx;
        if (existingCtx != null) ctx = new VelocityContext(existingCtx);
        else ctx = new VelocityContext();
        try {
            props = PropertyUtil.loadProperties(template.getParent() 
                                                + File.separator
                                                + pluginName
                                                + suffix + ".properties");
            VelocityUtil.addToContext(ctx, props);

        } catch (IOException e) {
            // OK, no props for this plugin
        }
        return ctx;
    }

    private String getMetricGroupName (String base) {
        return base + " Metrics";
    }

    private void condense (List metricGroups, Map metricsByGroup) {

        String group1, group2;
        Set metricSet1, metricSet2;
        int groupCount = metricGroups.size();
        List matchingGroupLists = new ArrayList();
        MatchingGroupList mgl;
        for (int i=0; i<groupCount; i++) {

            group1     = (String) metricGroups.get(i);
            metricSet1 = (Set) metricsByGroup.get(group1);

            mgl = new MatchingGroupList(group1);
            matchingGroupLists.add(mgl);

            for (int j=i+1; j<metricGroups.size(); j++) {
                group2     = (String) metricGroups.get(j);
                metricSet2 = (Set) metricsByGroup.get(group2);

                if (metricSet1.containsAll(metricSet2) &&
                    metricSet2.containsAll(metricSet2)) {
                    mgl.add(group2);
                }
            }
        }

        String newName;
        for (int i=0; i<matchingGroupLists.size(); i++) {
            mgl = (MatchingGroupList) matchingGroupLists.get(i);
            // Figure out a name for the common group
            mgl.calculateNewName();
        }

        // Now adjust all newnames.  If any current name ends with
        // a condended name, we need to qualify the condensed name
        for (int i=0; i<matchingGroupLists.size(); i++) {
            mgl = (MatchingGroupList) matchingGroupLists.get(i);
            if (!mgl.wasCondensed()) continue;
            for (int j=i+1; j<matchingGroupLists.size(); j++) {
                MatchingGroupList other
                    = (MatchingGroupList) matchingGroupLists.get(j);
                if (other.newName.endsWith(mgl.newName)) {
                    // System.err.println("calling qualify (b1="+mgl.baseGroup+",b2="+other.baseGroup+") because n1="+mgl.newName+"=="+other.newName);
                    // qualify both
                    other.qualifyNewName();
                    mgl.qualifyNewName();
                }
            }
        }

        for (int i=0; i<matchingGroupLists.size(); i++) {
            mgl = (MatchingGroupList) matchingGroupLists.get(i);
            int loc = metricGroups.indexOf(mgl.baseGroup);
            if (loc == -1) {
                // we were already removed
                continue;
            }
            metricGroups.set(loc, mgl.newName);
            metricsByGroup.put(mgl.newName, metricsByGroup.get(mgl.baseGroup));

            // remove matches from metricGroups
            for (int j=0; j<mgl.matches.size(); j++) {
                metricGroups.remove(mgl.matches.get(j));
            }
        }
    }

    private void initVelocity(File template) throws Exception {
        Properties vProps = new Properties();
        vProps.setProperty("resource.loader", "file");
        vProps.setProperty("file.resource.loader.description", "what-eva, what-eva, i do what i want!");
        vProps.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        vProps.setProperty("file.resource.loader.path", template.getParent());
        vProps.setProperty("file.resource.loader.cache", "true");
        vProps.setProperty("file.resource.loader.modificationCheckInterval", "0");
        vProps.setProperty("counter.initial.value", "0");
        try {
            Velocity.init(vProps);
        } catch(Exception e) {
            System.err.println("Problem initializing Velocity : " + e);
            throw e;
        }
    }

    class MatchingGroupList {
        public String newName;
        public String baseGroup;
        public List matches = new ArrayList();
        public MatchingGroupList (String name) {
            baseGroup = name;
        }
        public void add (String name) {
            matches.add(name);
        }
        public boolean hasMatches () {return (matches.size() > 0);}
        public boolean wasCondensed () { return !baseGroup.equals(newName); }
        private List getAllList () {
            List all = new ArrayList(matches);
            all.add(baseGroup);
            return all;
        }
        public void calculateNewName () {
            // Figure out what the common ending is.
            List all = getAllList();
            newName = getCommonEnding(all);
            // System.err.println("CONDENSED (b="+baseGroup+", m="+StringUtil.listToString(matches)+") to: " + newName);
        }
        public void qualifyNewName () {
            List all = getAllList();
            String commonBeginning = getCommonBeginning(all);
            String commonEnding = getCommonEnding(all);
            String qual = removeBeginningAndEnd(baseGroup,
                                                           commonBeginning, 
                                                           commonEnding);
            for (int i=0; i<matches.size(); i++) {
                String part1 = (String) matches.get(i);
                String part2 = removeBeginningAndEnd(part1, 
                                                     commonBeginning,
                                                     commonEnding);
                // System.err.println("xform("+commonBeginning+","+commonEnding+"):'" + part1 + "'-->'" + part2 + "'");
                if (i == matches.size()-1) {
                    qual += " and " + part2;
                } else {
                    qual += ", " + part2;
                }
            }
            newName = commonBeginning + " " + qual + " " + commonEnding;
        }
    }

    class MetricGroupSorter implements Comparator {
        private List serviceGroups;
        private Map metricData;
        private String pluginName;
        public MetricGroupSorter (List serviceGroups, Map metricData, 
                                  String pluginName) {
            this.serviceGroups = serviceGroups;
            this.metricData = metricData;
            this.pluginName = pluginName;
        }

        public int compare(Object o1,
                           Object o2) {
            if (o1 instanceof String && 
                o2 instanceof String ) {
                String s1 = (String) o1;
                String s2 = (String) o2;
                // default group is always first
                if (s1.startsWith(getDefaultGroupName(pluginName))) {
                    return Integer.MIN_VALUE;
                }
                // get magnitudes
                int m1 = ((List) metricData.get(s1)).size();
                int m2 = ((List) metricData.get(s2)).size();
                boolean isService1 = serviceGroups.contains(s1);
                boolean isService2 = serviceGroups.contains(s2);
                if (!isService1) {
                    if (!isService2) {
                        // both server groups, return diff
                        return m1-m2;
                    } else {
                        // m1 is server, m2 is service. m1 comes first
                        return Integer.MIN_VALUE + m2;
                    }
                } else {
                    if (!isService2) {
                        // m2 is server, m1 is service.  m2 comes first
                        return Integer.MAX_VALUE - m1;
                    } else {
                        // both are services, return 100000 + (diff)
                        return 1000000 + (m1-m2);
                    }
                }
            }
            return 0;
        }

        public boolean equals(Object obj) { return false; }
    }

    private String getDefaultGroupName (String plugin) {
        return "General Server";
    }

    /**
     * Given a list of strings, return the longest string of words that they
     * all end with.  If they all end differently, then an empty string will be
     * returned.
     * @return The longest common string of words that all the given strings 
     * end with.
     */
    private static String getCommonEnding (List strings) {
    
        int numStrings = strings.size();
        if (numStrings == 0) return "";
        if (numStrings == 1) return (String) strings.get(0);
    
        StringBuffer rstr = new StringBuffer();
    
        List wordList = new ArrayList();
        int shortest = getSmallestWordCount(strings, wordList);
    
        String word, next;
        List words;
        for (int i=0; i<shortest; i++) {
            words = (List) wordList.get(0);
            word = (String) words.get(words.size()-1-i);
            for (int j=1; j<numStrings; j++) {
                words = (List) wordList.get(j);
                next = (String) words.get(words.size()-1-i);
                if (!next.equals(word)) return rstr.toString().trim();
            }
            rstr.insert(0, word);
            rstr.insert(0, ' ');
        }
        return rstr.toString().trim();
    }

    private static String removeBeginningAndEnd (String source, 
                                                 String begin, String end) {
        if (source.startsWith(begin)) {
            source = source.substring(begin.length());
        }
        if (source.endsWith(end)) {
            source = source.substring(0, source.length()-end.length());
        }
        return source.trim();
    }

    /**
     * Given a list of strings, return the longest string of words that they
     * all begin with.  If they all begin differently, then an empty string 
     * will be returned.
     * @return The longest common string of words that all the given strings 
     * begin with.
     */
    private static String getCommonBeginning (List strings) {
    
        int numStrings = strings.size();
        if (numStrings == 0) return "";
        if (numStrings == 1) return (String) strings.get(0);
    
        StringBuffer rstr = new StringBuffer();
    
        List wordList = new ArrayList();
        int shortest = getSmallestWordCount(strings, wordList);
    
        String word, next;
        List words;
        for (int i=0; i<shortest; i++) {
            words = (List) wordList.get(0);
            word = (String) words.get(i);
            for (int j=1; j<numStrings; j++) {
                words = (List) wordList.get(j);
                next = (String) words.get(i);
                if (!next.equals(word)) return rstr.toString().trim();
            }
            rstr.append(word);
            rstr.append(' ');
        }
        return rstr.toString().trim();
    }

    /**
     * @param strings The strings to examine
     * @param wordLists a List that, when the method returns, will contain a 
     * List of Lists, each sublist containing the words of the strings.  If
     * null, this will not be filled out.
     * @return The number of words in the String with the fewest number of words
     */
    private static int getSmallestWordCount (List strings, List wordLists) {
        int numStrings = strings.size();
        if (numStrings == 0) return 0;
    
        int shortest = Integer.MAX_VALUE;
        List words;
        for (int i=0; i<numStrings; i++) {
            words = StringUtil.explode((String) strings.get(i), " \n\t");
            if (wordLists != null) wordLists.add(words);
            if (i == 0 || words.size() < shortest) {
                shortest = words.size();
            }
        }
        return shortest;
    }

    /**
     * Find a value in an array.  This method returns true only if
     * the exact object is found in the array (i.e. the comparison
     * is done with == not .equals).
     * @param srch The object to find
     * @param array The array to search
     * @return true if the object exists in the array, false otherwise.
     */
    private static boolean containsValue(Object srch, Object[] array) {
        for (int i=0; i<array.length; i++) {
            if (srch == array[i]) {
                return true;
            }
        }
    
        return false;
    }
    
    private final class HappyList extends ArrayList {
        public Object get(int index) {
            try {
                return super.get(index);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }
}
