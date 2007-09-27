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

/*-- START alertConfigFunctions.js --*/
function selectMetric(selName, hidName) {
  var sel = document.getElementsByName(selName)[0];
  var selValue = sel[sel.selectedIndex].value;
  var selText = sel[sel.selectedIndex].text;

  document.getElementsByName(hidName)[0].value = selText;
}

function changeDropDown (masterSelName, selName, selDD, baselineOption) {
  var masterSel = document.getElementsByName(masterSelName)[0];
  var sel = document.getElementsByName(selName)[0];

  var masterSelValue = masterSel[masterSel.selectedIndex].value;
/*
  alert("masterSel: " + masterSel +
        "\nselName: " + selName +
        "\nsel: " + sel +
        "\nbaselineOption: " + baselineOption +
        "\nmasterSelValue: " + masterSelValue);
*/
  if (masterSelValue == '') {
    sel.disabled = true;
  } else {
    sel.options.length = 0;
    sel.options.length = baselines[masterSelValue].length;

    if (isIE) {
      sel.options[0].text = selDD;
      sel.options[0].value = '';
    } else {
      sel.options[0] = new Option(selDD, '');
    }
    sel.options[0].selected = true;
    for (i=0; i<baselines[masterSelValue].length-1; i++) {
      if (isIE) {
        sel.options[i+1].text = baselines[masterSelValue][i].label;
        sel.options[i+1].value = baselines[masterSelValue][i].value;
      } else {
        sel.options[i+1] = new Option(baselines[masterSelValue][i].label, baselines[masterSelValue][i].value);
      }
      if (baselineOption != null) {
        if (sel.options[i+1].value == baselineOption) {
          sel.options[0].selected = false;
          sel.options[i+1].selected = true;
        }
      }
    }
    sel.disabled = false;
  }
}

function checkEnable() {
  if (document.forms[0].whenEnabled[0].checked == true) {
    document.forms[0].meetTimeTP.value = "";
    document.forms[0].howLongTP.value = "";
    document.forms[0].numTimesNT.value = "";
    document.forms[0].howLongNT.value = "";
  } else if (document.forms[0].whenEnabled[1].checked == true) {
    document.forms[0].numTimesNT.value = "";
    document.forms[0].howLongNT.value = "";
  } else if (document.forms[0].whenEnabled[2].checked == true) {
    document.forms[0].meetTimeTP.value = "";
    document.forms[0].howLongTP.value = "";
  }
}
function checkEnableTP() {
  document.forms[0].whenEnabled[0].checked = false;
  document.forms[0].whenEnabled[1].checked = true;
  document.forms[0].whenEnabled[2].checked = false;
  document.forms[0].numTimesNT.value = "";
  document.forms[0].howLongNT.value = "";
}
function checkEnableNT() {
  document.forms[0].whenEnabled[0].checked = false;
  document.forms[0].whenEnabled[1].checked = false;
  document.forms[0].whenEnabled[2].checked = true;
  document.forms[0].meetTimeTP.value = "";
  document.forms[0].howLongTP.value = "";
}

function checkRecover() {
  if (document.forms[0].disableForRecovery.checked == true) {
    document.forms[0].recoverId.disabled = true;
  }
  else {
    document.forms[0].recoverId.disabled = false;
  }

  if (document.forms[0].recoverId.value == '') {
    document.forms[0].disableForRecovery.disabled = false;
  }
  else {
    document.forms[0].disableForRecovery.disabled = true;
  }
}

function syslogFormEnabledToggle() {
  if (document.forms[0].shouldBeRemoved.checked) {
    document.forms[0].metaProject.disabled = true;
    document.forms[0].project.disabled = true;
    document.forms[0].version.disabled = true;
  } else {
    document.forms[0].metaProject.disabled = false;
    document.forms[0].project.disabled = false;
    document.forms[0].version.disabled = false;
  }
}
/*-- END alertConfigFunctions.js --*/
