package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

public class FormRepresentation {
	private String method;
	private String uri;
	private List<InputRepresentation> inputs = new ArrayList<InputRepresentation>();
	private List<FramesetRepresentation> framesets = new ArrayList<FramesetRepresentation>();
	
	public FormRepresentation(String method, String uri) {
		this.method = method;
		this.uri = uri;
	}
	
	public String getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public List<InputRepresentation> getInputs() {
		return inputs;
	}

	public List<FramesetRepresentation> getFramesets() {
		return framesets;
	}

	public void addInput(String type, Boolean required, String name, String defaultValue) {
		inputs.add(new InputRepresentation(type, required, name, defaultValue));
	}

	public FramesetRepresentation addFrameset(String name) {
		return addFrameset(name, Boolean.FALSE);
	}
	
	public FramesetRepresentation addFrameset(String name, Boolean repeatable) {
		FramesetRepresentation result = new FramesetRepresentation(name, repeatable);
		
		framesets.add(result);
		
		return result;
	}
}
