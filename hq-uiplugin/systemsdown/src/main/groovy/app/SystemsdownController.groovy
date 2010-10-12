/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

import java.text.DateFormat
import java.util.Locale
import org.hyperic.util.units.FormatSpecifics
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.appdef.server.session.DownResSortField

class SystemsdownController extends BaseController {
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)

    private getAlertListImg() {
        def imgUrl = urlFor(asset:'images') +
            "/icon_zoom.gif"
        """<img src="${imgUrl}" width="16" height="16" border="0"
                class="alertListIcon" title="Click to go to the alert list for this resource">"""
    }

    private final SYSTEMSDOWN_SCHEMA = [
        getData: {pageInfo, params -> 
            resourceHelper.getDownResources(params.getOne('typeId'), pageInfo)
        },
        defaultSort: DownResSortField.DOWNTIME,
        defaultSortOrder: 1,  // descending
        columns: [
            [field:DownResSortField.RESOURCE, width:'37%',
             label:{linkTo(it.name,
                           [resource:it.resource.entityId])}],
            [field:DownResSortField.TYPE, width:'30%',
             label:{it.type}],
            [field:DownResSortField.SINCE, width:'15%',
             label:{df.format(it.timestamp)}],
            [field:DownResSortField.DOWNTIME, width:'13%',
             //label:{formatDuration(it.duration)}
              label:{formatDuration(it.duration)}
              ],
            [field:DownResSortField.ALERTS, width:'5%',
             label:{
             linkTo(getAlertListImg(), [resource:it,rawLabel:true])
             }],
        ]
    ]
   
   def SystemsdownController() {
        setTemplate('standard')
    }

    boolean logRequests() {
        false
    }    

    def getNow() {
        System.currentTimeMillis()
    }

    def formatDuration(d) {
        return UnitsFormat.format(new UnitNumber(d, UnitsConstants.UNIT_DURATION,
                                                 UnitsConstants.SCALE_MILLI),
                                  Locale.getDefault(), null).toString()
    }

    def index(params) {
    	render(locals:[ systemsDownSchema : SYSTEMSDOWN_SCHEMA, numRows : params.numRows])
    }

    def data(params) {
        def json = DojoUtil.processTableRequest(SYSTEMSDOWN_SCHEMA , params)
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }

    def getTypeJSON(type, count) {
        def json = ""
        if (type != null) {
            json += "{name: \"" + type.name + "\", id: \""
            json += type.appdefType + ":" + type.id + "\", count: " + count + "}"
        }
        return json
    }

    def summary(params) {
        def map = resourceHelper.downResourcesMap.entrySet()

        def json = "[\n"

        def appdefType = 1
        def first = true
        map.each { entry ->
            def list = entry.value

            if (list.size() > 0) {
	            if (first) {
	            	first = false
	            }
	            else {
	                json += ",\n"
	            }
	
                json += "{parent: \"" + entry.key + "\",\n" +
                        "id: " + appdefType + ",\n" +
                        "count: " + list.size() + ",\n" +
                        "children:[\n"
    
                def previous = null
                def count = 0
    
                list.each { type ->
                    if (previous == null || previous.name != type.name) {
                        json += getTypeJSON(previous, count)

                        if (previous != null) {
                            json += ",\n"
                        }

                        previous = type
                        count = 0
                    }
    
                    count++
                }

                json += getTypeJSON(previous, count)
                json += "]\n}"
            }
            appdefType++
        }

        json += "]"
		render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }
}
