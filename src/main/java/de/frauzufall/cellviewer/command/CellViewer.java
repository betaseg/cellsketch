package de.frauzufall.cellviewer.command;

import de.frauzufall.cellviewer.CellData;
import net.imagej.ImageJ;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import sc.fiji.project.BdvProject;
import sc.fiji.project.DefaultBdvProject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

@Plugin(type = Command.class,
		menuPath = "Analyze>Cell Viewer>Display in BDV", headless = true)
public class CellViewer implements Command {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File projectDir;

	@Parameter
	private Context context;

	@Override
	public void run() {
		BdvProject app = new DefaultBdvProject(projectDir, context);
		app.setEditable(false);
		CellData data = new CellData(app);
		app.run();
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellViewer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java CellViewer"+parser.printExample(ALL));
			return;
		}
		Map<String, Object> command_args = new HashMap<>();
		if(this.projectDir != null) command_args.put("projectDir", this.projectDir);
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run(CellViewer.class, true, command_args).get();
	}
}
