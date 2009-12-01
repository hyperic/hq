package org.hyperic.hq.ui;

/**
 * Dashboard descriptor
 * @author rpack
 *
 */
public class Dashboard {

	private Integer _id;
	private String _description;
	private String _name;

	public Dashboard() {}

	public Dashboard(String name) {
		_name = name;
		_id = new Integer(-1);
		_description = "";
	}

	public Integer getId() {
		return _id;
	}

	public void setId(Integer id) {
		this._id = id;
	}

	public String getDescription() {
		return _description;
	}

	public void setDescription(String description) {
		this._description = description;
	}

	public String getName() {
		return _name;
	}

	public void set_name(String name) {
		this._name = name;
	}
}
