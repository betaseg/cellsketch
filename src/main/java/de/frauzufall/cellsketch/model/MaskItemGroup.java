/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.frauzufall.cellsketch.model;

import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import de.frauzufall.cellsketch.BdvProject;
import org.scijava.app.StatusService;

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
		project.context().service(StatusService.class).showStatus("written config to " + getConfigPath());
	}

	protected String getConfigPath() {
		return maskItem.getConfigPath();
	}

}
