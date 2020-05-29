package sc.fiji.project;

import net.imagej.ImageJ;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.GenericTable;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@Plugin(type = Command.class)
public class ExampleProjectCommand implements Command {

	@Parameter(label = "Project directory", style = FileWidget.DIRECTORY_STYLE)
	private File projectDir;

	@Parameter(label = "Load displayable data")
	private boolean loadData = true;

	@Parameter(label = "Edit data")
	private boolean editMode = false;

	@Parameter
	private CommandService commandService;

	@Parameter
	private UIService uiService;

	@Parameter
	private Context context;

	private ItemGroup mitochondriaGroup;
	private ItemGroup synapsesGroup;
	private ItemGroup unsortedGroup;

	private ImageFileItem sourceItem;

	private LabelMapFileItem noiseItem;

	private LabelMapFileItem membranesNeuritesGliaLabelsItem;

	private LabelMapFileItem synapsesLabelsItem;
	private TableFileItem synapsesStatsItem;
	private LabelTagItem synapsesSizeItem;

	private LabelMapFileItem mitochondriaLabelsItem;
	private TableFileItem mitochondriaStatsItem;
	private LabelTagItem mitochondriaSizeItem;

	private BdvProject project;

	@Override
	public void run() {
		project = new DefaultBdvProject(projectDir, context);
		project.setEditable(editMode);
		setupItems();
		setupColors();
		setupActions();
		project.setSourceItem(sourceItem);
		project.getItems().add(sourceItem);
		project.getItems().add(mitochondriaGroup);
		project.getItems().add(synapsesGroup);
		project.getItems().add(unsortedGroup);
		project.run();
		if(loadData) {
			loadData();
		}
	}

	private void setupItems() {
		sourceItem = new ImageFileItem(project, "Source");
		setupUnsortedGroup();
		setupMitochondria();
		setupSynapses();
	}

	private void setupUnsortedGroup() {
		membranesNeuritesGliaLabelsItem = new LabelMapFileItem(project, "Membranes, neurites, glia");
		noiseItem = new LabelMapFileItem(project, "Noise");
		unsortedGroup = new DefaultItemGroup("Unsorted");
		unsortedGroup.getItems().add(membranesNeuritesGliaLabelsItem);
		unsortedGroup.getItems().add(noiseItem);
	}

	private void setupMitochondria() {
		mitochondriaLabelsItem = new LabelMapFileItem(project, "Mitochondria labels");
		mitochondriaStatsItem = new TableFileItem(project, "Mitochondria stats", new MitochondriaTable());
		mitochondriaSizeItem = new LabelTagItem(project, "Mitochondria size", mitochondriaLabelsItem, mitochondriaStatsItem, MitochondriaTable.getSizeColumn());
		mitochondriaGroup = new DefaultItemGroup("Mitochondria");
		mitochondriaGroup.getItems().add(mitochondriaLabelsItem);
		mitochondriaGroup.getItems().add(mitochondriaStatsItem);
		mitochondriaGroup.getItems().add(mitochondriaSizeItem);
	}

	private void setupSynapses() {
		synapsesLabelsItem = new LabelMapFileItem(project, "Synapses labels");
		synapsesStatsItem = new TableFileItem(project, "Synapses stats", new SynapsesTable());
		synapsesSizeItem = new LabelTagItem(project, "Synapses size", synapsesLabelsItem, synapsesStatsItem, SynapsesTable.getSizeColumn());
		synapsesGroup = new DefaultItemGroup("Synapses");
		synapsesGroup.getItems().add(synapsesLabelsItem);
		synapsesGroup.getItems().add(synapsesStatsItem);
		synapsesGroup.getItems().add(synapsesSizeItem);
	}

	private void setupColors() {
//		microtubulesLabelMapItem.setDefaultColor(ARGBType.rgba(255, 200, 0, 255));
//		granulesLabelMapItem.setDefaultColor(ARGBType.rgba(100, 100, 100, 100));

		sourceItem.setColor(ARGBType.rgba(100, 100, 100, 255));

		synapsesLabelsItem.setColor(ARGBType.rgba(30, 150, 150, 255));

		mitochondriaLabelsItem.setColor(ARGBType.rgba(150, 30, 150, 255));

		mitochondriaSizeItem.setColor(ARGBType.rgba(255, 0, 0, 255));
		mitochondriaSizeItem.setMinValue(0);
		mitochondriaSizeItem.setMaxValue(90000);

		synapsesSizeItem.setColor(ARGBType.rgba(255, 0, 0, 255));
		synapsesSizeItem.setMinValue(0);
		synapsesSizeItem.setMaxValue(30000);

		membranesNeuritesGliaLabelsItem.setColor(0x212124ff);
	}

	private void setupActions() {
		if(project.isEditable()) {

			synapsesStatsItem.getActions().add(new DefaultAction(
					"Calculate",
					Collections.singletonList(synapsesLabelsItem),
					this::calculateSynapsesStats
			));
			mitochondriaStatsItem.getActions().add(new DefaultAction(
					"Calculate",
					Collections.singletonList(mitochondriaLabelsItem),
					this::calculateMitochondriaStats
			));
		}
	}

	private void calculateMitochondriaStats() {
		ImgLabeling<IntType, ? extends IntegerType<?>> labeling = mitochondriaLabelsItem.getModel().labeling();
		LabelRegions<IntType> regions = new LabelRegions<>(labeling);
		GenericTable table = mitochondriaStatsItem.getTable();
		if(table == null) table = SpecificTableBuilder.build(new MitochondriaTable());
		for (LabelRegion<IntType> region : regions) {
			table.appendRow(region.getLabel().toString());
			long size = region.size();
			table.set(MitochondriaTable.getSizeColumn(), table.getRowCount()-1, size);
		}
		mitochondriaStatsItem.setTable(table);
		try {
			mitochondriaStatsItem.save();
			project.save();
			project.updateUI();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void calculateSynapsesStats() {
		ImgLabeling<IntType, ? extends IntegerType<?>> labeling = synapsesLabelsItem.getModel().labeling();
		LabelRegions<IntType> regions = new LabelRegions<>(labeling);
		GenericTable table = synapsesStatsItem.getTable();
		if(table == null) table = SpecificTableBuilder.build(new SynapsesTable());
		for (LabelRegion<IntType> region : regions) {
			table.appendRow(region.getLabel().toString());
			table.set(SynapsesTable.getSizeColumn(), table.getRowCount()-1, region.size());
		}
		synapsesStatsItem.setTable(table);
		try {
			synapsesStatsItem.save();
			project.save();
			project.updateUI();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadData() {
		sourceItem.displayIfExists();
		mitochondriaLabelsItem.displayIfExists();
//		noiseItem.displayIfExists();
//		membranesNeuritesGliaLabelsItem.displayIfExists();
		synapsesLabelsItem.displayIfExists();
	}

	public static void main(String...args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(ExampleProjectCommand.class, true);
	}
}
