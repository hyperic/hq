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

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WebContextConfigure extends Task {

	private String _webContextFile;
	private String _maxConns;

	public void setWebContextFile(String webContextFile) {
		_webContextFile = webContextFile;
	}

	public void setMaxConns(String maxConns) {
		_maxConns = maxConns;
	}

	@Override
	public void execute() throws BuildException 
	{
		Document existingConfig;

		validate();
		File f = new File(_webContextFile);
		try {
			DocumentBuilder dom =
					DocumentBuilderFactory.newInstance().newDocumentBuilder();
			existingConfig = dom.parse(f);
		} 
		catch (Exception e) {
			throw new BuildException("Error parsing jms-context file " + f, e);
		}

		NodeList list = existingConfig.getElementsByTagName("init-param");
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			Element element = (Element)n;
			NodeList nlList = element.getElementsByTagName("param-name").item(0).getChildNodes();		 
	        Node nValue = nlList.item(0);
			if ("org.hyperic.lather.maxConns".equalsIgnoreCase( nValue.getNodeValue())) {
				nlList = element.getElementsByTagName("param-value").item(0).getChildNodes();		 
		        nValue = nlList.item(0);
		        nValue.setNodeValue(_maxConns);
			}
		}
		writeDocument(_webContextFile, existingConfig);
	}

	private void writeDocument(String file, Document d) throws BuildException
	{
		try {
			DOMSource ds = new DOMSource(d);
			StreamResult sr = new StreamResult(file);
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.transform(ds, sr);
		} catch (Exception e) {
			throw new BuildException("Error writing web.xml " + file, e);
		}
	}

	private void validate() throws BuildException {
		if (_webContextFile == null) {
			throw new BuildException("web.xml file not given");
		}

		if (_maxConns == null) {
			throw new BuildException("maxConns value is not given");
		}
	}
	
	public static void main(String[] args) {
		WebContextConfigure c = new WebContextConfigure();
		c.setMaxConns("11111");
		c.setWebContextFile("/tmp/web.xml");
		c.execute();
	}


}
