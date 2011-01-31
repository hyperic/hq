package org.hyperic.hq.pdk.domain;

public class MetricInfo {
	private String templateName;
	private String metricName;
	private String alias;
	private String units;
	private String category;
	private boolean indicator = false;
	private boolean defaultOn = false;
	
	public MetricInfo() {}
	
	public MetricInfo(String templateName, String metricName, String alias, String units, String category, boolean indicator, boolean defaultOn) {
		this.templateName = templateName + alias;
		this.metricName = metricName;
		this.alias = (alias == null || alias.isEmpty()) ? metricName.replaceAll(" ", "").toLowerCase() : alias;
		this.units = units;
		this.category = category;
		this.indicator = indicator;
		this.defaultOn = defaultOn;
	}
	
	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getUnits() {
		return units;
	}
	
	public void setUnits(String units) {
		this.units = units;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}
	
	public boolean isIndicator() {
		return indicator;
	}
	
	public void setIndicator(boolean indicator) {
		this.indicator = indicator;
	}
	
	public boolean isDefaultOn() {
		return defaultOn;
	}
	
	public void setDefaultOn(boolean defaultOn) {
		this.defaultOn = defaultOn;
	}
}

