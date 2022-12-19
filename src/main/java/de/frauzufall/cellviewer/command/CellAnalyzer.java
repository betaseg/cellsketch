package de.frauzufall.cellviewer.command;

import de.frauzufall.cellviewer.CellData;
import de.frauzufall.cellviewer.analysis.AnalyzeUtils;
import de.frauzufall.cellviewer.analysis.GranulesAnalyzer;
import de.frauzufall.cellviewer.analysis.MicrotubulesAnalyzer;
import de.frauzufall.cellviewer.analysis.NMLReader;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.boundary.Boundary;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;
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
import sc.fiji.project.DefaultBdvProject;
import sc.fiji.project.MaskFileItem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.kohsuke.args4j.OptionHandlerFilter.ALL;

@Plugin(type = Command.class,
		menuPath = "Analyze>Cell Viewer>Spatial analysis", headless = true)
public class CellAnalyzer implements Command {

	@Parameter(label = "Cell project directory (.n5)", style = FileWidget.DIRECTORY_STYLE)
	@Option(name = "--project")
	private File projectDir;

	@Parameter(label = "Pixel of imported masks to μm factor")
	@Option(name = "--pixelToMicroMeters")
	private double pixelToMicroMeters = 0.004 *4;

	@Parameter
	private Context context;

	@Parameter
	private OpService ops;

	private CellData data;

	@Override
	public void run() {
		try {
			DefaultBdvProject app = new DefaultBdvProject(projectDir, context);
			data = new CellData(app);
			createMissingData();
			analyzeMembrane();
			analyzeNucleus();
			analyzeGolgi();
			analyzeCentrioles();
			analyzeMicrotubules();
			analyzeGranules();
		} catch (IOException | NMLReader.NMLReaderIOException | DataConversionException e) {
			e.printStackTrace();
		}
	}


	public void createMissingData() {
		if (!data.getMembraneMaskItem().exists()) {
			if(data.getMembraneFullMaskItem().exists()) {
				calculateBoundary(data.getMembraneFullMaskItem(), data.getMembraneMaskItem());
				data.getMembraneFullMaskItem().unload();
			} else {
				System.out.println("Cannot generate membrane mask from full mask, full mask not found.");
			}
		}
	}

	private void calculateBoundary(MaskFileItem input, MaskFileItem output) {
		output.setImage(boundary(input.getImage()));
		try {
			output.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <T extends RealType<T>> Img<ByteType> boundary(RandomAccessibleInterval<T> image) {
		RandomAccessibleInterval<BoolType> res = Converters.convert(image, (input, output) -> output.set(input.getRealFloat() > 0), new BoolType());
		return ops.convert().int8(Views.iterable(new Boundary<>(res)));
	}

	private void analyzeMembrane() throws IOException {
		if(!data.getMembraneDistanceMapItem().exists()) {
			if(data.getMembraneMaskItem().exists()) {
				AnalyzeUtils.calculateDistanceTransform(ops, data.getMembraneMaskItem(), data.getMembraneDistanceMapItem());
			} else {
				System.out.println("Cannot calculate distance transform of membrane mask, mask not found.");
			}
		}
		data.getMembraneMaskItem().unload();
		data.getMembraneDistanceMapItem().unload();
	}

	private void analyzeNucleus() throws IOException {
		if(!data.getNucleusDistanceMapItem().exists()) {
			if(data.getNucleusMaskItem().exists()) {
				AnalyzeUtils.calculateDistanceTransform(ops, data.getNucleusMaskItem(), data.getNucleusDistanceMapItem());
			} else {
				System.out.println("Cannot calculate distance transform of nucleus mask, mask not found.");
			}
		}
		data.getNucleusMaskItem().unload();
		data.getNucleusDistanceMapItem().unload();
	}

	private void analyzeGolgi() throws IOException {
		if(!data.getGolgiDistanceTransformItem().exists()) {
			if(data.getGolgiMaskItem().exists()) {
				AnalyzeUtils.calculateDistanceTransform(ops, data.getGolgiMaskItem(), data.getGolgiDistanceTransformItem());
			} else {
				System.out.println("Cannot calculate distance transform of golgi mask, mask not found.");
			}
			data.getGolgiMaskItem().unload();
			data.getGolgiDistanceTransformItem().unload();
		}
	}

	private void analyzeCentrioles() throws IOException {
		if(!data.getCentriolesDistanceTransformItem().exists()) {
			if(data.getCentriolesMaskItem().exists()) {
				AnalyzeUtils.calculateDistanceTransform(ops, data.getCentriolesMaskItem(), data.getCentriolesDistanceTransformItem());
			} else {
				System.out.println("Cannot calculate distance transform of centrioles mask, mask not found.");
			}
			data.getCentriolesMaskItem().unload();
			data.getCentriolesDistanceTransformItem().unload();
		}
	}

	private void analyzeMicrotubules() throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
		MicrotubulesAnalyzer analyzer = new MicrotubulesAnalyzer(data);
		analyzer.analyze();
	}

	private void analyzeGranules() throws IOException {
		GranulesAnalyzer analyzer = new GranulesAnalyzer(data);
		analyzer.analyze(pixelToMicroMeters);
	}

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		new CellAnalyzer().doMain(args);
	}

	public void doMain(String[] args) throws ExecutionException, InterruptedException {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch(CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println();
			System.err.println("  Example: java CellAnalyzer"+parser.printExample(ALL));
			return;
		}
		Map<String, Object> command_args = new HashMap<>();
		if(this.projectDir != null) command_args.put("projectDir", this.projectDir);
		command_args.put("pixelToMicroMeters", this.pixelToMicroMeters);
		Context context = new Context();
		context.service(CommandService.class).run(CellAnalyzer.class, false, command_args).get();
		System.out.println("Done.");
	}
}
