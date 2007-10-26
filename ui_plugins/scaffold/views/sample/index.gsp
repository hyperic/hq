<p>
    ${l.Congrats}!  
</p>
<p>
     Plugin Name: <b><%= plugin.name %></b><br/>
     Description: <b><%= plugin.description %></b><br/>
     Version: <b><%= plugin.descriptor.get('plugin.version') %></b><br/>
</p>
<p>
    You should rename your controller from 
    <b>@NEW_PLUGIN_PATH@/app/SampleController.groovy</b> to
    something cooler, like <b>ConsoleController.groovy</b>
</p>
<p>
    You'll also want to change the following files:
    <ul>
        <li><b>@NEW_PLUGIN_PATH@/etc/@PLUGIN_NAME@_i18n.properties</b> contains 
               the description (which is currently <i><%= plugin.description %></i>)</li>
    </ul>
</p>
