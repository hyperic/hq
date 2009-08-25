/**
 * This utility class provides methods for creating web-widgets programatically.
 * The primary goal is to share functionality between views.
 */

package org.hyperic.hq.hqu.rendit.html

class HQUWebUtil {
    /**
     * Create a TwoPanedFilterBuilder.  
     *
     * In myView.gsp:
     *    <% hquTwoPanedFilter { w -> 
              w.filter('My Filter Box') {
                  w.filterElement('First Filter') {
                      %> some HTML <% 
                  }
                  w.filterElement('Second Filter') {
                      %> more HTML <%
                  }
              }
              w.pane {
                  %> right-side-of-the-window stuff <%
              }
           } %>
     *
     */
    static hquTwoPanedFilter(Binding b, Closure yield) {
        def output = b.PAGE.getOutput()
        def builder = new TwoPanedFilterBuilder(output:output)
        builder.filterAndPane {
            yield(builder)
        }
    }
}