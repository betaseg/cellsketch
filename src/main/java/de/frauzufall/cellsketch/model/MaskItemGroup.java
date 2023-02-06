package de.frauzufall.cellsketch.model;

import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import de.frauzufall.cellsketch.BdvProject;

import java.io.File;
import java.io.IOException;

public class MaskItemGroup extends BdvItemGroup implements HasDistanceMap {
	private MaskFileItem maskItem;
	private ImageFileItem<FloatType> distanceMapItem;
	private Double connectedToFilamentsThresholdInUM = null;

	public MaskItemGroup(BdvProject project, String name, String path) {
		super(project, name);
		this.maskItem = new MaskFileItem(project, path, false);
		this.distanceMapItem = new ImageFileItem<>(project, File.separator + "analysis" + maskItem.getDefaultFileName() + "_distance_map", true);
		this.maskItem.setName(name + " mask");
		this.distanceMapItem.setName(name + " distance map");
		this.getItems().add(maskItem);
		this.getItems().add(distanceMapItem);
	}

	public MaskFileItem getMask() {
		return maskItem;
	}

	@Override
	public ImageFileItem<FloatType> getDistanceMap() {
		return distanceMapItem;
	}

	@Override
	public ImageFileItem distanceMapSource() {
		return maskItem;
	}

	@Override
	public Double getConnectedToFilamentsEndThresholdInUM() {
		return connectedToFilamentsThresholdInUM;
	}

	public void setConnectedToFilamentsThresholdInUM(Double connectedToFilamentsThresholdInUM) {
		this.connectedToFilamentsThresholdInUM = connectedToFilamentsThresholdInUM;
	}


	@Override
	public void loadConfig() throws IOException {
		super.loadConfig();
		N5Reader reader = new N5FSReader(getConfigPath());
		if(reader.exists(File.separator)) {
			Double connectedThreshold = reader.getAttribute(File.separator, "connectedToFilamentsThresholdInUM", Double.class);
			if(connectedThreshold != null) {
				connectedToFilamentsThresholdInUM = connectedThreshold;
			}
		}
		reader.close();
	}

	@Override
	public void saveConfig() throws IOException {
		super.saveConfig();
		N5Writer writer = new N5FSWriter(getConfigPath());
		writer.setAttribute(File.separator, "connectedToFilamentsThresholdInUM", connectedToFilamentsThresholdInUM);
		writer.close();
		System.out.println("written config to " + getConfigPath());
	}

	protected String getConfigPath() {
		return maskItem.getConfigPath();
	}

}
