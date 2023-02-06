package de.frauzufall.cellsketch.command;

import de.frauzufall.cellsketch.CellProject;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.util.ColorRGBA;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;

public abstract class CommandColoredEntity extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	protected File project;

	@Parameter(required = false)
	protected CellProject projectObject;

	@Parameter(label = "Name of input dataset")
	@Option(name = "--name")
	protected String name;

	@Parameter(label = "Scale factor X for input dataset", stepSize="0.0001")
	@Option(name = "--scale_x")
	protected double scaleX = 1;
//	protected double scaleX = 0.25;

	@Parameter(label = "Scale factor Y for input dataset", stepSize="0.0001")
	@Option(name = "--scale_y")
	protected double scaleY = 1;

	@Parameter(label = "Scale factor Z for input dataset", stepSize="0.0001")
	@Option(name = "--scale_z")
	protected double scaleZ = 1;
//	protected double scaleZ = 0.2125;

	@Parameter(label = "Color", required = false)
	@Option(name = "--color")
	protected ColorRGBA color = new ColorRGBA(200, 200, 200, 200);

	@Parameter
	protected Context context;

	protected CellProject getOrLoadCellProject() {
		CellProject project;
		if(projectObject != null) {
			project = projectObject;
		} else {
			project = new CellProject(this.project, context);
			try {
				project.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return project;
	}
}
