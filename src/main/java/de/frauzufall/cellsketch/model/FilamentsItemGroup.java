package de.frauzufall.cellsketch.model;

import de.frauzufall.cellsketch.BdvProject;

public class FilamentsItemGroup extends LabelMapItemGroup {
	private final FileItem yamlItem;
	private final LabelTagItem tagLength;
	private final LabelTagItem tagTortuosity;

	public FilamentsItemGroup(BdvProject project, String name, String yamlPath, String path) {
		super(project, name, path);
		this.yamlItem = new FileItem(project, yamlPath, true);
		this.tagLength = addLabelIfNotExists(FilamentsTable.getLengthColumnName(), Double.class, true);
		this.tagTortuosity = addLabelIfNotExists(FilamentsTable.getTortuosityColumnName(), Double.class, true);
	}

	public FileItem getFilamentsYamlItem() {
		return yamlItem;
	}

	public LabelTagItem getTagLength() {
		return tagLength;
	}

	public LabelTagItem getTagTortuosity() {
		return tagTortuosity;
	}
}
