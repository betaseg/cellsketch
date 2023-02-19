package de.frauzufall.cellsketch.command;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.analysis.CellAnalyzer;
import net.imagej.ops.OpService;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Spatial analysis", headless = true)
public class CellSketchAnalyzer extends CommandWithCmdLineParser {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File project;

	@Parameter(label = "Max distance between two connected organelles in μm")
	@Option(name = "--connected_threshold_in_um")
	private float connectedThresholdInUM = 0.02f;


	@Parameter(label = "Skip existing distance transform maps.")
	@Option(name = "--skip_existing_distance_maps")
	private boolean skipExistingDistanceMaps = false;

	@Parameter(required = false)
	protected CellProject projectObject = null;

	@Parameter
	private Context context;

	@Parameter
	private OpService ops;

	@Override
	public void run() {
		if(projectObject == null) {
			projectObject = new CellProject(this.project, context);
			try {
				projectObject.loadConfig();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		new CellAnalyzer(projectObject, skipExistingDistanceMaps, ops, connectedThresholdInUM).analyze();
		projectObject.dispose();
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellSketchAnalyzer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.project != null) command_args.put("project", this.project);
		command_args.put("skipExistingDistanceMaps", this.skipExistingDistanceMaps);
		command_args.put("connectedThresholdInUM", this.connectedThresholdInUM);
		Context context = new Context();
		context.service(CommandService.class).run(this.getClass(), false, command_args).get();
		context.dispose();
		System.out.println("Done.");
	}
}
