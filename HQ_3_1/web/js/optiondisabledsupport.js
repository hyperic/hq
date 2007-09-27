/**
 * OptionDisabledSupport
 * by Kaleb Walton (http://toserveman.kalebwalton.com)
 * Inspired by techniques from Alistair Lattimore's article which can be found at
 * http://www.lattimore.id.au/2005/07/01/select-option-disabled-and-the-javascript-solution/
 *
 * Creative Commons License URL: http://creativecommons.org/licenses/by/2.5/
 */
var OptionDisabledSupport = new function() {
  this.previouslySelectedIndices = new Array();
  this.init = function() {
    selects = document.getElementsByTagName("select");
    for (var i=0;i<selects.length;i++) {
      var select = selects[i];
      var oldOnClick = select.onclick;
      var oldOnChange = select.onchange;
      select.onclick = null;
      select.onchange = null;
      // Must execute the old onclick/onchange handlers after this one.
      // For some reason IE reverses the order you specify.
      if (oldOnClick)
        select.attachEvent ('onclick',function(e) {oldOnClick.apply(e.srcElement, arguments)})
      if (oldOnChange)
        select.attachEvent ('onchange',function(e) {oldOnChange.apply(e.srcElement, arguments)})
      select.attachEvent ('onclick',function(event) {OptionDisabledSupport.previouslySelectedIndices[event.srcElement] = event.srcElement.selectedIndex;})
      select.attachEvent ('onchange',function(event) {OptionDisabledSupport.handleSelect(event.srcElement)})
      for (var z=0;z<select.options.length;z++) {
        option = select.options[z];
        option.style.color = option.disabled ? "graytext" : option.style.color;
      }
      OptionDisabledSupport.handleSelect(select);
    }
  }
  this.handleSelect = function(select) {
    if (select.multiple) {
      for (var i=0;i<select.options.length;i++) {
        option = select.options[i];
        option.selected = option.disabled && option.selected ? false : option.selected;
      }
    } else {
      if (select.selectedIndex > -1) {
        if (select.options[select.selectedIndex].disabled) {
          select.selectedIndex = OptionDisabledSupport.previouslySelectedIndices[select];
          if (select.options[select.selectedIndex].disabled) {
            select.selectedIndex = -1;
          }
        }
      }
    }
  }
}
if (window.attachEvent) window.onload = OptionDisabledSupport.init;