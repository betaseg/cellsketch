package de.frauzufall.cellviewer.command;

import de.frauzufall.cellviewer.CellData;
import de.frauzufall.cellviewer.analysis.MicrotubulesAnalyzer;
import de.frauzufall.cellviewer.analysis.NMLReader;
import org.jdom2.DataConversionException;
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
import sc.fiji.project.FileItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

@Plugin(type = Command.class,
		menuPath = "Analyze>Cell Viewer>New cell", headless = true)
public class CellImporter implements Command {

	@Parameter(label = "Project parent directory", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--parent")
	private File parent;

	@Parameter(label = "Project name")
	@Option(name = "--name")
	private String projectName;

	@Parameter(label = "Raw dataset")
	@Option(name = "--raw")
	private File input;

	@Parameter(label = "Filled membrane mask", required = false)
	@Option(name = "--filled_membrane_mask")
	private File filledMembraneMask;

	@Parameter(label = "Nucleus mask", required = false)
	@Option(name = "--nucleus_mask")
	private File nucleusMask;

	@Parameter(label = "Mitochondria mask", required = false)
	@Option(name = "--mitochondria_mask")
	private File mitochondriaMask;

	@Parameter(label = "Golgi mask", required = false)
	@Option(name = "--golgi_mask")
	private File golgiMask;

	@Parameter(label = "Centrioles mask", required = false)
	@Option(name = "--centrioles_mask")
	private File centriolesMask;

	@Parameter(label = "Granules index map", required = false)
	@Option(name = "--granules_labelmap")
	private File granulesMap;

	@Parameter(label = "Microtubules KNOSSOS file", required = false)
	@Option(name = "--microtubules_knossos")
	private File microtubulesKnossosFile;

	@Parameter(label = "Microtubules scale factor X", stepSize="0.0001")
	@Option(name = "--microtubules_scale_x")
	double microtubulesScaleX = 0.25;

	@Parameter(label = "Microtubules scale factor Y", stepSize="0.0001")
	@Option(name = "--microtubules_scale_y")
	double microtubulesScaleY = 0.25;

	@Parameter(label = "Microtubules scale factor Y", stepSize="0.0001")
	@Option(name = "--microtubules_scale_z")
	double microtubulesScaleZ = 0.2125;

//	double scale_low = 4.24 / 4.;
//	double scale_high = 3.4 / 4.;

	@Parameter
	private Context context;

	@Override
	public void run() {
		System.out.println("Importing cell data into " + parent + File.separator + projectName + "..");
		BdvProject app = new DefaultBdvProject(parent, projectName, context);
		app.setEditable(true);
		CellData data = new CellData(app);
		app.create(input, data.getSourceItem());
		try {
			loadFileItem(nucleusMask, data.getNucleusMaskItem());
			loadFileItem(mitochondriaMask, data.getMitocondriaMaskItem());
			loadFileItem(golgiMask, data.getGolgiMaskItem());
			loadFileItem(centriolesMask, data.getCentriolesMaskItem());
			loadFileItem(filledMembraneMask, data.getMembraneFullMaskItem());
			loadFileItem(granulesMap, data.getGranulesLabelMapItem());
			loadFileItem(microtubulesKnossosFile, data.getMicrotubulesKnossosItem());
			if(microtubulesKnossosFile != null && microtubulesKnossosFile.exists()) {
				MicrotubulesAnalyzer analyzer = new MicrotubulesAnalyzer(data);
				analyzer.processKnossosMTs(microtubulesKnossosFile, microtubulesScaleX, microtubulesScaleY, microtubulesScaleZ);
				analyzer.render();
			} else {
				System.out.println("Not rendering Microtubules, KNOSSOS file missing.");
			}
		} catch (IOException | NMLReader.NMLReaderIOException | DataConversionException e) {
			e.printStackTrace();
		}
	}

	private void loadFileItem(File inputFile, FileItem fileItem) throws IOException {
		if (inputFile != null && inputFile.exists()) fileItem.importAsFile(inputFile);
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellImporter().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java CellImporter"+parser.printExample(ALL));
			return;
		}
		Map<String, Object> command_args = new HashMap<>();
		command_args.put("projectName", this.projectName);
		command_args.put("parent", this.parent);
		command_args.put("input", this.input);
		command_args.put("filledMembraneMask", this.filledMembraneMask);
		command_args.put("nucleusMask", this.nucleusMask);
		command_args.put("mitochondriaMask", this.mitochondriaMask);
		command_args.put("golgiMask", this.golgiMask);
		command_args.put("centriolesMask", this.centriolesMask);
		command_args.put("granulesMap", this.granulesMap);
		command_args.put("microtubulesKnossosFile", this.microtubulesKnossosFile);
		command_args.put("microtubulesScaleX", this.microtubulesScaleX);
		command_args.put("microtubulesScaleY", this.microtubulesScaleY);
		command_args.put("microtubulesScaleZ", this.microtubulesScaleZ);
		Context context = new Context();
		context.service(CommandService.class).run(CellImporter.class, false, command_args).get();
		context.dispose();
		System.out.println("Done.");
	}
}
