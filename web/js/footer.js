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

/*-- START footer.js --*/

var conH;
var winH;
var footerH = 28;
var browserH = 88;
var aboutShown = false;

function setFoot() {
  if (isIE) {
    conH = document.body.scrollHeight;
    winH = document.body.clientHeight;
  }
  
  else {
    conH = document.height;
    winH = window.innerHeight;
  }

  var myHeight = winH - conH - footerH + browserH;
  if (myHeight > 60) {
    var footerSpacer = document.getElementById("footerSpacer");
    footerSpacer.setAttribute('height', myHeight);
  }
}

function hideAbout() {
    var about = $('about');
    var anchor = $('aboutAnchor');
    var x = anchor.offsetLeft;
    var y = anchor.offsetTop - about.offsetHeight + anchor.offsetHeight;
    new Rico.Effect.Position( about,
                              x,
                              y,
                              0,
                              1, // 1 steps
                              {}
                             );
    new Effect.Fade(about, {duration: 0});
    about.style.visibility = "visible";
}

function about() {
    var about = $('about');
    new Effect.Appear( about, {to: 0.85} );
    aboutShown = true;

  if (typeof(window['diagShown']) != 'undefined') {
    hideFormElements();
  }
}

function closeAbout(e) {
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

  if (aboutShown && (!target || "aboutLink" != target.name )) {
    new Effect.Fade( 'about' );
    aboutShown = false;
  }

  if (typeof(window['diagShown']) != 'undefined') {
      bodyClicked(e);
      showFormElements();
  }
}

document.body.onclick = closeAbout;

/*-- END footer.js --*/
