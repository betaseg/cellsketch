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
package de.frauzufall.cellsketch;

import com.google.gson.JsonSyntaxException;
import de.frauzufall.cellsketch.analysis.FilamentsImporter;
import de.frauzufall.cellsketch.analysis.NMLReader;
import de.frauzufall.cellsketch.model.*;
import net.imglib2.type.numeric.ARGBType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.jdom2.DataConversionException;
import org.scijava.Context;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CellProject extends DefaultBdvProject {

	public final static String appName = "CellSketch";
	private final List<LabelMapItemGroup> labelMapItems = new ArrayList<>();
	private final List<MaskItemGroup> maskItems = new ArrayList<>();
	private final List<FilamentsItemGroup> filamentsItems = new ArrayList<>();
	private final String configKeyMasks = "masks";
	private final String configKeyLabelMaps = "labelmaps";
	private final String configKeyFilaments = "filaments";
	private final String configKeyCellBounds = "cellbounds";
	private MaskItemGroup cellBoundsItem;

	public CellProject(File projectDir, String name, Context context) {
		super(projectDir, name, context);
		initSourceItem();
	}

	private void initSourceItem() {
		setSourceItem(new ImageFileItem<>(this, getDefaultFileName("source"), false));
		getSourceItem().setName("source");
		getItems().add(getSourceItem());
	}

	public CellProject(File projectDir, Context context) {
		super(projectDir, context);
		initSourceItem();
	}

	@Override
	public boolean load() throws IOException {
		loadConfig();
		if(cellBoundsItem != null && cellBoundsItem.exists() && !getItems().contains(cellBoundsItem)) {
			getItems().add(cellBoundsItem);
		}
		getSourceItem().load();
		return true;
	}

	@Override
	public void loadConfig() throws IOException {
		super.loadConfig();
		N5Reader reader = new N5FSReader(getConfigPath());
		loadMasks(reader);
		loadLabelMaps(reader);
		loadFilaments(reader);
		loadCellBounds(reader);
		reader.close();
	}

	private void loadMasks(N5Reader reader) throws IOException {
		Map<String, String> masks = reader.getAttribute(File.separator, configKeyMasks, HashMap.class);
		if(masks != null) {
			for (Map.Entry<String, String> entry : masks.entrySet()) {
				String name = entry.getKey();
				String path = entry.getValue();
				MaskItemGroup group = new MaskItemGroup(this, name, fixSeparator(path));
				group.loadConfig();
				getItems().add(group);
				maskItems.add(group);
			}
		}
	}

	private String fixSeparator(String name) {
		return name.replace("/", File.separator).replace("\\", File.separator);
	}

	private void loadLabelMaps(N5Reader reader) throws IOException {
		Map<String, String> labelmaps = reader.getAttribute(File.separator, configKeyLabelMaps, HashMap.class);
		if(labelmaps != null) {
			for (Map.Entry<String, String> entry : labelmaps.entrySet()) {
				String name = entry.getKey();
				String path = entry.getValue();
				LabelMapItemGroup group = new LabelMapItemGroup(this, name, fixSeparator(path));
				group.loadConfig();
				getItems().add(group);
				labelMapItems.add(group);
			}
		}
	}

	private void loadFilaments(N5Reader reader) throws IOException {
		Map<String, String>  filaments = reader.getAttribute(File.separator, configKeyFilaments, HashMap.class);
		if(filaments != null) {
			for (Map.Entry<String, String> entry : filaments.entrySet()) {
				String name = entry.getKey();
				String path = fixSeparator(entry.getValue());
				FilamentsItemGroup group = new FilamentsItemGroup(this, name, path + ".yml", path);
				group.loadConfig();
				getItems().add(group);
				filamentsItems.add(group);
			}
		}
	}

	private void loadCellBounds(N5Reader reader) throws IOException {
		try {
			Map<String, String> cellBounds = reader.getAttribute(File.separator, configKeyCellBounds, HashMap.class);
			if(cellBounds != null) {
				for (Map.Entry<String, String> entry : cellBounds.entrySet()) {
					String name = entry.getKey();
					String path = fixSeparator(entry.getValue());
					MaskItemGroup group = new MaskItemGroup(this, name, path);
					group.loadConfig();
					getItems().add(group);
					this.cellBoundsItem = group;
				}
			}
		} catch(JsonSyntaxException e) {
			String cellBounds = reader.getAttribute(File.separator, configKeyCellBounds, String.class);
			if(cellBounds != null) {
				String name = cellBounds;
				String path = getName() + "_" + toFileName(name);
				MaskItemGroup group = new MaskItemGroup(this, name, path);
				group.loadConfig();
				getItems().add(group);
				this.cellBoundsItem = group;
			}
		}
	}

	@Override
	public void saveConfig() throws IOException {
		super.saveConfig();
		N5Writer writer = new N5FSWriter(getConfigPath());
		Map<String, String> masks = new HashMap<>();
		Map<String, String> labelmaps = new HashMap<>();
		Map<String, String> filaments = new HashMap<>();
		Map<String, String> cellBounds = new HashMap<>();
		for (MaskItemGroup maskItem : getMaskItems()) {
			masks.put(maskItem.getName(), maskItem.getMask().getDefaultFileName());
		}
		for (LabelMapItemGroup item : getLabelMapItems()) {
			labelmaps.put(item.getName(), item.getLabelMap().getDefaultFileName());
		}
		for (FilamentsItemGroup item : getFilamentsItems()) {
			filaments.put(item.getName(), item.getLabelMap().getDefaultFileName());
		}
		writer.setAttribute(File.separator, configKeyMasks, masks);
		writer.setAttribute(File.separator, configKeyLabelMaps, labelmaps);
		writer.setAttribute(File.separator, configKeyFilaments, filaments);
		if(cellBoundsItem != null) {
			cellBounds.put(cellBoundsItem.getName(), cellBoundsItem.getMask().getDefaultFileName());
			writer.setAttribute(File.separator, configKeyCellBounds, cellBounds);
		}
		writer.close();
	}

	@Override
	public void deleteFileItem(FileItem item) throws IOException {
		super.deleteFileItem(item);
		getLabelMapItems().remove(item);
		getMaskItems().remove(item);
		getFilamentsItems().remove(item);
		if(cellBoundsItem.equals(item)) cellBoundsItem = null;
		configChanged();
	}

	public void configChanged() throws IOException {
		saveConfig();
		populateModel();
		updateUI();
	}

	@Override
	public void deleteItemGroup(BdvItemGroup item) throws IOException {
		super.deleteItemGroup(item);
		if(getLabelMapItems().contains(item)) getLabelMapItems().remove(item);
		if(getMaskItems().contains(item)) getMaskItems().remove(item);
		if(getFilamentsItems().contains(item)) getFilamentsItems().remove(item);
		if(getItems().contains(item)) getItems().remove(item);
		configChanged();
	}

	public List<LabelMapItemGroup> getLabelMapItems() {
		return labelMapItems;
	}

	public List<FilamentsItemGroup> getFilamentsItems() {
		return filamentsItems;
	}

	public List<MaskItemGroup> getMaskItems() {
		return maskItems;
	}

	public MaskItemGroup addMaskItem(File input, String name, int color, Double connectedToFilamentsThresholdInUM, double scaleX, double scaleY, double scaleZ) throws IOException {
		MaskItemGroup group = new MaskItemGroup(this, name, getDefaultFileName(toFileName(name)));
		if (input != null && input.exists()) {
			addImageFile(input.toPath(), group.getMask().getDefaultFileName(), "int", scaleX, scaleY, scaleZ);
			group.getMask().setFile(new File(getProjectDir(), group.getMask().getDefaultFileName()));
			updateUI();
		}
		group.getMask().setColor(color);
		group.setConnectedToFilamentsThresholdInUM(connectedToFilamentsThresholdInUM);
		getItems().add(group);
		maskItems.add(group);
		configChanged();
		return group;
	}

	public void setBoundary(File input, String name, int color, Double connectedToFilamentsThresholdInUM, double scaleX, double scaleY, double scaleZ) throws IOException {
		if(input == null) return;
		MaskFileItem item = new MaskFileItem(this, getDefaultFileName(toFileName(name)), true);
		if (input != null && input.exists()) {
			addImageFile(input.toPath(), item.getDefaultFileName(), "int", scaleX, scaleY, scaleZ);
			item.setFile(new File(getProjectDir(), item.getDefaultFileName()));
			updateUI();
		}
		else return;
		item.setColor(color);
		item.setName(name);
		MaskItemGroup group = new MaskItemGroup(this, name, getDefaultFileName(toFileName(name)));
		group.setConnectedToFilamentsThresholdInUM(connectedToFilamentsThresholdInUM);
		group.saveConfig();
		this.cellBoundsItem = group;
		getItems().add(group);
	}

	public void addFilamentsFromKNOSSOS(File input, String name, int color, double scaleX, double scaleY, double scaleZ, double radius_in_um, boolean fixZOffset, boolean fixLineOrder) throws IOException, NMLReader.NMLReaderIOException, DataConversionException {
		if(input != null && input.exists()) {
			String defaultFileName = getDefaultFileName(toFileName(name));
			FilamentsItemGroup item = new FilamentsItemGroup(this, name, defaultFileName + ".yml", defaultFileName);
			FilamentsImporter importer = new FilamentsImporter(this, item);
			importer.processKnossosFilaments(input, scaleX, scaleY, scaleZ, fixZOffset, fixLineOrder);
			importer.render(radius_in_um * (1./this.getPixelToUM()));
			item.getLabelMap().setColor(color);

			item.getTagLength().setColorForMaxValues(false);
			item.getTagLength().setColor(ARGBType.rgba(200, 200, 200, 200));
			item.getTagLength().setMinValue(0);
			item.getTagLength().setMaxValue(10);

			item.getTagTortuosity().setColorForMaxValues(false);
			item.getTagTortuosity().setColor(ARGBType.rgba(200, 200, 200, 200));
			item.getTagTortuosity().setMinValue(1);
			item.getTagTortuosity().setMaxValue(10);

			getFilamentsItems().add(item);
			getItems().add(item);
		} else {
			context().service(StatusService.class).showStatus("Not rendering filaments, KNOSSOS file missing.");
		}
	}

	public LabelMapItemGroup addLabelMapItem(File input, String name, int color, Double connectedToFilamentsEndThresholdInUM, double scaleX, double scaleY, double scaleZ) throws IOException {
		LabelMapItemGroup group = new LabelMapItemGroup(this, name, getDefaultFileName(toFileName(name)));
		if (input != null && input.exists()) {
			addImageFile(input.toPath(), group.getLabelMap().getDefaultFileName(), "int", scaleX, scaleY, scaleZ);
			group.getLabelMap().setFile(new File(getProjectDir(), group.getLabelMap().getDefaultFileName()));
			updateUI();
		} else return null;
		group.getLabelMap().setColor(color);
		group.setConnectedToFilamentsEndThresholdInUM(connectedToFilamentsEndThresholdInUM);
		getItems().add(group);
		labelMapItems.add(group);
		return group;
	}

	private String toFileName(String name) {
		return fixSeparator(name).toLowerCase(Locale.ROOT).replace(" ", "_");
	}

	public MaskItemGroup getBoundary() {
		return cellBoundsItem;
	}
}
