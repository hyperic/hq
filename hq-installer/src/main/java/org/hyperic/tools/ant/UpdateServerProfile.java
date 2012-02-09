/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2009 - 2012], Hyperic, Inc.
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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


public class UpdateServerProfile extends Task {

	private String src;
	private String dest;

	public UpdateServerProfile () {}

	public void setSrc ( String src ) {
		this.src = src;
	}
	public void setDest ( String dest ) {
		this.dest = dest;
	}

	@Override
	public void execute () throws BuildException {
		validateAttributes();
		Properties profile = loadProperties(src);
		Properties confFile = loadProperties(dest);
		Enumeration enumProps = profile.propertyNames();
		while (enumProps.hasMoreElements()) {
			String  key = (String) enumProps.nextElement();
			if(null != confFile.getProperty(key)) {
				confFile.setProperty(key, profile.getProperty(key));
			}
		}
		saveProperties(confFile, dest);
	}

	private void validateAttributes () throws BuildException {
		if ( src == null ) {
			throw new BuildException("No 'src' attribute specified.");
		}
		if ( dest == null ) {
			throw new BuildException("No 'dest' attribute specified.");
		}
	}

	private  Properties loadProperties(String fileName) {

		InputStream inPropFile;
		Properties tempProp = new Properties();

		try {
			inPropFile = new FileInputStream(fileName);
			tempProp.load(inPropFile);
			inPropFile.close();
		} 
		catch (IOException ioe) {
			throw new BuildException("Could not find file '" + fileName + "'");
		}
		return tempProp;
	}

	private static void saveProperties(Properties p, String fileName) {

		OutputStream outPropFile;
		try {
			outPropFile = new FileOutputStream(fileName);
			p.store(outPropFile, "Properties File to the Test Application");
			outPropFile.close();
		} catch (IOException ioe) {
			throw new BuildException("Could not find file '" + fileName + "'");
		}
	}
}
