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
	   if(attributes.labelMarkup == null)
	       attributes.labelMarkup = '';
        def res = [nodeType: name, name: attributes.label, markup: attributes.labelMarkup]
        if (name == 'filterAndPane') {
            output.write("""
    <div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">""")
            res.finish = { """
    </div>
"""  }
        } else if (name == 'filter') {
            output.write("""
        <div style="float:left;width:200px;margin-right:10px;">
          <div class="filters">
            <div class="BlockTitle">${res.name}</div>
            <div class="filterBox">""")
            res.finish= { """
            </div>
          </div>
        </div>
""" }
        } else if (name == 'filterElement') {
            output.write("""
             <div class="fieldSetStacked" style="margin-bottom:8px;">
               <span><strong>${res.name}</strong></span>${res.markup}
               <div>""")
            res.finish = { """
     	       </div>
             </div>          
""" }
        } else if (name == 'pane') {
            output.write("""
        <div style="width:81%;height: 445px;overflow-x: hidden; overflow-y: auto;" 
             id="logsCont">
          <div>""")
            res.finish = { """
          </div>
        </div>
""" }
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
