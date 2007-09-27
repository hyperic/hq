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

package org.hyperic.hq.ui.action.portlet.controlactions;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm  {
    
    /** Holds value of property lastCompleted. */
    private Integer _lastCompleted;
    
    /** Holds value of property nextScheduled. */
    private Integer _nextScheduled;
    
    /** Holds value of property mostFrequent. */
    private Integer _mostFrequent;
    
    /** Holds value of property useLastCompleted. */
    private boolean _useLastCompleted;
    
    /** Holds value of property useMostFrequent. */
    private boolean _useMostFrequent;
    
    /** Holds value of property useNextScheduled. */
    private boolean _useNextScheduled;
    
    /** Holds value of property past. */
    private long _past;
    
    //-------------------------------------instance variables

    //-------------------------------------constructors

    public PropertiesForm() {
        super();
}

    //-------------------------------------public methods


    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        super.reset(mapping, request);
        //xxx figure this out
        setUseLastCompleted(false);
        setUseMostFrequent(false);
        setUseNextScheduled(false);
    }

    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        /* only validate if 1) the form's ok button was clicked and 2)
         * the mapping specifies an input form to return to.
         *
         * condition #2 can be false when a form has failed validation
         * and has forwarded to the input page; the ok button request
         * parameter will still be set, but the prepare action for the
         * input page will not have (another) input page specified.
         */

        // if (! isOk() &&
        // XXX: remove when ImageBeanButton is working
        if (RequestUtils.isOkClicked(request) &&
            mapping.getInput() != null) {
            return super.validate(mapping, request);
        }

        return null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }
    
    /** Getter for property lastCompleted.
     * @return Value of property lastCompleted.
     *
     */
    public Integer getLastCompleted() {
        return _lastCompleted;
    }
    
    /** Setter for property lastCompleted.
     * @param lastCompleted New value of property lastCompleted.
     *
     */
    public void setLastCompleted(Integer lastCompleted) {
        _lastCompleted = lastCompleted;
    }
    
    /** Getter for property lastScheduled.
     * @return Value of property lastScheduled.
     *
     */
    public Integer getNextScheduled() {
        return _nextScheduled;
    }
    
    /** Setter for property lastScheduled.
     * @param lastScheduled New value of property lastScheduled.
     *
     */
    public void setNextScheduled(Integer nextScheduled) {
        _nextScheduled = nextScheduled;
    }
    
    /** Getter for property mostFrequent.
     * @return Value of property mostFrequent.
     *
     */
    public Integer getMostFrequent() {
        return _mostFrequent;
    }
    
    /** Setter for property mostFrequent.
     * @param mostFrequent New value of property mostFrequent.
     *
     */
    public void setMostFrequent(Integer mostFrequent) {
        _mostFrequent = mostFrequent;
    }
    
    /** Getter for property useLastCompleted.
     * @return Value of property useLastCompleted.
     *
     */
    public boolean isUseLastCompleted() {
        return _useLastCompleted;
    }
    
    /** Setter for property useLastCompleted.
     * @param useLastCompleted New value of property useLastCompleted.
     *
     */
    public void setUseLastCompleted(boolean useLastCompleted) {
        _useLastCompleted = useLastCompleted;
    }
    
    /** Getter for property useMostFrequent.
     * @return Value of property useMostFrequent.
     *
     */
    public boolean isUseMostFrequent() {
        return _useMostFrequent;
    }
    
    /** Setter for property useMostFrequent.
     * @param useMostFrequent New value of property useMostFrequent.
     *
     */
    public void setUseMostFrequent(boolean useMostFrequent) {
        _useMostFrequent = useMostFrequent;
    }
    
    /** Getter for property useNextScheduled.
     * @return Value of property useNextScheduled.
     *
     */
    public boolean isUseNextScheduled() {
        return _useNextScheduled;
    }
    
    /** Setter for property useNextScheduled.
     * @param useNextScheduled New value of property useNextScheduled.
     *
     */
    public void setUseNextScheduled(boolean useNextScheduled) {
        _useNextScheduled = useNextScheduled;
    }
    
    public long getPast() {
        return _past;
    }
    
    public void setPast(long past) {
        _past = past;
    }
}
