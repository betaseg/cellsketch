package de.frauzufall.cellsketch.model;

import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import de.frauzufall.cellsketch.BdvProject;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LabelMapItemGroup<T extends IntegerType<T>> extends BdvItemGroup implements HasDistanceMap {
	private final ImageFileItem<FloatType> distanceMapItem;
	private final LabelMapFileItem<T> labelMapItem;
	private final TableFileItem statsItem;
	private final TableFileItem individualStatsItem;
	private Double connectedToFilamentsEndThresholdInUM = null;

	public LabelMapItemGroup(BdvProject project, String name, String path) {
		super(project, name);
		this.labelMapItem = new LabelMapFileItem(project, path, false);
		this.labelMapItem.setName(name + " labels");
		this.distanceMapItem = new ImageFileItem<>(project, File.separator + "analysis" + labelMapItem.getDefaultFileName() + "_distance_map", true);
		this.distanceMapItem.setName(name + " distance map");
		this.statsItem = new TableFileItem(project, File.separator + "analysis" + labelMapItem.getDefaultFileName() + ".csv", true);
		this.statsItem.setName(labelMapItem.getName() + " statistics");
		this.individualStatsItem = new TableFileItem(project, File.separator + "analysis" + labelMapItem.getDefaultFileName() + "_individual.csv", true);
		this.individualStatsItem.setName(labelMapItem.getName() + " individual statistics");
		this.getItems().add(labelMapItem);
		this.getItems().add(distanceMapItem);
		this.getItems().add(statsItem);
		this.getItems().add(individualStatsItem);
	}

	@Override
	public void loadConfig() throws IOException {
		super.loadConfig();
		N5Reader reader = new N5FSReader(getConfigPath());
		if(reader.exists(File.separator)) {
			Map<String, Map> tags = reader.getAttribute(File.separator, "tags", Map.class);
			if(tags != null) {
				for (Map.Entry<String, Map> entry : tags.entrySet()) {
					Map<String, String> tagAttributes = entry.getValue();
					LabelTagItem tag = addLabelIfNotExists(tagAttributes.get("column"), asClass(tagAttributes.get("class")), true);
					if(tagAttributes.get("min") != null) tag.setMinValue(Double.valueOf(tagAttributes.get("min")));
					if(tagAttributes.get("max") != null) tag.setMaxValue(Double.valueOf(tagAttributes.get("max")));
					if(tagAttributes.get("color") != null) tag.setColor(Integer.valueOf(tagAttributes.get("color")));
					if(tagAttributes.get("colorMax") != null) tag.setColorForMaxValues(Boolean.valueOf(tagAttributes.get("colorMax")));
				}
			}
		}
		reader.close();
	}

	@Override
	public void saveConfig() throws IOException {
		super.saveConfig();
		N5Writer writer = new N5FSWriter(getConfigPath());
		Map<String, Map<String, String>> tags = new HashMap<>();
		for(LabelTagItem tag: labelMapItem.getTagItems()) {
			Map<String, String> tagProperties = new HashMap<>();
			tagProperties.put("color", String.valueOf(tag.getColor()));
			tagProperties.put("class", tag.getTagClass().getName());
			tagProperties.put("min", String.valueOf(tag.getMinValue()));
			tagProperties.put("max", String.valueOf(tag.getMaxValue()));
			tagProperties.put("column", tag.getReferenceColumnName());
			tagProperties.put("table", tag.getReferenceTable().getDefaultFileName());
			tagProperties.put("colorMax", String.valueOf(tag.getColorMax()));
			tags.put(tag.getName(), tagProperties);
		}
		writer.setAttribute(File.separator, "tags", tags);
		writer.close();
		project.context().service(StatusService.class).showStatus("written config to " + getConfigPath());
	}

	private Class asClass(Object className) {
		if(className.equals(Double.class.getName())) return Double.class;
		if(className.equals(Float.class.getName())) return Float.class;
		if(className.equals(Integer.class.getName())) return Integer.class;
		if(className.equals(Boolean.class.getName())) return Boolean.class;
		if(className.equals(String.class.getName())) return String.class;
		return Object.class;
	}

	protected String getConfigPath() {
		return labelMapItem.getConfigPath();
	}

	public LabelMapFileItem<T> getLabelMap() {
		return labelMapItem;
	}

	public TableFileItem getOverallStats() {
		return statsItem;
	}

	public TableFileItem getIndividualStats() {
		return individualStatsItem;
	}

	@Override
	public ImageFileItem<FloatType> getDistanceMap() {
		return distanceMapItem;
	}

	@Override
	public ImageFileItem distanceMapSource() {
		return this.labelMapItem;
	}

	@Override
	public Double getConnectedToFilamentsEndThresholdInUM() {
		return connectedToFilamentsEndThresholdInUM;
	}

	public void setConnectedToFilamentsEndThresholdInUM(Double connectedToFilamentsEndThresholdInUM) {
		this.connectedToFilamentsEndThresholdInUM = connectedToFilamentsEndThresholdInUM;
	}

	public LabelTagItem addLabelIfNotExists(String sizeColumnName, Class className, boolean colorMax) {
		LabelTagItem tagDistance = getLabelMap().addLabel(
				sizeColumnName, getIndividualStats(),
				sizeColumnName, className);
		tagDistance.setColorForMaxValues(colorMax);
		if(getItems().contains(tagDistance)) {
			tagDistance.setTagClass(className);
			return tagDistance;
		}
		getItems().add(tagDistance);
		return tagDistance;

	}
}
