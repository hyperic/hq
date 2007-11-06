package org.hyperic.tools.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

public class EhCacheUpgrader extends Task {

    private String _existingConfigFile;
    private String _newConfigFile;

    public void log(String log) {
        System.out.println(log);
    }

    public void setExisting(String existingConfigFile) {
        _existingConfigFile = existingConfigFile;
    }

    public void setNew(String newConfigFile) {
        _newConfigFile = newConfigFile;
    }

    public void execute() throws BuildException {

        validate();
        Document existingConfig = loadDocument(_existingConfigFile);
        Document newConfig = loadDocument(_newConfigFile);

        Map sizes = loadCacheSizes(existingConfig);

        NodeList list = newConfig.getElementsByTagName("cache");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            String name = n.getAttributes().getNamedItem("name").getNodeValue();
            String size = n.getAttributes().getNamedItem("maxElementsInMemory")
                .getNodeValue();

            Integer newSize = new Integer(size);
            Integer oldSize = (Integer)sizes.get(name);

            if (oldSize != null && oldSize.intValue() > newSize.intValue()) {
                log("Increasing cache size for " + name + " from default " +
                    " value of " + newSize + " to " + oldSize);
                n.getAttributes().getNamedItem("maxElementsInMemory")
                    .setNodeValue(oldSize.toString());
            }
        }

        writeDocument(_newConfigFile, newConfig);
    }

    private Map loadCacheSizes(Document doc) throws BuildException
    {
        Map ret = new HashMap();

        NodeList list = doc.getElementsByTagName("cache");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            String name = n.getAttributes().getNamedItem("name").getNodeValue();
            String size = n.getAttributes().getNamedItem("maxElementsInMemory").
                getNodeValue();
            ret.put(name, new Integer(size));
        }

        return ret;
    }

    private Document loadDocument(String file) throws BuildException
    {
        File f = new File(file);
        log("Parsing ehcache configuration=" + f.getAbsolutePath());
        try {
            DocumentBuilder dom =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return dom.parse(f);
        } catch (Exception e) {
            throw new BuildException("Error parsing ehcache config " + file, e);
        }
    }

    private void writeDocument(String file, Document d) throws BuildException
    {
        try {
            DOMSource ds = new DOMSource(d);
            StreamResult sr = new StreamResult(file);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.transform(ds, sr);
        } catch (Exception e) {
            throw new BuildException("Error writing ehcache config " + file, e);
        }
    }

    private void validate() throws BuildException {
        if (_existingConfigFile == null) {
            throw new BuildException("Existing ehcache configuration not given");
        }

        if (_newConfigFile == null) {
            throw new BuildException("New ehcache configuration not given");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Usage: oldConfig.xml newConfig.xml");
        }

        EhCacheUpgrader upgrader = new EhCacheUpgrader();
        upgrader.setExisting(args[0]);
        upgrader.setNew(args[1]);
        upgrader.execute();
    }
}
