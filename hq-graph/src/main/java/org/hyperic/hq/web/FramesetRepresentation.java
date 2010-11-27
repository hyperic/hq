package org.hyperic.hq.web;

import java.util.ArrayList;
import java.util.List;

public class FramesetRepresentation {
	private String name;
	private Boolean repeatable;
	private List<InputRepresentation> inputs = new ArrayList<InputRepresentation>();
	
	public FramesetRepresentation(String name, Boolean repeatable) {
		this.name = name;
		this.repeatable = repeatable;
	}
	
	public String getName() {
		return name;
	}

	public Boolean getRepeatable() {
		return repeatable;
	}

	public List<InputRepresentation> getInputs() {
		return inputs;
	}

	public void addInput(String type, Boolean required, String name, String defaultValue) {
		inputs.add(new InputRepresentation(type, required, name, defaultValue));
	}
}