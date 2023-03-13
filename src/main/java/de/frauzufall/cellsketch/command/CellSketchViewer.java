package de.frauzufall.cellsketch.command;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.CellSketch;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Display project", headless = true)
public class CellSketchViewer extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File projectDir;

	@Parameter
	private Context context;

	@Override
	public void run() {
		CellProject project = new CellProject(projectDir, context);
		try {
			project.load();
			project.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellSketchViewer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.projectDir != null) command_args.put("projectDir", this.projectDir);
		CellSketch cellSketch = new CellSketch();
		cellSketch.ui().showUI();
		cellSketch.command().run(this.getClass(), true, command_args).get();
	}
}
