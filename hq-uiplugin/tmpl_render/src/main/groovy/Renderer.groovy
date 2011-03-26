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

import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory


/**
 * This plugin provides basic .gsp template rendering support.  It is
 * primarily used through the RenditServer.renderTemplate() method
 */
class Renderer {
    // We cache the templates here in a static context, because each
    // creation of a template results in a new class -- and that's
    // bad if classloaders aren't being cleaned up correctly.
    private static final Map<String, Map> _templates = [:]
    private final Log _log = LogFactory.getLog(Renderer.class.name)

    private Template getTemplate(File gspFile) {
        if (!gspFile.isFile()) {
            throw new RuntimeException("File [${gspFile.absolutePath}] not readable or not found");
        }
        
        long mtime = gspFile.lastModified()
        synchronized(_templates) {
            Map tmplMap = _templates.get(gspFile)
            
            if (!tmplMap || tmplMap.mtime != mtime) {
                _log.info("Creating template for [${gspFile.absolutePath}]")
                gspFile.withReader("UTF-8") { reader ->
                    def eng = new SimpleTemplateEngine(Thread.currentThread().contextClassLoader)
                    Template t = eng.createTemplate(reader)
                    _templates[gspFile] = [mtime: mtime, template : t]
                    return t
                }
            } else {
                return tmplMap.template
            }
        }
    }

    def render(File gspFile, Map params, Writer output) {
        getTemplate(gspFile).make(params).writeTo(output)
        output.flush()
    }
}

