/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.html.HtmlUtil

import org.hyperic.util.config.ConfigResponse

import java.util.regex.PatternSyntaxException

import org.json.JSONObject
import org.json.JSONArray

class JmxController extends BaseController
{
    private static final DEFAULT_PATTERN = "java.lang:type=MemoryPool,*"
    private static final DEFAULT_ATTRIBUTE_FILTER = ".*"
    private static final DEFAULT_OPERATION_FILTER = ".*"
            
    protected void init() {
        onlyAllowSuperUsers()
        setJSONMethods(['query',  'invoke'])
    }

      def queryMBeans(params) {
        def eid = viewedResource.entityId
        def pattern = params.getOne('pattern')
        def attributeFilter = params.getOne('attributeFilter')
        def operationFilter = params.getOne('operationFilter')
        def presetFilter = params.getOne('presetFilter')
        def refreshInterval = params.getOne('refreshInterval')
        def message, data = []
        
        if (pattern) {
            def resources = viewedMembers

            def matches = 0

            for (resource in resources) {
                def result = resourceQuery(resource, pattern, attributeFilter, operationFilter)
                if (result['message']) {
                    message = result['message']
                }
                if (resources.size() > 1) {
                    result['resource'] = resource
                }
                data << result
                if (result['beans']) {
                    matches += result['beans'].size()
                }
            }

            if (!message) message = matches + " matches"
        }
        else {
            message = ''
            pattern = DEFAULT_PATTERN
            attributeFilter = DEFAULT_ATTRIBUTE_FILTER
            operationFilter = DEFAULT_OPERATION_FILTER
        }

        render(locals:[
            eid:eid,
            data:data,
            pattern:pattern,
            attributeFilter:attributeFilter,
            operationFilter:operationFilter,
            message:message,
            filters:serverFilters,
            presetFilter:presetFilter,
            refreshInterval:refreshInterval
        ])
    }
    
    def listMBeans(params) {
        def eid = viewedResource.entityId
        def pattern = params.getOne('pattern')
        def attributeFilter = params.getOne('attributeFilter')
        def operationFilter = params.getOne('operationFilter')
        def presetFilter = params.getOne('presetFilter')
        
        JSONArray res = new JSONArray()
        
        if (pattern) {
            def resources = viewedMembers
            for (resource in resources) {
                def result = resourceQuery(resource, pattern, attributeFilter, operationFilter)
                for (bean in result['beans']) {
                    for (attrName in bean.attrNames) { 
                        def val = [id: "${bean.dojoId}.${attrName}.value", value: bean.attrs.get(attrName)['Value'], attribute:attrName] as JSONObject
                        res.put(val)
                    }
                }
            }
        }

        JSONObject jsres = new JSONObject()
        jsres.put('results', res)
        render(inline:"/* ${jsres} */",
               contentType:'text/json-comment-filtered')
               
    }
    
    private getServerFilters() {
        def filters = []
        File dir = Bootstrap.getResource("hqu/jmx/conf").getFile();
        XmlParser parser = new XmlParser()
        def children = dir.listFiles();
        for (child in children) {
            // Get filename of file or directory
            if (child.name.endsWith("-filter.xml"))
                filters.addAll(parser.parse(child).children())
        }
        filters
    }
    
    private filterAttributes(attrs, filter) {
        def filteredAttrs = [:]
        for (attr in attrs.keySet()) {
            if (attr.matches(filter)) {
                filteredAttrs.put(attr,attrs[attr])
            }
        }
        filteredAttrs
    }
    
    private filterOperations(ops, filter) {
        def filteredOps = []
        for (op in ops) {
            if (op.name.matches(filter)) {
                filteredOps << op
            }
        }
        filteredOps
    }
    
    private getViewedMembers() {
        def r = viewedResource
        def members

        if (r == null) {
            //masthead
            members = getResourceHelper().find(byPrototype: 'Sun JVM 1.5')
        }
        else if (r.isGroup()) {
            members = r.getGroupMembers(user).findAll {it.entityId.isServer()}
        } else {
            members = [r]
        }
        members
    }

    def arrayToString(obj) {
        def buff = new StringBuffer("[")
        obj.each()  { (it.toString()) ? buff.append(it).append(", ") : 0 };
        (buff.length() > 2) ? buff.substring(0, buff.length()-2).concat("]") : "[]"
    }
    
    def valueToString(obj) {
        if (obj == null) {
            return null
        }
        if (obj.getClass().isArray()) {
            return arrayToString(obj)
        }
        if (obj instanceof javax.management.openmbean.CompositeData)
        {
            javax.management.openmbean.CompositeData comp = 
              (javax.management.openmbean.CompositeData) obj;
            def keyVals = [:]
            comp.getCompositeType().keySet().each() { keyVals.put(it, valueToString(comp.get(it))) }
            return keyVals.toString()
        }
        else if (obj instanceof javax.management.openmbean.TabularData) {
            javax.management.openmbean.TabularData tab = 
              (javax.management.openmbean.TabularData) obj;            
            def keyComp = []
            for (key in tab.keySet()) {
              javax.management.openmbean.CompositeData comp = obj.get((Object[])key)
              def keyVals = [:]
              comp.getCompositeType().keySet().each() { keyVals.put(it, valueToString(comp.get(it))) }
              keyComp << keyVals
            }
            return keyComp.toString()
        }
        else {
            return obj.toString()
        }
    }    
    
    private def resourceQuery(resource, pattern, attributeFilter, operationFilter) {
        def cfg = [ObjectName : pattern] as ConfigResponse
        def res = resource.getLiveData(user, "query", cfg)
        if (res.errorMessage) {
            return [ message : "<b>Error: </b>" + res.errorMessage ]
        }
        def ores = res.objectResult
        if (ores.size() == 0) {
            return [ message : 'No matches' ]
        }

        def beans = []
        def data = [beans : beans]

        for (bean in ores) {
            def attrs
            try {
                attrs = filterAttributes(bean.value.get("Attributes"), attributeFilter)
            }
            catch (PatternSyntaxException  e) {
                return [ message : "<b>Error: </b> Attribute filter must be a valid regular expression" ]
            }
            def attrNames = attrs.keySet().toArray(new String[0])
            Arrays.sort(attrNames)

            def ops
            try {
                ops = filterOperations(bean.value.get("Methods"), operationFilter)
            }
            catch (PatternSyntaxException  e) {
                return [ message : "<b>Error: </b> Operation filter must be a valid regular expression" ]
            }
            
            def entry = [
                name : bean.key,
                dojoId: bean.key.hashCode(),
                resId : resource.id,
                resName : resource.name,
                attrNames : attrNames,
                attrs : prettify(attrNames, attrs),
                ops : ops.sort { a,b -> a.signature.length <=> b.signature.length },
            ]
            beans << entry
        }

        return data
    }

    def prettify(attrNames, attrs) {
        for (attr in attrNames) {
            attrs.get(attr)['Value']= HtmlUtil.escapeHtml(valueToString(attrs.get(attr)['Value']))
        }
        attrs
    }
    
    def invoke(params) {
        def id = params.getOne('id').toInteger()
        def name = params.getOne('name')
        def op = params.getOne('op')
        def args = params.getOne('args')

        def resource = getResourceHelper().findResource(id)

        def cfg = [ObjectName: name, Method: op] as ConfigResponse
        if (args) {
            cfg.setValue("Arguments", args)
        }
        def res = resource.getLiveData(user, "invoke", cfg)

        def msg, val
        if (res.errorMessage) {
            msg = "Error invoking operation on ${resource.name}: ${res.errorMessage}"
        } else {
            if (!args) {
                args = ""
            }
            msg = "Invoked ${op}(${args}) on ${resource.name} successfully."
            def obj = res.objectResult
            if (obj) {
                val = HtmlUtil.escapeHtml(obj)
            }
            else {
                val = "No value returned."
            }
        }

        def html = """
        <h3>Operation Status:</h3>
        <p>${msg}</p>
        Result: ${val}
        """

        [html: html]
    }
}
