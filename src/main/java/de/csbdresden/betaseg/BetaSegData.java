package de.csbdresden.betaseg;

import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.project.BdvProject;
import sc.fiji.project.DefaultItemGroup;
import sc.fiji.project.ImageFileItem;
import sc.fiji.project.LabelMapFileItem;
import sc.fiji.project.LabelTagItem;
import sc.fiji.project.MaskFileItem;
import sc.fiji.project.TableFileItem;

public class BetaSegData {

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
	private MaskFileItem ciliumMaskItem;
	private ImageFileItem<FloatType> centriolesDistanceTransformItem;
	private ImageFileItem<FloatType> golgiDistanceTransformItem;
	private MaskFileItem centriolesMaskItem;
	private MaskFileItem mitocondriaMaskItem;
	private TableFileItem microtubulesStatsItem;
	private TableFileItem microtubulesIndividualStatsItem;
	private TableFileItem microtubulesMembraneLocationCell;
	private TableFileItem granulesStatsItem;
	private TableFileItem granulesIndividualStatsItem;
	private LabelTagItem microtubulesGranulesRelationItem;
	private LabelTagItem connectedGranulesItem;
	private LabelTagItem microtubulesLength;
	private LabelTagItem granulesMembraneRelationItem;
	private LabelTagItem connectedMTCentriolesItem;
	private LabelTagItem connectedMTGolgiItem;
	private LabelTagItem notConnectedMTItem;


	//	@Parameter(label = "Pixel (Knossos) to μm factor")
	private double pixelToMicroMetersKnossos = 0.004;

	//	@Parameter(label = "Pixel (TIFF) to μm factor")
	private double pixelToMicroMetersTiff = pixelToMicroMetersKnossos*4;

	public BetaSegData(BdvProject app) {
		this.app = app;
		setupItems();
	}

	private void setupItems() {
		sourceItem = new ImageFileItem(app, "Source");
		app.setSourceItem(sourceItem);
		app.getItems().add(sourceItem);
		setupMembrane();
		setupNucleus();
		setupMitochondria();
		setupMicrotubules();
		setupCilium();
		setupCentrioles();
		setupGranules();
		setupGolgi();
	}

	private void setupMembrane() {
		membraneMaskItem = new MaskFileItem(app, "Membrane mask");
		membraneDistanceTransformItem = new ImageFileItem<>(app, "Membrane distance map");
		membraneFullMaskItem = new MaskFileItem(app, "Membrane full mask");

		DefaultItemGroup membraneGroup = new DefaultItemGroup("Membrane");
		membraneGroup.getItems().add(getMembraneMaskItem());
		membraneGroup.getItems().add(getMembraneDistanceMapItem());
		membraneGroup.getItems().add(getMembraneFullMaskItem());
		app.getItems().add(membraneGroup);
	}

	private void setupNucleus() {
		nucleusMaskItem = new MaskFileItem(app, "Nucleus mask");
		nucleusDistanceTransformItem = new ImageFileItem<>(app, "Nucleus distance map");

		DefaultItemGroup nucleusGroup = new DefaultItemGroup("Nucleus");
		nucleusGroup.getItems().add(getNucleusMaskItem());
		nucleusGroup.getItems().add(getNucleusDistanceMapItem());
		app.getItems().add(nucleusGroup);
	}

	private void setupMitochondria() {
		mitocondriaMaskItem = new MaskFileItem(app, "Mitochondria mask");

		DefaultItemGroup mitochondriaGroup = new DefaultItemGroup("Mitochondria");
		mitochondriaGroup.getItems().add(getMitocondriaMaskItem());
		app.getItems().add(mitochondriaGroup);

	}

	private void setupMicrotubules() {
		microtubulesLabelMapItem = new LabelMapFileItem(app, "Microtubule labels");
		microtubulesDistanceMapItem = new ImageFileItem<>(app, "Microtubules distance map");
		microtubulesStatsItem = new TableFileItem(app, "Microtubules statistics");
		microtubulesIndividualStatsItem = new TableFileItem(app, "Microtubules individual statistics", new MicrotubulesTable());
		microtubulesLength = microtubulesLabelMapItem.addLabel(
				"Length", microtubulesIndividualStatsItem,
				MicrotubulesTable.getLengthColumn(), Double.class);
		connectedMTCentriolesItem = microtubulesLabelMapItem.addLabel(
				"Connected to centrioles", microtubulesIndividualStatsItem,
				MicrotubulesTable.getConnectedToCentriolesColumn(), Boolean.class);
		connectedMTGolgiItem = microtubulesLabelMapItem.addLabel(
				"Connected to golgi", microtubulesIndividualStatsItem,
				MicrotubulesTable.getConnectedToGolgiColumn(), Boolean.class);

		DefaultItemGroup microtubulesGroup = new DefaultItemGroup("Microtubules");
		microtubulesGroup.getItems().add(getMicrotubulesLabelMapItem());
		microtubulesGroup.getItems().add(getMicrotubulesDistanceMapItem());
		microtubulesGroup.getItems().add(getMicrotubulesStatsItem());
		microtubulesGroup.getItems().add(getMicrotubulesIndividualStatsItem());
		microtubulesGroup.getItems().add(getMicrotubulesLength());
		microtubulesGroup.getItems().add(connectedMTCentriolesItem);
		microtubulesGroup.getItems().add(connectedMTGolgiItem);
		app.getItems().add(microtubulesGroup);
	}

	private void setupCilium() {
		ciliumMaskItem = new MaskFileItem(app, "Cilium mask");

		DefaultItemGroup ciliumGroup = new DefaultItemGroup("Cilium");
		ciliumGroup.getItems().add(getCiliumMaskItem());
		app.getItems().add(ciliumGroup);
	}

	private void setupCentrioles() {
		centriolesDistanceTransformItem = new ImageFileItem<>(app, "Centrioles distance map");
		centriolesMaskItem = new MaskFileItem(app, "Centrioles mask");

		DefaultItemGroup centriolesGroup = new DefaultItemGroup("Centrioles");
		centriolesGroup.getItems().add(getCentriolesMaskItem());
		centriolesGroup.getItems().add(getCentriolesDistanceTransformItem());

		app.getItems().add(centriolesGroup);
	}

	private void setupGolgi() {
		golgiDistanceTransformItem = new ImageFileItem<>(app, "Golgi distance map");
		golgiMaskItem = new MaskFileItem(app, "Golgi mask");

		DefaultItemGroup golgiGroup = new DefaultItemGroup("Golgi");
		golgiGroup.getItems().add(getGolgiMaskItem());
		golgiGroup.getItems().add(getGolgiDistanceTransformItem());

		app.getItems().add(golgiGroup);
	}

	private void setupGranules() {
//		granulesProbabilityMapItem = new AppImageFile(app, "Granula probability map");
		granulesLabelMapItem = new LabelMapFileItem(app, "Granule labels");
		spaceForGranulesDistanceTransformItem = new ImageFileItem<>(app, "Space for granules distance map");
		spaceForGranulesItem = new MaskFileItem(app, "Space for granules mask");
		granulesIndividualStatsItem = new TableFileItem(app, "Granules individual statistics", new GranulesTable());
		granulesStatsItem = new TableFileItem(app, "Granules statistics", new GranulesOverviewTable());
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
		granulesGroup.getItems().add(getGranulesLabelMapItem());
//		granulesGroup.getItems().add(granulesProbabilityMapItem);
		granulesGroup.getItems().add(getGranulesStatsItem());
		granulesGroup.getItems().add(getGranulesIndividualStatsItem());
		granulesGroup.getItems().add(getMicrotubulesGranulesRelationItem());
		granulesGroup.getItems().add(getGranulesMembraneRelationItem());
		granulesGroup.getItems().add(getConnectedGranulesItem());
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

	public MaskFileItem getCiliumMaskItem() {
		return ciliumMaskItem;
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

	public MaskFileItem getMitocondriaMaskItem() {
		return mitocondriaMaskItem;
	}

	public TableFileItem getMicrotubulesStatsItem() {
		return microtubulesStatsItem;
	}

	public TableFileItem getMicrotubulesIndividualStatsItem() {
		return microtubulesIndividualStatsItem;
	}

	public TableFileItem getMicrotubulesMembraneLocationCell() {
		return microtubulesMembraneLocationCell;
	}

	public TableFileItem getGranulesStatsItem() {
		return granulesStatsItem;
	}

	public TableFileItem getGranulesIndividualStatsItem() {
		return granulesIndividualStatsItem;
	}

	public LabelTagItem getMicrotubulesGranulesRelationItem() {
		return microtubulesGranulesRelationItem;
	}

	public LabelTagItem getConnectedGranulesItem() {
		return connectedGranulesItem;
	}

	public LabelTagItem getMicrotubulesLength() {
		return microtubulesLength;
	}

	public LabelTagItem getConnectedMTGolgiItem() {
		return connectedMTGolgiItem;
	}

	public LabelTagItem getNotConnectedMTiItem() {
		return notConnectedMTItem;
	}

	public LabelTagItem getConnectedMTCentriolesItem() {
		return connectedMTCentriolesItem;
	}

	public LabelTagItem getGranulesMembraneRelationItem() {
		return granulesMembraneRelationItem;
	}

	public double getPixelToMicroMetersTiff() {
		return pixelToMicroMetersTiff;
	}

	public double getPixelToMicroMetersKnossos() {
		return pixelToMicroMetersKnossos;
	}

	public BdvProject app() {
		return app;
	}
}
