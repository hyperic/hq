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

package org.hyperic.hq.common.shared.util;

import java.util.Iterator;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.system.ServiceMBean;

/**
 * JSR-77.666
 * if only the MBean
 *   jboss.management.single:
 *   J2EEServer=Single,j2eeType=J2EEApplication,_name=covalent-eam.ear
 *   (class org.jboss.management.j2ee.J2EEApplication)
 * was an EventProvider, we would not need this class.  cannot find
 * a way to receive notification when said application has been fully
 * started.  this class attempts to provide said functionality.
 * it does so by registering notifications for the modules within the
 * ear which are EventProviders.  once all modules have started, we
 * consider the ear to be started.
 * listing a pile of <depends> in product-plugin-service.xml has not been
 * solving this problem properly either.
 */

public class EjbModuleLifecycle
    implements NotificationListener 
{
    private static final int SERVICE_STARTED = ServiceMBean.STARTED;
    private static final int SERVICE_STOPPED = ServiceMBean.STOPPED;

    private static final String SCOPE = "jboss.j2ee:service=EjbModule,*";

    private boolean                    _running = false;
    private boolean                    _notifyStopped = false;
    private ObjectName                 _scope;
    private int                        _numModules = 0;
    private int                        _startedModules = 0;
    private MBeanServer                _server;
    private EjbModuleLifecycleListener _listener;
    private String                     _name;

    public EjbModuleLifecycle(MBeanServer server,
                              EjbModuleLifecycleListener listener,
                              String name) 
    {
        _server   = server;
        _listener = listener;
        _name     = name;
    }

    public boolean isRunning() {
        return _running;
    }

    private Iterator getIterator() {
        // return this.server.queryNames(this.scope, this.query).iterator(); 
        return _server.queryNames(_scope, null).iterator();
    }

    private boolean apply(ObjectName obj) {
        //XXX jboss seems unhappy with this.query.apply(obj)
        String prop = obj.getKeyProperty("url"); //3.0
      
        if (prop == null) {
            prop = obj.getKeyProperty("module"); //3.2
        }
        if (prop == null) {
            return false;
        }
        return prop.indexOf(_name) > -1;
    }

    public void start() {
        try {
            _scope = new ObjectName(SCOPE);
        } catch (Exception e) {
            //aint gonna happen
            e.printStackTrace();
        }

        for (Iterator it = getIterator(); it.hasNext();) { 
            ObjectName obj = (ObjectName)it.next();

            try {
                if (!apply(obj)) {
                    continue;
                }

                _server.addNotificationListener(obj, this, null, null);
                _numModules++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        for (Iterator it = getIterator(); it.hasNext();) { 
            ObjectName obj = (ObjectName)it.next();

            try {
                if (!apply(obj)) {
                    continue;
                }
                _server.removeNotificationListener(obj, this);
            } catch (Exception e) {
            }
        }

        _notifyStopped  = false;
        _running        = false;
        _numModules     = 0;
        _startedModules = 0;
    }

    public void handleNotification(Notification notification, Object handback) {
        if (!(notification instanceof AttributeChangeNotification)) {
            //we should never get anything but.
            return;
        }

        AttributeChangeNotification attrChange = 
            (AttributeChangeNotification)notification;

        if (!attrChange.getAttributeName().equals("State")) {
            return;
        }

        int state = ((Integer)attrChange.getNewValue()).intValue();

        switch (state) {
        case SERVICE_STOPPED:
            if (!_notifyStopped) {
                _running = false;
                _notifyStopped = true;
                _listener.ejbModuleStopped();
            }
            return;
        case SERVICE_STARTED:
            //fallthrough
            break;
        default:
            return;
        }

        if (++_startedModules != _numModules) {
            return;
        }

        _running = true;

        _listener.ejbModuleStarted();
    }
}
