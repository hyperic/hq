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

package org.hyperic.hq.ui.taglib;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ResourceTreeNode;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.beans.ImageMapRectAreaBean;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.servlet.NavMapImageServlet;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.image.widget.ResourceTree;
import org.hyperic.util.data.IResourceTreeNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * <p>A JSP tag that will retrieve a tree of all resources connected
 * to the current resource and store it in the session.  The "treeVar"
 * request attribute will hold the name of the session attribute
 * holding the ResourceTree data.</p>
 *
 * <p>Whatever is set as the areasVar will be set as request attribute
 * holding the image map &lt;area&gt; beans.  The number of image map
 * &lt;area&gt; beans will be stored in the request under whatever
 * wasareasSizeVar was set as.</p>
 *
 */
public class NavMapTag extends TagSupport {
    Log log = LogFactory.getLog( NavMapTag.class.getName() );
    private static final int MAX_HEIGHT = 600;

    //----------------------------------------------------instance variables

    private String areasVar;
    private String areasSizeVar;
    private String imageWidth;

    //----------------------------------------------------constructors

    public NavMapTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the request attribute that should hold the list
     * of ImageMapRectAreaBean objects.
     *
     * @param var the name of the request attribute
     */
    public void setAreasVar(String areasVar) {
        this.areasVar = areasVar;
    }
    
    /**
     * Set the name of the request attribute that should hold the size
     * of the list of ImageMapRectAreaBean objects.
     *
     * @param sizeVar the name of the request attribute
     */
    public void setAreasSizeVar(String areasSizeVar) {
        this.areasSizeVar = areasSizeVar;
    }
    
    /**
     * Set the width of the navigation map image.
     *
     * @param imageWidth the name of the request attribute
     */
    public void setImageWidth(String imageWidth) {
        this.imageWidth = imageWidth;
    }
    
    /**
     * Process the tag, generating appropriate HTML.
     *
     * @exception JspException if any errors occur
     */
    public final int doStartTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        ServletContext ctx = pageContext.getServletContext();

        // first try to get the resource ids as eids, then as rid / type
        AppdefEntityID[] eids = null;
        try {
            eids = RequestUtils.getEntityIds(request);
        } catch (ParameterNotFoundException e) {
            // either an auto-group of platforms or an individual resource
            try {
                eids =
                    new AppdefEntityID[] { RequestUtils.getEntityId(request) };
            } catch (ParameterNotFoundException pnfe) {
                // auto-group of platforms
                eids = null;
            }
        }
        Integer ctype = null;
        if (eids != null && eids.length > 0) {
            switch (eids[0].getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_GROUP :
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    ctype = null;
                    break;
                default :
                    ctype = RequestUtils.getIntParameter(request,
                                Constants.CHILD_RESOURCE_TYPE_ID_PARAM,
                                null);
                    break;
            }
        }

        Integer autogrouptype = RequestUtils.getIntParameter(request,
                Constants.AUTOGROUP_TYPE_ID_PARAM, null);

        if (autogrouptype != null)
            ctype = autogrouptype;

        // The tree variable for the session will be computed from the
        // eids, ctype and current timestamp.
        StringBuffer tvBuf = new StringBuffer("resourceTree_")
            .append( System.currentTimeMillis() ).append('_');
        if (null != eids) {
            for (int i=0; i<eids.length; ++i) {
                tvBuf.append( eids[i].getAppdefKey() ).append('_');
            }
        }

        if (null != ctype) {
            tvBuf.append('_').append(ctype);
        }
        
        String treeVar = tvBuf.toString();
        request.setAttribute(NavMapImageServlet.TREE_VAR_PARAM, treeVar);

        Integer imageWidth = (Integer) ExpressionUtil
                .evalNotNull("navMap", "imageWidth", this.imageWidth,
                             Integer.class, this, pageContext);
        try {
            ResourceTree tree = _getResourceTree( eids, ctype, imageWidth.intValue() );
            request.getSession().setAttribute(treeVar, tree);

            List areas = _getAreasForTree(tree);
            request.setAttribute(areasVar, areas);
            request.setAttribute(areasSizeVar, new Integer( areas.size() ) );
        } catch (Exception e) {
            log.debug("Error while getting tree.", e);
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    private List _getAreasForTree(ResourceTree tree) {
        IResourceTreeNode[][] levels = tree.getLevels();

        ArrayList areas = new ArrayList();
        for (int i=0; i<levels.length; ++i) {
            for (int j=0; j<levels[i].length; ++j) {
                ResourceTreeNode node = (ResourceTreeNode)levels[i][j];
                _addAreasForTreeNode(node, areas);
            }
        }

        return areas;
    }

    private void _addAreasForTreeNode(ResourceTreeNode node, List areas) {
        // Create <area> tag for parent node.  If the rectangle is
        // null, we won't create an <area> tag for it.
        Rectangle[] rects = node.getRectangles();
        if (null != rects) {
            for (int i=0; i<rects.length; ++i) {
                int x2 = (int)(rects[i].x + rects[i].width);
                int y2 = (int)(rects[i].y + rects[i].height);
                if ( node.getType() == org.hyperic.util.data.IResourceTreeNode.AUTO_GROUP ) {
                    areas.add( new ImageMapRectAreaBean( (int)rects[i].x,
                                                         (int)rects[i].y,
                                                         x2,
                                                         y2,
                                                         node.getEntityIds(),
                                                         node.getCtype(),
                                                         node.getName(),
                                                         // fix for autogroups
                                                         node.getREntityId().getType() + ":" + node.getCtype() ));
                } else if ( node.hasCtype() ) {
                    areas.add( new ImageMapRectAreaBean( (int)rects[i].x,
                                                         (int)rects[i].y,
                                                         x2,
                                                         y2,
                                                         node.getEntityIds(),
                                                         node.getCtype(),
                                                         node.getName() ) );
                } else {
                    areas.add( new ImageMapRectAreaBean( (int)rects[i].x,
                                                         (int)rects[i].y,
                                                         x2,
                                                         y2,
                                                         node.getEntityIds(),
                                                         node.getName() ) );
                }
            }
        }

        // Create <area> tags for child nodes.
        IResourceTreeNode[] children = (IResourceTreeNode[])node.getUpChildren();
        if (null != children) {
            for (int i=0; i<children.length; ++i) {
                ResourceTreeNode cnode = (ResourceTreeNode)children[i];
                _addAreasForTreeNode(cnode, areas);
            }
        }
        children = (IResourceTreeNode[])node.getDownChildren();
        if (null != children) {
            for (int i=0; i<children.length; ++i) {
                ResourceTreeNode cnode = (ResourceTreeNode)children[i];
                _addAreasForTreeNode(cnode, areas);
            }
        }
    }

    private ResourceTree _getResourceTree(AppdefEntityID[] eids, Integer ctype, int imageWidth)
        throws ServletException, SessionNotFoundException,
               SessionTimeoutException, AppdefEntityNotFoundException,
               PermissionException, RemoteException
    {
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        ServletContext ctx = pageContext.getServletContext();
        int sessionId = RequestUtils.getSessionId(request).intValue();
        AppdefBoss ab = ContextUtils.getAppdefBoss(ctx);

        ResourceTree tree = new ResourceTree(imageWidth);
        
        ResourceTreeNode[] data = null;
        if (null == ctype) {
            data = ab.getNavMapData(sessionId, eids[0]);
        } else {
            data = ab.getNavMapData( sessionId, eids, ctype.intValue() );
        }
        if (data != null) {
            tree.addLevel(data);
        }

        // We must call getImage() here because it initializes the
        // tree such that the coordinates for the nodes can be
        // computed.  A NullPointerException will result if getImage()
        // has not been called before calculateCoordinates().
        tree.getImage();
        tree.calculateCoordinates();

        // Ask the tree for the image dimensions and check them against
        // the max width and height parameters. If we're not within range
        // then auto-group any promotable nodes.
        Dimension dim = tree.getImageSize();
        if ( dim.getHeight() > MAX_HEIGHT ) {
            tree = new ResourceTree(imageWidth);
            ResourceTreeNode.autoGroupData(data); 
            tree.addLevel(data);
            tree.getImage();
            tree.calculateCoordinates();
        }
        return tree;

    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;        
    }

    public void release() {
        areasVar = null;
        areasSizeVar = null;
        super.release();
    }
}

// EOF
