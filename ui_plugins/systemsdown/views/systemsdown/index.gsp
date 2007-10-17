 <script type="text/javascript">
      var openElement = "";
     function getData(type, data, evt) {
         if (data) {
         alert(data)
             var domTree = document.getElementById('resourceTree');
             var tree = "";
             for (var x = 0; x < data.length; x++) {
                 var parent = data[x]['parent'];
                 var children = data[x]['children'];
                 var innerChildren = "";
                 for (var i = 0; i < children.length; i++) {
                     innerChildren += Child(children[i]['name'], children[i]['url'], children[i]['id']);
                 }
                 tree +=  Parent(data[x]['parent'], data[x]['id'], children.length, innerChildren);
             }
             //set the entire tree contents here
             domTree.innerHTML = tree;
         }
     }
    

     var Parent = function(name, id, length, innerChildren) {
         var ret = '<div class="topCat" onclick="swapVis(this);disableSelection(this);" resourceType="'+id+'">'+name+" ("+length+")</div>"
             + '<div class="resourcetypelist" style="display:none">' ;
         ret += innerChildren;
         ret += "</div>";
         return ret;
     }

     var Child = function(name, id) {
         return '<div class="listItem" onclick="updateTable(this)" resourceId="' + id + '">' + name + '</div>';
     }


       function updateTable(obj) {
         if (obj.resourceId) {
             //bindurl
             //dojo.io.bind({url:url + "&resourceId=" + obj.resourceId, load:getData, method:"get", mimetype: "text/json-comment-filtered"});
              openItemrid =  obj.resourceId;
         } else if (obj.resourceType) {
             //dojo.io.bind({url:url + "&resourceType=" + obj.resourceType, load:getData, method:"get", mimetype: "text/json-comment-filtered"});
              openItemrtype = obj.resourceType;
             //bindurl
         }
     }
     
      /*
     function updateTable(obj) {
     var openItemrid;
     var openItemrtype;

     
         if (obj.resourceId) {
             //bindurl
             rid = obj.resourceId;
             openItemrid =  obj.resourceId;
         } else if (obj.resourceType) {
             rtype= obj.resourceType; 
             //bindurl
             openItemrtype = obj.resourceType;
         }
        systemsDownTable _refreshTable();
        dojo.io.cookie.deleteCookie('downresrid');
        dojo.io.cookie.deleteCookie('downresrtype');
        dojo.io.cookie.setCookie('downresrid', openItemrid);
        dojo.io.cookie.setCookie('downresrtype', openItemrtype);
        //var dd = dojo.io.cookie.getCookie("downresrid");
        //dojo.debug("dcdisplay cookie found with value:" + dd);
     }
     */

     dojo.io.bind({
         url: "/hqu/systemsdown/systemsdown/summary.hqu",
         load: getData,
         method: "get",
         mimetype: "text/json-comment-filtered"
     });


     function swapVis(elem) {
        var sib = elem.nextSibling;
       if(dojo.html.getStyleProperty(sib, 'display')=='none')
        sib.style.display='block';
         else
        sib.style.display='none';
     }

     function checkDisplay(el) {
         if (el.style.display == 'none') {
             el.style.display = '';
         } else {
             el.style.display = 'none';
         }
     }

     function disableSelection(element) {
         element.onselectstart = function() {
             return false;
         };
         element.unselectable = "on";
         element.style.MozUserSelect = "none";
         //element.style.cursor = "default";
     }

     function openAll() {
         var tree = document.getElementById('resourceTree');
         var x = tree.getElementsByTagName('div');
         for (var i = 0; i < x.length; i++) {
             if (x[i].className == 'resourcetypelist') {
                 x[i].style.display = '';
                // setselColor(x[i])
             }
         }
     }

     function closeAll() {
         var tree = document.getElementById('resourceTree');
         var x = tree.getElementsByTagName('div');
         for (var i = 0; i < x.length; i++) {
             if (x[i].className == 'resourcetypelist') {
                 x[i].style.display = 'none';
                 //setunselColor(x[i])
             }
         }
     }

     function setselColor(elem) {
         elem.style.backgroundColor = "#EEf3f3";
     }

     function setunselColor(elem) {
         elem.style.backgroundColor = "#ffffff";
     }


    // onloads.push(setSelectedOption);

</script>

<div dojoType="TabContainer" id="mainTabContainer"
     style="width: 100%; height:500px;">
  <div dojoType="ContentPane" label="SystemsDown">
    <div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
      <div style="float:left;width:18%;margin-right:10px;">
       <div class="leftbxblueborder">
            <div class="BlockTitle" style="text-align:left;">${l.ResType}</div>
                 <div>
                 <div onclick="" style="padding:3px 3px 0px 5px;float:left;display:inline;"><a href="javascript:closeAll();">${l.CollAll}</a></div><div style="padding:3px 5px 0px 3px;float:right;display:inline;"><a href="javascript:openAll();">${l.DispAll}</a></div>
                  <div style="clear:both;height:1px;"></div>
                 </div>
               <div class="leftbxht">
                    <div style="padding:3px;">
                        <div class="tree" id="resourceTree" >
                        /*
                        <div id="platforms" class="topCat" onclick="swapVis(this.id);disableSelection(this);"></div>
                        <div id="platlist" class="resourcetypelist" style="display:none;">
                            <ul style="list-style: none;margin-left: 0;padding-left: 1em;text-indent: 0.2em;">
                                <li onclick="disableSelection(this);">Linux</li>
                            </ul>
                        </div>
                       */
                    </div>
                </div>
               </div>
            </div>
      </div>
      <div style="float:right;width:78%;display:inline;height: 445px;overflow-x: hidden; overflow-y: auto;" id="downCont">
        <div id="systemsDownTable">
        <%= dojoTable(id:'SystemsDown', title:l.SystemsDownTitle,
                refresh:60, pageControls:false, url:urlFor(action:'data'),
                schema:systemsDownSchema, numRows:100) %>
           </div>
      </div>
    </div>
<div style="clear:both;height:1px;"></div>
  </div>
</div>

</script>
