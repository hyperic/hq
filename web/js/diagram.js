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

function toggleDiagram(eId) {
  var thisDiv = document.getElementById(eId);
  
  if ( thisDiv.style.visibility == "hidden") {
    showDiagram(eId);
  } else {
    hideDiagram(eId);
  }
}

function hideDiagram(eId) {
  var thisDiv = document.getElementById(eId);
  
  thisDiv.style.visibility = "hidden";  
  showFormElements();
  
}

function showDiagram(eId) {
  var thisDiv = document.getElementById(eId);

  thisDiv.style.visibility = "visible";
  hideFormElements();
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

  if ( !target || ('navMapIcon' != target.name && 'navMapImage' != target.name) ) {
    hideDiagram('diagramDiv');
  }
}
document.body.onclick = bodyClicked;

/*-- END diagram.js --*/
