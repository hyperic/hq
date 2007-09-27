// NOTE: This copyright does *not* cover user programs that use HQ
// program services by normal system calls through the application
// program interfaces provided as part of the Hyperic Plug-in Development
// Kit or the Hyperic Client Development Kit - this is merely considered
// normal use of the program, and does *not* fall under the heading of
// "derived work".
// 
// Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
// This file is part of HQ.
// 
// HQ is free software; you can redistribute it and/or modify
// it under the terms version 2 of the GNU General Public License as
// published by the Free Software Foundation. This program is distributed
// in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
// even the implied warranty of MERCHANTABILITY or FITNESS FOR A
// PARTICULAR PURPOSE. See the GNU General Public License for more
// details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA.

var overlay = {
    curTime : null,
    times: new Array(),

    findPosX: function (obj) {
        var curleft = 0;
        if (obj.offsetParent) {
            while (obj.offsetParent) {
                curleft += obj.offsetLeft
                obj = obj.offsetParent;
            }
        }
        else if (obj.x)
            curleft += obj.x;

        return curleft;
    },

    findPosY: function (obj) {
        var curtop = 0;
        if (obj.offsetParent) {
            while (obj.offsetParent) {
                curtop += obj.offsetTop
                obj = obj.offsetParent;
            }
        }
        else if (obj.y)
            curtop += obj.y;

            return curtop;
    },

    moveOverlay: function (anchor) {
        var ovl = $('overlay');
        var anchorY = this.findPosY(anchor);
        var left = this.findPosX(anchor);

        var top = this.findPosY($('charttop')) - 4;

        var bottom = this.findPosY($('timetop')) + 4;

        ovl.style.visibility='visible';
        ovl.style.left = left + 'px';
        ovl.style.top = top + 'px';
        ovl.style.height = (bottom - top) + 'px';
    },

    showTimePopup: function (index, e) {
        if (this.curPopup != null) {
            this.curPopup.style.visibility='hidden';
        }
        var anchor = $('timePopup_' + index);
        var left = this.findPosX(anchor) - 35;
        var top = this.findPosY(anchor) + 10;
        this.curPopup = $('timePopup');
        this.curPopup.innerHTML = this.times[index];
        this.curPopup.style.left = left + 'px';

        if (e)
            this.curPopup.style.top = e.pageY + 'px';
        else
            this.curPopup.style.top = top + 'px';

        new Rico.Effect.FadeTo(this.curPopup, 0, 1, 1, null);
        this.curPopup.style.visibility ='visible';
        this.fadeInTimePopup();

        if (e) {
            setTimeout("overlay.moveTimePopup(" + top + ")", 2500);
        }
    },

    showTimePopupTopMetricChart: function (index, e) {
        if (this.curPopup != null) {
            this.curPopup.style.visibility='hidden';
        }
        var anchor = $('timePopup_' + index);
        var left = this.findPosX(anchor) - 35;
        var top = this.findPosY(anchor) + 10;
        this.curPopup = $('timePopup');
        this.curPopup.innerHTML = this.times[index];
        this.curPopup.style.left = left + 'px';

        if (e)
            this.curPopup.style.top = 290 + 'px';
        else
            this.curPopup.style.top = top + 'px';

        new Rico.Effect.FadeTo(this.curPopup, 0, 1, 1, null);
        this.curPopup.style.visibility ='visible';
        this.fadeInTimePopup();

        if (e) {
            setTimeout("overlay.moveTimePopup(" + top + ")", 2500);
        }
    },

    fadeInTimePopup: function (top) {
        if (top != null)
            this.curPopup.style.top = top + 'px';
        new Rico.Effect.FadeTo(this.curPopup, 1, 1000, 10, null);
    },

    moveTimePopup: function (top) {
        if (this.curPopup.style.top != (top + 'px')) {
            new Rico.Effect.FadeTo(this.curPopup, 0, 1000, 10, null);
            setTimeout("overlay.fadeInTimePopup(" + top + ")", 1000);
        }
    },

    hideTimePopup: function () {
        if (this.curPopup != null) {
            this.curPopup.style.visibility='hidden';
        }
    },

    delayTimePopup: function (time, index) {
      this.curTime = time;
      setTimeout("overlay.showCurrentTimePopup(" + time + "," + index + ")" ,
                 1000);
    },

    showCurrentTimePopup: function (time, index) {
      if (this.curTime == time)
        this.showTimePopup(time);
    }
}

/*************************************************************************
  This code is from Dynamic Web Coding at http://www.dyn-web.com/
  Copyright 2003 by Sharon Paine 
  See Terms of Use at http://www.dyn-web.com/bus/terms.html
  regarding conditions under which you may use this code.
  This notice must be retained in the code as is!
*************************************************************************/

var viewport = {
  getWinWidth: function () {
    this.width = 0;
    if (window.innerWidth) this.width = window.innerWidth - 18;
    else if (document.documentElement && document.documentElement.clientWidth)
        this.width = document.documentElement.clientWidth;
    else if (document.body && document.body.clientWidth)
        this.width = document.body.clientWidth;
  },

  getWinHeight: function () {
    this.height = 0;
    if (window.innerHeight) this.height = window.innerHeight - 18;
    else if (document.documentElement && document.documentElement.clientHeight)
        this.height = document.documentElement.clientHeight;
    else if (document.body && document.body.clientHeight)
        this.height = document.body.clientHeight;
  },

  getScrollX: function () {
    this.scrollX = 0;
    if (typeof window.pageXOffset == "number") this.scrollX = window.pageXOffset
;
    else if (document.documentElement && document.documentElement.scrollLeft)
        this.scrollX = document.documentElement.scrollLeft;
    else if (document.body && document.body.scrollLeft)
        this.scrollX = document.body.scrollLeft;
    else if (window.scrollX) this.scrollX = window.scrollX;
  },

  getScrollY: function () {
    this.scrollY = 0;
    if (typeof window.pageYOffset == "number") this.scrollY = window.pageYOffset
;
    else if (document.documentElement && document.documentElement.scrollTop)
        this.scrollY = document.documentElement.scrollTop;
    else if (document.body && document.body.scrollTop)
        this.scrollY = document.body.scrollTop;
    else if (window.scrollY) this.scrollY = window.scrollY;
  },

  getAll: function () {
    this.getWinWidth(); this.getWinHeight();
    this.getScrollX();  this.getScrollY();
  }

}

var menuLayers = {
  timer: null,
  activeMenuID: null,
  offX: 3,   // horizontal offset 
  offY: 5,   // vertical offset 

  show: function(id, e) {
    var mnu = $? $(id): null;
    if (!mnu) return;
    this.activeMenuID = id;
    if ( mnu.onmouseout == null ) mnu.onmouseout = this.mouseoutCheck;
    if ( mnu.onmouseover == null ) mnu.onmouseover = this.clearTimer;
    viewport.getAll();
    this.position(mnu, e);
  },
  
  hide: function() {
    this.clearTimer();
    if (this.activeMenuID && $) 
      this.timer = setTimeout("$('"+menuLayers.activeMenuID+"').style.visibility = 'hidden'", 200);
  },
  
  position: function(mnu, e) {
    var x = e.pageX? e.pageX: e.clientX + viewport.scrollX;
    var y = e.pageY? e.pageY: e.clientY + viewport.scrollY;
    
    //if ( x + mnu.offsetWidth + this.offX > viewport.width + viewport.scrollX )
      x = x - mnu.offsetWidth - this.offX;
    //else x = x + this.offX;
  
    if ( y + mnu.offsetHeight + this.offY >
         viewport.height + viewport.scrollY )
      y = ( y - mnu.offsetHeight - this.offY > viewport.scrollY )?
          y - mnu.offsetHeight - this.offY :
          viewport.height + viewport.scrollY - mnu.offsetHeight;
    else y = y + this.offY;

    this.timer = setTimeout("$('" + menuLayers.activeMenuID + "').style.visibility = 'visible'", 200);
    mnu.style.left = x + "px"; mnu.style.top = y + "px";
  },
  
  mouseoutCheck: function(e) {
    e = e? e: window.event;
    // is element moused into contained by menu? or is it menu (ul or li or a to menu div)?
    var mnu = $(menuLayers.activeMenuID);
    var toEl = e.relatedTarget? e.relatedTarget: e.toElement;
    if ( mnu != toEl && !menuLayers.contained(toEl, mnu) ) menuLayers.hide();
  },
  
  // returns true of oNode is contained by oCont (container)
  contained: function(oNode, oCont) {
    if (!oNode) return; // in case alt-tab away while hovering (prevent error)
    while ( oNode = oNode.parentNode ) 
      if ( oNode == oCont ) return true;
    return false;
  },

  clearTimer: function() {
    if (menuLayers.timer) clearTimeout(menuLayers.timer);
  }
}

