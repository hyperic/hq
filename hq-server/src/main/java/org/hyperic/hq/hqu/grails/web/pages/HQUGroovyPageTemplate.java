package org.hyperic.hq.hqu.grails.web.pages;

import java.util.Map;


import groovy.lang.Writable;
import groovy.text.Template;

public class HQUGroovyPageTemplate implements Template {
    private HQUGroovyPageMetaInfo metaInfo;

    public HQUGroovyPageTemplate(HQUGroovyPageMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public Writable make() {
        return new HQUGroovyPageWritable(metaInfo);
    }

    public Writable make(Map binding) {
    	HQUGroovyPageWritable gptw = new HQUGroovyPageWritable(metaInfo);
        gptw.setBinding(binding);
        return gptw;
    }

	public HQUGroovyPageMetaInfo getMetaInfo() {
		return metaInfo;
	}
}
