package de.frauzufall.cellsketch.command;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.analysis.NMLReader;
import net.imglib2.type.numeric.ARGBType;
import org.jdom2.DataConversionException;
import org.kohsuke.args4j.Option;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
		menuPath = "Analyze>" + CellProject.appName + ">Add filaments from KNOSSOS", headless = true)
public class AddFilamentsFromKNOSSOS extends CommandColoredEntity {

	@Parameter(label = "Filaments KNOSSOS file", required = false)
	@Option(name = "--input")
	private File input;

//	double scale_low = 4.24 / 4.;
//	double scale_high = 3.4 / 4.;

	@Override
	public void run() {
		CellProject project = getOrLoadCellProject();
		String progressName = "Adding boundary from " + input;
		try {
			project.startProgress(progressName);
			project.addFilamentsFromKNOSSOS(input, name, ARGBType.rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()), scaleX, scaleY, scaleZ);
			project.configChanged();
		} catch (IOException | NMLReader.NMLReaderIOException | DataConversionException e) {
			e.printStackTrace();
		} finally {
			project.endProgress(progressName);
		}
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new AddFilamentsFromKNOSSOS().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		if (!parseArguments(args)) return;
		Map<String, Object> command_args = new HashMap<>();
		if(this.project != null) command_args.put("project", this.project);
		command_args.put("color", this.color);
		command_args.put("input", this.input);
		command_args.put("name", this.name);
		command_args.put("scaleX", this.scaleX);
		command_args.put("scaleY", this.scaleY);
		command_args.put("scaleZ", this.scaleZ);
		Context context = new Context();
		context.service(CommandService.class).run(this.getClass(), false, command_args).get();
		context.dispose();
		System.out.println("Done.");
	}
}
