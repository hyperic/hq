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
package org.hyperic.hq.hqu.grails.plugins

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.plugins.codecs.*
import org.codehaus.groovy.grails.commons.*
import grails.util.Environment

/**
 * A plug-in that configures pluggable codecs 
 * 
 */
class HQUCodecsGrailsPlugin {
	
	def version = grails.util.GrailsUtil.getGrailsVersion()
	def dependsOn = [HQUCore:version]
	def watchedResources = "file:./grails-app/utils/**/*Codec.groovy"
	def providedArtefacts = [
                               HTMLCodec,
                               JavaScriptCodec,
                               URLCodec,
                               Base64Codec,
                               MD5Codec,
                               MD5BytesCodec,
                               HexCodec,
                               SHA1Codec,
                               SHA1BytesCodec,
                               SHA256Codec,
                               SHA256BytesCodec                               
                            ]

	def onChange = { event ->
		if(application.isArtefactOfType(CodecArtefactHandler.TYPE, event.source)) {
			application.addArtefact(CodecArtefactHandler.TYPE, event.source)
		}
	}

	def doWithDynamicMethods = { applicationContext ->
        for(GrailsCodecClass c in application.codecClasses) {
            def codecClass = c
            def codecName = codecClass.name
            def encodeMethodName = "encodeAs${codecName}"
            def decodeMethodName = "decode${codecName}"

            def encoder
            def decoder
            if (Environment.current == Environment.DEVELOPMENT) {
                // Resolve codecs in every call in case of a codec reload
                encoder = {->
                    def encodeMethod = codecClass.getEncodeMethod()
                    if(encodeMethod) {
                        return encodeMethod(delegate)
                    } else {
                        // note the call to delegate.getClass() instead of the more groovy delegate.class.
                        // this is because the delegate might be a Map, in which case delegate.class doesn't
                        // do what we want here...
                        throw new MissingMethodException(encodeMethodName, delegate.getClass(), [] as Object[])
                    }
                }
                decoder = {->
                    def decodeMethod = codecClass.getDecodeMethod()
                    if(decodeMethod) {
                        return decodeMethod(delegate)
                    } else {
                        // note the call to delegate.getClass() instead of the more groovy delegate.class.
                        // this is because the delegate might be a Map, in which case delegate.class doesn't
                        // do what we want here...
                        throw new MissingMethodException(decodeMethodName, delegate.getClass(), [] as Object[])
                    }
                }
            } else {
                // Resolve codec methods once only at startup
                def encodeMethod = codecClass.encodeMethod
                def decodeMethod = codecClass.decodeMethod
                if(encodeMethod) {
                    encoder = {-> encodeMethod(delegate) }
                } else {
                    // note the call to delegate.getClass() instead of the more groovy delegate.class.
                    // this is because the delegate might be a Map, in which case delegate.class doesn't
                    // do what we want here...
                    encoder = {-> throw new MissingMethodException(encodeMethodName, delegate.getClass(), [] as Object[]) }
                }
                if(decodeMethod) {
                    decoder = {-> decodeMethod(delegate) }
                } else {
                    // note the call to delegate.getClass() instead of the more groovy delegate.class.
                    // this is because the delegate might be a Map, in which case delegate.class doesn't
                    // do what we want here...
                    decoder = {-> throw new MissingMethodException(decodeMethodName, delegate.getClass(), [] as Object[]) }
                }
            }

            Object.metaClass."${encodeMethodName}" << encoder
            Object.metaClass."${decodeMethodName}" << decoder
        }
	}
}