package de.csbdresden.betaseg.viewer;

import de.csbdresden.betaseg.BetaSegData;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;
import sc.fiji.project.BdvProject;
import sc.fiji.project.DefaultBdvProject;

import java.io.File;
import java.io.IOException;

@Plugin(type = Command.class,
		menuPath = "Analyze>BetaSeg", headless = true)
public class BetaSegViewer implements Command {

	@Parameter(label = "Workflow directory", style = FileWidget.DIRECTORY_STYLE)
	private File projectDir;

//	@Parameter(label = "Load displayable data")
	private boolean loadData = true;

	@Parameter(label = "Edit data")
	private boolean editMode = false;

	@Parameter
	private CommandService commandService;

	@Parameter
	private OpService opService;

	@Parameter
	private UIService uiService;

	@Parameter
	private Context context;

	private BdvProject app;
	private BetaSegData data;

	@Override
	public void run() {
		app = new DefaultBdvProject(projectDir, context);
		app.setEditable(editMode);
		data = new BetaSegData(app);
		setupColors();
		app.run();
		try {
			app.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(loadData) {
			loadData();
		}
	}

	private void setupColors() {
//		microtubulesLabelMapItem.setDefaultColor(ARGBType.rgba(255, 200, 0, 255));
//		granulesLabelMapItem.setDefaultColor(ARGBType.rgba(100, 100, 100, 100));

		data.getSourceItem().setColor(ARGBType.rgba(100, 100, 100, 255));

		data.getMicrotubulesLabelMapItem().setColor(ARGBType.rgba(255, 166, 214, 255));

		data.getGranulesLabelMapItem().setColor(ARGBType.rgba(255, 139, 115, 255));

		data.getGranulesMembraneRelationItem().setColor(ARGBType.rgba(255, 191, 178, 255));
		data.getGranulesMembraneRelationItem().setColorForMaxValues(false);
		data.getGranulesMembraneRelationItem().setMinValue(0);
		data.getGranulesMembraneRelationItem().setMaxValue(3.5);

		data.getConnectedGranulesItem().setColor(ARGBType.rgba(166, 253, 167, 200));

		data.getMicrotubulesGranulesRelationItem().setColor(ARGBType.rgba(153, 138, 162, 200));
		data.getMicrotubulesGranulesRelationItem().setColorForMaxValues(false);
		data.getMicrotubulesGranulesRelationItem().setMinValue(0);
		data.getMicrotubulesGranulesRelationItem().setMaxValue(1);

		data.getMicrotubulesLength().setColorForMaxValues(false);
		data.getMicrotubulesLength().setColor(ARGBType.rgba(200, 200, 200, 200));
		data.getMicrotubulesLength().setMinValue(0);
		data.getMicrotubulesLength().setMaxValue(10);

		data.getMembraneMaskItem().setColor(ARGBType.rgba(200, 200, 200, 155));

		data.getMitocondriaMaskItem().setColor(ARGBType.rgba(73 ,145, 202, 75));

		data.getNucleusMaskItem().setColor(ARGBType.rgba(255, 204, 104, 100));

		data.getMembraneDistanceMapItem().setMaxValue(100);
		data.getMicrotubulesDistanceMapItem().setMaxValue(30);
		data.getNucleusDistanceMapItem().setMaxValue(100);

		data.getGolgiMaskItem().setColor(ARGBType.rgba(50, 180, 137, 150));
		data.getGolgiDistanceTransformItem().setColor(ARGBType.rgba(90, 220, 177, 150));

		data.getConnectedMTCentriolesItem().setColor(ARGBType.rgba(255, 153, 0, 255));
		data.getConnectedMTGolgiItem().setColor(ARGBType.rgba(255, 255, 0, 255));
	}

	private void loadData() {
		data.getSourceItem().displayIfExists();
//		microtubulesMaskItem.displayIfExists();
//		granulesCoreMaskItem.displayIfExists();
//		data.getMembraneMaskItem().displayIfExists();
//		data.getNucleusMaskItem().displayIfExists();
//		data.getMicrotubulesLabelMapItem().displayIfExists();
//		data.getGranulesLabelMapItem().displayIfExists();
//		showInBvv();
//		showIn3DViewer();
	}

	public static void main(String...args) {
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(BetaSegViewer.class, true);
	}
}
