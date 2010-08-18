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

package org.hyperic.hq.hqu.rendit.html

/**
 * A TwoPanedFilter is a builder which allows for a filter, 
 * filter-elements, and a content pane.
 *
 *  It contains a small left-column (where filters are) and a large
 *  right column (the content pane)
 *
 * To use it:
 *  def x = new TwoPanedFilterBuilder(output) 
 *  x.filterAndPane {
      x.filters('Filter Box') {
          x.filterElement('Element one') {
              output.write('Body of one')
          }
          x.filterElement('Element two') {
              output.write('Body of two')
          }
      }
    x.pane {
        output.write('Body of pane')
    }
 }
 *
 *
 */
class TwoPanedFilterBuilder extends BuilderSupport {
    def output
    
    def createNode(name, value) {
	    createNode(name, [label: value]);
    }
    
    def createNode(name) {
        createNode(name, 'unspecified') 
    }
    
    def createNode(name, Map attributes) {
	   if (attributes.labelMarkup == null) attributes.labelMarkup = '';
	   
       def res = [nodeType: name, name: attributes.label, markup: attributes.labelMarkup]
       
       if (name == 'filterAndPane') {
    	   output.write("""<div style="margin-bottom:5px;padding-right:10px;border:1px solid #7BAFFF; padding:10px;background-color:#fff">""")
           
    	   res.finish = { """</div>""" }
       } else if (name == 'filter') {
           output.write("""<div style="float:left;width:200px;margin-right:10px;"><div class="filters"><div class="BlockTitle">${res.name}</div><div class="filterBox">""")
           
           res.finish= { """</div></div></div>""" }
       } else if (name == 'filterElement') {
           output.write("""<div class="fieldSetStacked" style="margin-bottom:8px;"><span><strong>${res.name}</strong></span>${res.markup}<div>""")
            
           res.finish = { """</div></div>""" }
       } else if (name == 'pane') {
           output.write("""<div style="width:auto;height: 445px;overflow-x: hidden; overflow-y: auto;" id="logsCont"><div>""")
           
           res.finish = { """</div></div>""" }
       } else {
           throw new RuntimeException("Unknown type: [$name]")
       }

       res
    }
    
    def createNode(name, Map Attributes, value) {
        createNode(name)
    }
    
    void setParent(parent, child) {
        child.parent = parent
    }
    
    void nodeCompleted(parent, node) {
        output.write(node.finish())
    }
}
