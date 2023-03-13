package de.frauzufall.cellsketch;

import de.frauzufall.cellsketch.analysis.FilamentsImporter;
import de.frauzufall.cellsketch.analysis.NMLReader;
import de.frauzufall.cellsketch.model.*;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.boundary.Boundary;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;
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
	private MaskFileItem cellBoundsItem;

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
				MaskItemGroup group = new MaskItemGroup(this, name, path);
				group.loadConfig();
				getItems().add(group);
				maskItems.add(group);
			}
		}
	}

	private void loadLabelMaps(N5Reader reader) throws IOException {
		Map<String, String> labelmaps = reader.getAttribute(File.separator, configKeyLabelMaps, HashMap.class);
		if(labelmaps != null) {
			for (Map.Entry<String, String> entry : labelmaps.entrySet()) {
				String name = entry.getKey();
				String path = entry.getValue();
				LabelMapItemGroup group = new LabelMapItemGroup(this, name, path);
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
				String path = entry.getValue();
				FilamentsItemGroup group = new FilamentsItemGroup(this, name, path + ".yml", path);
				group.loadConfig();
				getItems().add(group);
				filamentsItems.add(group);
			}
		}
	}

	private void loadCellBounds(N5Reader reader) throws IOException {
		String cellBounds = reader.getAttribute(File.separator, configKeyCellBounds, String.class);
		if(cellBounds != null) {
			this.cellBoundsItem = new MaskFileItem(this, getDefaultFileName(toFileName(cellBounds)), true);
			this.cellBoundsItem.loadConfig();
		}
	}

	@Override
	public void saveConfig() throws IOException {
		super.saveConfig();
		N5Writer writer = new N5FSWriter(getConfigPath());
		Map<String, String> masks = new HashMap<>();
		Map<String, String> labelmaps = new HashMap<>();
		Map<String, String> filaments = new HashMap<>();
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
		if(cellBoundsItem != null) writer.setAttribute(File.separator, configKeyCellBounds, cellBoundsItem.getName());
		writer.close();
	}

	@Override
	public void deleteFileItem(FileItem item) throws IOException {
		super.deleteFileItem(item);
		if(getLabelMapItems().contains(item)) getLabelMapItems().remove(item);
		if(getMaskItems().contains(item)) getMaskItems().remove(item);
		if(getFilamentsItems().contains(item)) getFilamentsItems().remove(item);
		if(cellBoundsItem == item) cellBoundsItem = null;
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

//	private void setupColors() {
////		microtubulesLabelMapItem.setDefaultColor(ARGBType.rgba(255, 200, 0, 255));
////		granulesLabelMapItem.setDefaultColor(ARGBType.rgba(100, 100, 100, 100));
//
//		sourceItem.setColor(ARGBType.rgba(130, 130, 130, 255));
//
//		granulesMembraneRelationItem.setColor(ARGBType.rgba(255, 191, 178, 255));
//		granulesMembraneRelationItem.setColorForMaxValues(false);
//		granulesMembraneRelationItem.setMinValue(0);
//		granulesMembraneRelationItem.setMaxValue(3.5);
//
//		connectedGranulesItem.setColor(ARGBType.rgba(166, 253, 167, 200));
//
//		microtubulesGranulesRelationItem.setColor(ARGBType.rgba(153, 138, 162, 200));
//		microtubulesGranulesRelationItem.setColorForMaxValues(false);
//		microtubulesGranulesRelationItem.setMinValue(0);
//		microtubulesGranulesRelationItem.setMaxValue(1);
//
//		membraneFullMaskItem.setColor(ARGBType.rgba(55, 55, 55, 155));
//
//		membraneDistanceTransformItem.setMaxValue(100);
//		microtubulesDistanceMapItem.setMaxValue(30);
//		nucleusDistanceTransformItem.setMaxValue(100);
//
//		golgiDistanceTransformItem.setColor(ARGBType.rgba(90, 220, 177, 150));
//
//		connectedMTCentriolesItem.setColor(ARGBType.rgba(255, 153, 0, 255));
//		connectedMTGolgiItem.setColor(ARGBType.rgba(255, 255, 0, 255));
//	}

	public MaskItemGroup addMaskItem(File input, String name, int color, Double connectedToFilamentsThresholdInUM, double scaleX, double scaleY, double scaleZ) throws IOException {
		MaskItemGroup group = new MaskItemGroup(this, name, getDefaultFileName(toFileName(name)));
		if (input != null && input.exists()) {
			addImageFile(input.toPath(), group.getMask().getDefaultFileName(), "int", scaleX, scaleY, scaleZ);
			group.getMask().setFile(new File(getProjectDir(), group.getMask().getDefaultFileName()));
			updateUI();
		}
		group.getMask().setColor(color);
		group.setConnectedToFilamentsThresholdInUM(connectedToFilamentsThresholdInUM);
		group.saveConfig();
		getItems().add(group);
		maskItems.add(group);
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
		this.cellBoundsItem = item;
		String borderName = name + " border";
		MaskItemGroup group = new MaskItemGroup(this, borderName, getDefaultFileName(toFileName(borderName)));
		calculateBoundary(item, group.getMask());
		group.setConnectedToFilamentsThresholdInUM(connectedToFilamentsThresholdInUM);
		group.saveConfig();
		maskItems.add(group);
		getItems().add(group);
	}

	public void addFilamentsFromKNOSSOS(File input, String name, int color, double scaleX, double scaleY, double scaleZ) throws IOException, NMLReader.NMLReaderIOException, DataConversionException {
		if(input != null && input.exists()) {
			String defaultFileName = getDefaultFileName(toFileName(name));
			FilamentsItemGroup item = new FilamentsItemGroup(this, name, defaultFileName + ".yml", defaultFileName);
			FilamentsImporter importer = new FilamentsImporter(this, item);
			importer.processKnossosFilaments(input, scaleX, scaleY, scaleZ);
			importer.render();
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
		return name.toLowerCase(Locale.ROOT).replace(" ", "_");
	}

	private void calculateBoundary(MaskFileItem input, MaskFileItem output) {
		output.setImage(boundary(input.getImage()));
		try {
			output.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <T extends RealType<T>> Img<ByteType> boundary(RandomAccessibleInterval<T> image) {
		RandomAccessibleInterval<BoolType> res = Converters.convert(image, (input, output) -> output.set(input.getRealFloat() > 0), new BoolType());
		return context().service(OpService.class).convert().int8(Converters.convert(Views.iterable(new Boundary<>(res)), (input, output) -> output.set((byte) (input.get()? 255 : 0)), new ByteType()));
	}
}
