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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JmsContextConfigure extends Task {

	private String _jmsContextFile;
	private String _useJmx;
	private String _jmxPort;

	public void setJmsContextFile(String jmsContextFile) {
		_jmsContextFile = jmsContextFile;
	}

	public void setUseJmx(String useJmx) {
		_useJmx = useJmx;
	}

	public void setJmxPort(String jmxPort) {
		_jmxPort = jmxPort;
	}

	@Override
	public void execute() throws BuildException 
	{
		Document existingConfig;
		
		validate();
		File f = new File(_jmsContextFile);
		try {
			DocumentBuilder dom =
					DocumentBuilderFactory.newInstance().newDocumentBuilder();
			 existingConfig = dom.parse(f);
		} 
		catch (Exception e) {
			throw new BuildException("Error parsing jms-context file " + f, e);
		}
		
		NodeList list = existingConfig.getElementsByTagName("amq:broker");
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			n.getAttributes().getNamedItem("useJmx").setNodeValue(_useJmx);
		}
		list = existingConfig.getElementsByTagName("amq:managementContext");
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			if (null != n.getAttributes().getNamedItem("connectorPort"))
				n.getAttributes().getNamedItem("connectorPort").setNodeValue(_jmxPort);
		}
	writeDocument(_jmsContextFile, existingConfig);
}

private void writeDocument(String file, Document d) throws BuildException
{
	try {
		DOMSource ds = new DOMSource(d);
		StreamResult sr = new StreamResult(file);
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.transform(ds, sr);
	} catch (Exception e) {
		throw new BuildException("Error writing jms-context config " + file, e);
	}
}

private void validate() throws BuildException {
	if (_jmsContextFile == null) {
		throw new BuildException("jmx-context file not given");
	}

	if (_useJmx == null) {
		throw new BuildException("useJmx flag is not given");
	}

	if (_jmxPort == null) {
		throw new BuildException("Jmx port is not given");
	}
}


}
