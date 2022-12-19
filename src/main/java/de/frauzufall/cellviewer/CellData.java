package de.frauzufall.cellviewer;

import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import org.jetbrains.annotations.NotNull;
import sc.fiji.project.*;

import java.io.File;

public class CellData {

	private final BdvProject app;

	private ImageFileItem sourceItem;
	private ImageFileItem<FloatType> microtubulesDistanceMapItem;
	private LabelMapFileItem microtubulesLabelMapItem;
	private MaskFileItem spaceForGranulesItem;
	private ImageFileItem<FloatType> spaceForGranulesDistanceTransformItem;
	private LabelMapFileItem granulesLabelMapItem;
	private MaskFileItem membraneMaskItem;
	private ImageFileItem<FloatType> membraneDistanceTransformItem;
	private ImageFileItem<FloatType> nucleusDistanceTransformItem;
	private MaskFileItem membraneFullMaskItem;
	private MaskFileItem nucleusMaskItem;
	private MaskFileItem golgiMaskItem;
	private ImageFileItem<FloatType> centriolesDistanceTransformItem;
	private ImageFileItem<FloatType> golgiDistanceTransformItem;
	private MaskFileItem centriolesMaskItem;
	private MaskFileItem mitocondriaMaskItem;
	private FileItem microtubulesYamlItem;
	private FileItem microtubulesKnossosItem;
	private TableFileItem microtubulesStatsItem;
	private TableFileItem microtubulesIndividualStatsItem;
	private TableFileItem microtubulesMembraneLocationCell;
	private TableFileItem granulesStatsItem;
	private TableFileItem granulesIndividualStatsItem;
	private LabelTagItem microtubulesGranulesRelationItem;
	private LabelTagItem connectedGranulesItem;
	private LabelTagItem microtubulesLength;
	private LabelTagItem microtubulesTortuosity;
	private LabelTagItem granulesMembraneRelationItem;
	private LabelTagItem connectedMTCentriolesItem;
	private LabelTagItem connectedMTGolgiItem;
	private LabelTagItem notConnectedMTItem;

	public CellData(BdvProject app) {
		this.app = app;
		setupItems();
		setupColors();
	}


	private void setupColors() {
//		microtubulesLabelMapItem.setDefaultColor(ARGBType.rgba(255, 200, 0, 255));
//		granulesLabelMapItem.setDefaultColor(ARGBType.rgba(100, 100, 100, 100));

		sourceItem.setColor(ARGBType.rgba(130, 130, 130, 255));

		microtubulesLabelMapItem.setColor(ARGBType.rgba(255, 166, 214, 255));

		granulesLabelMapItem.setColor(ARGBType.rgba(255, 139, 115, 255));

		granulesMembraneRelationItem.setColor(ARGBType.rgba(255, 191, 178, 255));
		granulesMembraneRelationItem.setColorForMaxValues(false);
		granulesMembraneRelationItem.setMinValue(0);
		granulesMembraneRelationItem.setMaxValue(3.5);

		connectedGranulesItem.setColor(ARGBType.rgba(166, 253, 167, 200));

		microtubulesGranulesRelationItem.setColor(ARGBType.rgba(153, 138, 162, 200));
		microtubulesGranulesRelationItem.setColorForMaxValues(false);
		microtubulesGranulesRelationItem.setMinValue(0);
		microtubulesGranulesRelationItem.setMaxValue(1);

		microtubulesLength.setColorForMaxValues(false);
		microtubulesLength.setColor(ARGBType.rgba(200, 200, 200, 200));
		microtubulesLength.setMinValue(0);
		microtubulesLength.setMaxValue(10);

		microtubulesLength.setColorForMaxValues(false);
		microtubulesLength.setColor(ARGBType.rgba(200, 200, 200, 200));
		microtubulesTortuosity.setMinValue(1);
		microtubulesTortuosity.setMaxValue(10);

		membraneMaskItem.setColor(ARGBType.rgba(200, 200, 200, 155));
		membraneFullMaskItem.setColor(ARGBType.rgba(55, 55, 55, 155));

		mitocondriaMaskItem.setColor(ARGBType.rgba(73 ,145, 202, 75));

		nucleusMaskItem.setColor(ARGBType.rgba(255, 204, 104, 100));

		membraneDistanceTransformItem.setMaxValue(100);
		microtubulesDistanceMapItem.setMaxValue(30);
		nucleusDistanceTransformItem.setMaxValue(100);

		golgiMaskItem.setColor(ARGBType.rgba(50, 180, 137, 150));
		golgiDistanceTransformItem.setColor(ARGBType.rgba(90, 220, 177, 150));

		connectedMTCentriolesItem.setColor(ARGBType.rgba(255, 153, 0, 255));
		connectedMTGolgiItem.setColor(ARGBType.rgba(255, 255, 0, 255));
	}

	private void setupItems() {
		sourceItem = new ImageFileItem(app, "Source", getDefaultFileName("volumes", "raw"));
		app.setSourceItem(sourceItem);
		app.getItems().add(sourceItem);
		setupMembrane();
		setupNucleus();
		setupMitochondria();
		setupMicrotubules();
		setupCentrioles();
		setupGranules();
		setupGolgi();
	}

	private void setupMembrane() {
		membraneMaskItem = new MaskFileItem(app, "Membrane mask", getDefaultFileName("volumes", "membrane"));
		membraneDistanceTransformItem = new ImageFileItem<>(app, "Membrane distance map", getDefaultFileName("volumes", "membrane_distance_map"));
		membraneFullMaskItem = new MaskFileItem(app, "Membrane full mask", getDefaultFileName("volumes", "membrane_full"));

		DefaultItemGroup membraneGroup = new DefaultItemGroup("Membrane");
		membraneGroup.getItems().add(getMembraneMaskItem());
		membraneGroup.getItems().add(getMembraneDistanceMapItem());
		membraneGroup.getItems().add(getMembraneFullMaskItem());
		app.getItems().add(membraneGroup);
	}

	private String getDefaultFileName(String directory, String name) {
		return File.separator + directory + File.separator + this.app().getName() + "_" + name;
	}

	private void setupNucleus() {
		nucleusMaskItem = new MaskFileItem(app, "Nucleus mask", getDefaultFileName("volumes", "nucleus"));
		nucleusDistanceTransformItem = new ImageFileItem<>(app, "Nucleus distance map", getDefaultFileName("volumes", "nucleus_distance_map"));

		DefaultItemGroup nucleusGroup = new DefaultItemGroup("Nucleus");
		nucleusGroup.getItems().add(getNucleusMaskItem());
		nucleusGroup.getItems().add(getNucleusDistanceMapItem());
		app.getItems().add(nucleusGroup);
	}

	private void setupMitochondria() {
		mitocondriaMaskItem = new MaskFileItem(app, "Mitochondria mask", getDefaultFileName("volumes", "mitochondria"));

		DefaultItemGroup mitochondriaGroup = new DefaultItemGroup("Mitochondria");
		mitochondriaGroup.getItems().add(mitocondriaMaskItem);
		app.getItems().add(mitochondriaGroup);

	}

	private void setupMicrotubules() {
		microtubulesLabelMapItem = new LabelMapFileItem<>(app, "Microtubule labels", getDefaultFileName("volumes", "microtubules"));
		microtubulesDistanceMapItem = new ImageFileItem<>(app, "Microtubules distance map", getDefaultFileName("volumes", "microtubules_distance_map"));
		microtubulesKnossosItem = new FileItem(app, "Microtubules KNOSSOS file", ".xml", getDefaultFileName("misc", "microtubules.xml"));
		microtubulesYamlItem = new FileItem(app, "Microtubules yaml file", ".yml", getDefaultFileName("misc", "microtubules.yml"));
		microtubulesStatsItem = new TableFileItem(app, "Microtubules statistics", getDefaultFileName("misc", "microtubules.csv"));
		microtubulesIndividualStatsItem = new TableFileItem(app, "Microtubules individual statistics", getDefaultFileName("misc", "microtubules_individual.csv"), new MicrotubulesTable());
		microtubulesLength = microtubulesLabelMapItem.addLabel(
				"Length", microtubulesIndividualStatsItem,
				MicrotubulesTable.getLengthColumn(), Double.class);
		microtubulesTortuosity = microtubulesLabelMapItem.addLabel(
				"Tortuosity", microtubulesIndividualStatsItem,
				MicrotubulesTable.getTortuosityColumn(), Double.class);
		connectedMTCentriolesItem = microtubulesLabelMapItem.addLabel(
				"Connected to centrioles", microtubulesIndividualStatsItem,
				MicrotubulesTable.getConnectedToCentriolesColumn(), Boolean.class);
		connectedMTGolgiItem = microtubulesLabelMapItem.addLabel(
				"Connected to golgi", microtubulesIndividualStatsItem,
				MicrotubulesTable.getConnectedToGolgiColumn(), Boolean.class);

		DefaultItemGroup microtubulesGroup = new DefaultItemGroup("Microtubules");
		microtubulesGroup.getItems().add(microtubulesLabelMapItem);
		microtubulesGroup.getItems().add(microtubulesYamlItem);
		microtubulesGroup.getItems().add(microtubulesDistanceMapItem);
		microtubulesGroup.getItems().add(microtubulesStatsItem);
		microtubulesGroup.getItems().add(microtubulesIndividualStatsItem);
		microtubulesGroup.getItems().add(microtubulesLength);
		microtubulesGroup.getItems().add(microtubulesTortuosity);
		microtubulesGroup.getItems().add(connectedMTCentriolesItem);
		microtubulesGroup.getItems().add(connectedMTGolgiItem);
		app.getItems().add(microtubulesGroup);
	}

	private void setupCentrioles() {
		centriolesDistanceTransformItem = new ImageFileItem<>(app, "Centrioles distance map", getDefaultFileName("volumes", "centrioles_distance_map"));
		centriolesMaskItem = new MaskFileItem(app, "Centrioles mask", getDefaultFileName("volumes", "centrioles"));

		DefaultItemGroup centriolesGroup = new DefaultItemGroup("Centrioles");
		centriolesGroup.getItems().add(getCentriolesMaskItem());
		centriolesGroup.getItems().add(getCentriolesDistanceTransformItem());

		app.getItems().add(centriolesGroup);
	}

	private void setupGolgi() {
		golgiDistanceTransformItem = new ImageFileItem<>(app, "Golgi distance map", getDefaultFileName("volumes", "golgi_distance_map"));
		golgiMaskItem = new MaskFileItem(app, "Golgi mask", getDefaultFileName("volumes", "golgi"));

		DefaultItemGroup golgiGroup = new DefaultItemGroup("Golgi");
		golgiGroup.getItems().add(getGolgiMaskItem());
		golgiGroup.getItems().add(getGolgiDistanceTransformItem());

		app.getItems().add(golgiGroup);
	}

	private void setupGranules() {
//		granulesProbabilityMapItem = new AppImageFile(app, "Granula probability map");
		granulesLabelMapItem = new LabelMapFileItem(app, "Granule labels", getDefaultFileName("volumes", "granules"));
		spaceForGranulesDistanceTransformItem = new ImageFileItem<>(app, "Space for granules distance map", getDefaultFileName("volumes", "granules_space_for_distance_map"));
		spaceForGranulesItem = new MaskFileItem(app, "Space for granules mask", getDefaultFileName("volumes", "granules_space"));
		granulesIndividualStatsItem = new TableFileItem(app, "Granules individual statistics", getDefaultFileName("misc", "granules_individual.csv"), new GranulesTable());
		granulesStatsItem = new TableFileItem(app, "Granules statistics", getDefaultFileName("misc", "granules.csv"), new GranulesOverviewTable());
		microtubulesGranulesRelationItem = granulesLabelMapItem.addLabel(
				"Distance MT", granulesIndividualStatsItem,
				GranulesTable.getDistanceToMicrotubuleColumn(), Double.class);
		granulesMembraneRelationItem = granulesLabelMapItem.addLabel(
				"Distance membrane", granulesIndividualStatsItem,
				GranulesTable.getDistanceToMembraneColumn(), Double.class);
		connectedGranulesItem = granulesLabelMapItem.addLabel(
				"Connected to MT", granulesIndividualStatsItem,
				GranulesTable.getConnectedToMicrotubuleColumn(), Boolean.class);

//		granulesProbabilityMapItem = new AppImageFile(app, "Granula probability map");
		DefaultItemGroup granulesGroup = new DefaultItemGroup("Granules");
		granulesGroup.getItems().add(granulesLabelMapItem);
//		granulesGroup.getItems().add(granulesProbabilityMapItem);
		granulesGroup.getItems().add(granulesStatsItem);
		granulesGroup.getItems().add(granulesIndividualStatsItem);
		granulesGroup.getItems().add(microtubulesGranulesRelationItem);
		granulesGroup.getItems().add(granulesMembraneRelationItem);
		granulesGroup.getItems().add(connectedGranulesItem);
//		granulesGroup.getItems().add(getSpaceForGranulesItem());
//		granulesGroup.getItems().add(getSpaceForGranulesDistanceTransformItem());

		app.getItems().add(granulesGroup);
	}


	public ImageFileItem getSourceItem() {
		return sourceItem;
	}

	public ImageFileItem<FloatType> getMicrotubulesDistanceMapItem() {
		return microtubulesDistanceMapItem;
	}

	public LabelMapFileItem getMicrotubulesLabelMapItem() {
		return microtubulesLabelMapItem;
	}

	public MaskFileItem getSpaceForGranulesItem() {
		return spaceForGranulesItem;
	}

	public ImageFileItem<FloatType> getSpaceForGranulesDistanceTransformItem() {
		return spaceForGranulesDistanceTransformItem;
	}

	public LabelMapFileItem getGranulesLabelMapItem() {
		return granulesLabelMapItem;
	}

	public MaskFileItem getMembraneMaskItem() {
		return membraneMaskItem;
	}

	public ImageFileItem<FloatType> getMembraneDistanceMapItem() {
		return membraneDistanceTransformItem;
	}

	public ImageFileItem<FloatType> getNucleusDistanceMapItem() {
		return nucleusDistanceTransformItem;
	}

	public MaskFileItem getMembraneFullMaskItem() {
		return membraneFullMaskItem;
	}

	public MaskFileItem getNucleusMaskItem() {
		return nucleusMaskItem;
	}

	public ImageFileItem<FloatType> getCentriolesDistanceTransformItem() {
		return centriolesDistanceTransformItem;
	}

	public ImageFileItem<FloatType> getGolgiDistanceTransformItem() {
		return golgiDistanceTransformItem;
	}

	public MaskFileItem getCentriolesMaskItem() {
		return centriolesMaskItem;
	}

	public MaskFileItem getGolgiMaskItem() {
		return golgiMaskItem;
	}

	public TableFileItem getMicrotubulesStatsItem() {
		return microtubulesStatsItem;
	}

	public TableFileItem getMicrotubulesIndividualStatsItem() {
		return microtubulesIndividualStatsItem;
	}

	public TableFileItem getGranulesStatsItem() {
		return granulesStatsItem;
	}

	public TableFileItem getGranulesIndividualStatsItem() {
		return granulesIndividualStatsItem;
	}

	public BdvProject app() {
		return app;
	}

	public MaskFileItem getMitocondriaMaskItem() {
		return mitocondriaMaskItem;
	}

	public FileItem getMicrotubulesYamlFile() {
		return microtubulesYamlItem;
	}

	public FileItem getMicrotubulesKnossosItem() {
		return microtubulesKnossosItem;
	}

	public LabelTagItem getMicrotubulesConnectedToGolgiItem() {
		return connectedMTGolgiItem;
	}

	public LabelTagItem getMicrotubulesConnectedToCentriolesItem() {
		return connectedMTCentriolesItem;
	}

	public LabelTagItem getGranulesConnectedToMicrotubules() {
		return connectedGranulesItem;
	}
}
