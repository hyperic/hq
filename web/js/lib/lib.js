function fieldFocus(elem){
    if(!elem.getAttribute('readonly')){
        if(elem.parentNode.className == "fieldRow hint")
            elem.parentNode.className = "fieldRow hint active";
        else
            elem.parentNode.className = "fieldRow active";
    }
}
function fieldBlur(elem){
    if(elem.parentNode.className == "fieldRow hint active")
        elem.parentNode.className = "fieldRow hint";
    else
        elem.parentNode.className = "fieldRow";
}