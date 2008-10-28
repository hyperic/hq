<p>
    ${l.Congrats}!  
</p>
<p>
<table>
<tr>
     <td>Plugin Name</td><td> <b><%= plugin.name %></b></td>
</tr>
<tr>
     <td>Description</td><td><b><%= plugin.description %></b></td></tr>
</tr>
<tr>
     <td>Version</td><td><b><%= plugin.descriptor.get('plugin.version') %></b></td></tr>
</tr>
</p>
</table>

<p>
    Your username is ${userName}<br/>
</p>

<p>
    The method named 'index' in <b>app/SaaSCenterController.groovy</b> 
    was invoked to render this page.
</p>
<p>
    It then rendered <b>views/saasCenter/index.gsp</b> which you are reading.
</p>

<p> 
    You'll also want to change the following files:
    <ul>
        <li><b>/Users/rpack/Source/ee/hq/ui_plugins/saasCenter/etc/saasCenter_i18n.properties</b> contains 
               the description (which is currently <i><%= plugin.description %></i>)</li>
    </ul>
</p>
