package de.frauzufall.cellsketch.model;

import de.frauzufall.cellsketch.BdvProject;

import java.io.IOException;

public class BdvItemGroup extends DefaultItemGroup {
	protected final BdvProject project;

	public BdvItemGroup(BdvProject project, String name) {
		super(name, true);
		this.project = project;
	}

	@Override
	public void delete() throws IOException {
		super.delete();
		project.deleteItemGroup(this);
	}
}
