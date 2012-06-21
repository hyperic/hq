/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2012], VMWare, Inc.
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
package org.hyperic.hq.api.rest;

import java.lang.reflect.Array;

import org.hyperic.hq.api.rest.RestTestCaseBase.ServiceBindingType;
import org.hyperic.hq.test.TestHelper;
import org.hyperic.hq.tests.context.TestDataPopulator;
import org.springframework.beans.factory.annotation.Autowired;

import com.meterware.servletunit.ServletRunner;

public abstract class AbstractRestTestDataPopulator<T> extends TestHelper implements TestDataPopulator{ 

	@Autowired
	protected ServletRunner servletRunner ; 
	
    private T[] arrServices ; 
	
    private String serviceURL ; 
    private Class<T> serviceInterface ; 
    
    public AbstractRestTestDataPopulator(final Class<T> serviceInterface, final String serviceURL) { 
    	super() ; 
    	this.serviceInterface = serviceInterface ; 
    	this.serviceURL = serviceURL ; 
    }//EOM 
    
    @Override
	public void populate() throws Exception {
    	this.generateServices() ; 
    }//EOM 
    
    protected final void generateServices() { 
    	arrServices = (T[]) Array.newInstance(this.serviceInterface, 2); 
    	arrServices[0]  = RestTestCaseBase.generateServiceClient(this.serviceInterface, ServiceBindingType.XML, this.serviceURL, this.servletRunner) ;
    	arrServices[1] = RestTestCaseBase.generateServiceClient(this.serviceInterface, ServiceBindingType.JSON, this.serviceURL, this.servletRunner) ;
    }//EOM 
    
    public final T[] getServices() { 
    	return this.arrServices ; 
    }//EOM 
    
}//EOC 
