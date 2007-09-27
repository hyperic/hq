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

package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.pager.PageFetcher;
import org.hyperic.util.pager.PageList;

import java.util.ArrayList;
import java.util.List;

/**
 * The value printer page fetcher.  Beware all ye who enter here -- there
 * be dragons, and strange math (which is probably also incorrect).
 *
 * This PageFetcher uses a real page fetcher + value printer to 
 * make PageLists which represent the formatted data contained within
 * the real fetcher.
 */
public class ValuePrinterPageFetcher
    extends PageFetcher
{
    private PageFetcher  fetcher;
    private ValuePrinter printer;
    private int          headerLength;
    private List         headers;

    public ValuePrinterPageFetcher(PageFetcher fetcher, ValuePrinter printer){
        this.init(fetcher, printer);
    }

    private void init(PageFetcher fetcher, ValuePrinter printer){
        this.fetcher      = fetcher;
        this.printer      = printer;
        this.headers      = this.printer.getList(new ArrayList(), true);
        this.headerLength = this.headers.size();
    }


    public PageList getPage(PageControl control)
        throws PageFetchException
    {
        PageList res, data;
        boolean fetched = false, trimFront;
        int pageNo;

        if(control.getPagesize() == PageControl.SIZE_UNLIMITED ||
           control.getPagenum() == -1)
        {
            control.setPagesize(Integer.MAX_VALUE);
            control.setPagenum(0);
        }

        res = new PageList();

        trimFront = true;
        // Print headers if we need to
        if(this.headerLength > control.getPageEntityIndex()){
            for(int i=control.getPageEntityIndex(); i < headerLength; i++){
                res.add(this.headers.get(i));
            }
        } 

        // Now find the first page we need to start adding 
        // to the end of the pageList
        pageNo = control.getPagenum() - 
            (this.headerLength / control.getPagesize()) - 1;
        if(pageNo < 0){
            // Did we print out the headers already?  If so, no trimmy
            if(pageNo < 0)
               pageNo = 0;
            trimFront = false;
        }

        // Bizarre case when we lie on the page boundary because of the
        // headers
        if((this.headerLength % control.getPagesize()) == 0 &&
           control.getPagenum() > (this.headerLength / control.getPagesize()))
        {
            trimFront = false;
            pageNo++;
        }

        while(res.size() < control.getPagesize()){
            boolean gotShortPage;
            
            fetched = true;
            control.setPagenum(pageNo++);
            
            data = this.fetcher.getPage(control);
            gotShortPage = data.size() < control.getPagesize();
            if(trimFront == true){
                int numToTrim;

                numToTrim = control.getPagesize() - 
                    (this.headerLength % control.getPagesize());
                while(numToTrim-- != 0 && data.size() > 0){
                    data.remove(0);
                }
                trimFront = false;
            }
            res.setTotalSize(data.getTotalSize() + this.headerLength);
            res.addAll(this.printer.getList(data, false));
            
            // If the page didn't return enough data, break out
            // since we must be on the last page.
            if(gotShortPage)
                break;
        }

        if(res.size() > control.getPagesize()){
            int numToRemove = res.size() - control.getPagesize();

            while(numToRemove-- != 0){
                res.remove(res.size() - 1);
            }
        }

        /* If we never did a fetch, we need to, since we need
           to report an accurate number of total lines */
        if(!fetched){
            control.setPagenum(1);
            data = this.fetcher.getPage(control);
            res.setTotalSize(data.getTotalSize() + this.headerLength);
        }
        return res;
    }
}
