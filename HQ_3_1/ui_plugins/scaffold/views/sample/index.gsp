<p>
    ${l.Congrats}!  
</p>
<p>
     Plugin Name: <b><%= pluginInfo.name %></b><br/>
     Description: <b><%= pluginInfo.description %></b><br/>
     Version: <b><%= pluginInfo.version%></b><br/>
     Dumping Scripts: <b><%= pluginInfo.dumpScripts %></b><br/>
</p>
<p>
    You should rename your controller from 
    <b>@NEW_PLUGIN_PATH@/app/SampleController.groovy</b> to
    something cooler, like <b>ConsoleController.groovy</b>
</p>
<p>
    You'll also want to change init.groovy at <b>@NEW_PLUGIN_PATH@</b> to use
    a better description.  (which is currently 
    <i><%= pluginInfo.description %></i>)
</p>
