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

/*-- START diagram.js --*/

var diagShown = false;
var diagramDiv;
var toolsShown = false;

function toggleDiagram(eId) {
  var thisDiv = $(eId);
  
  if ( diagShown ) {
    hideDiagram(thisDiv);
  } else {
    showDiagram(thisDiv);
  }
}

function hideDiagram(thisDiv) {
  new Effect.SlideUp(thisDiv, { duration: 0.2 });
  showFormElements();
  diagShown = false;
}

function showDiagram(thisDiv) {
  new Effect.SlideDown(thisDiv, { duration: 0.2 });
  hideFormElements();

  diagShown = true;

  diagramDiv = thisDiv;
  setTimeout("makeDiagramVisible()", 200);
}

function makeDiagramVisible(eId) {
  diagramDiv.style.visibility = "visible";
}

function show(menu) {
  new Effect.SlideDown(menu, { duration: 0.2 });
  menu.shown = true;
}

function hide(menu) {
  new Effect.SlideUp(menu, { duration: 0.2 });
  menu.shown = false;
}

function toggleMenu(menuId) {
  var menu = $(menuId);
  if ( menu.shown ) {
    hide(menu);
  }
  else {
    show(menu);
  }
}

// We register this body.onclick handler within this javascript file
// so that the onclick happens only on pages that have the diagram.
// This function should work in all IE and Gecko-based browsers.
function bodyClicked(e) {
  if (!e) {
    if (window.event) {
      e = window.event;
    }
  }

  var target = null;
  if (e.target) {
    target = e.target;
  } else if (e.srcElement) {
    target = e.srcElement;
  }

  if ( diagShown && (!target || ('navMapIcon' != target.name &&
                                 'navMapImage' != target.name)) ) {
    hideDiagram('diagramDiv');
  }

  if ( $('toolMenu') && (!target.id || target.id != 'toolMenuSpan') &&
       $('toolMenu').shown ) {
    hide($('toolMenu'));
  }

  if ( (!target.id || target.id != 'recentImg') && $('recent') && $('recent').shown ) {
    hide($('recent'));
  }
}
dojo.connect(dojo.body(), "onclick", window, bodyClicked);
//document.body.onclick = bodyClicked;

/*-- END diagram.js --*/
