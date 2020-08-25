package de.csbdresden.betaseg.analysis;

import de.csbdresden.betaseg.BetaSegData;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.boundary.Boundary;
import net.imglib2.type.logic.BitType;
import net.imglib2.view.Views;
import org.jdom2.DataConversionException;
import org.scijava.Context;
import sc.fiji.project.DefaultBdvProject;
import sc.fiji.project.MaskFileItem;

import java.io.File;
import java.io.IOException;

public class BetaSegAnalyzer {

	private final BetaSegData data;
	private final String cell;
	private final OpService ops;

	public BetaSegAnalyzer(String projectPath, String cell, Context context) throws IOException {
		DefaultBdvProject app = new DefaultBdvProject(new File(projectPath, cell), context);
		data = new BetaSegData(app);
		this.cell = cell;
		app.loadProject();
		ops = data.app().context().service(OpService.class);
	}

	public void createMissingData() {
		if(!data.getMembraneMaskItem().exists()) calculateBoundary(data.getMembraneFullMaskItem(), data.getMembraneMaskItem());
	}

	private void calculateBoundary(MaskFileItem input, MaskFileItem output) {
		IterableRegion<BitType> region = Regions.iterable(ops.convert().bit(Views.iterable(input.getImage())));
		Boundary res = new Boundary<>(region);
		output.setImage(ops.convert().int8(Views.iterable((RandomAccessibleInterval<BitType>)res)));
		try {
			output.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void analyze() throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
//		createMissingData();
//		analyzeMembrane();
//		analyzeNucleus();
//		analyzeGolgi();
		analyzeMicrotubules();
//		analyzeGranules();
	}

	private void analyzeMembrane() {
		AnalyzeUtils.calculateDistanceTransform(ops, data.getMembraneMaskItem(), data.getMembraneDistanceMapItem());
	}

	private void analyzeNucleus() {
		AnalyzeUtils.calculateDistanceTransform(ops, data.getNucleusMaskItem(), data.getNucleusDistanceMapItem());
	}

	private void analyzeGolgi() {
		AnalyzeUtils.calculateDistanceTransform(ops, data.getGolgiMaskItem(), data.getGolgiDistanceTransformItem());
	}

	private void analyzeMicrotubules() throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
		String sortedYAML = cell + "_microtubules_sorted.yaml";
		String indexImage = cell + "_microtubules.tif";
		String distanceMapImage = cell + "_microtubules_distance_map.tif";
		String sumTable = cell + "_microtubules.csv";
		String individualTable = cell + "_microtubules_individual.csv";
		Context context = new Context();
		MicrotubulesAnalyzer analyzer = new MicrotubulesAnalyzer(data);
//		analyzer.processKnossosMTs(cell + "_MTs_minus_MTOC.xml", sortedYAML);
//		analyzer.render(sortedYAML, indexImage, distanceMapImage);
		analyzer.analyze(sortedYAML, sumTable, individualTable);
	}

	private void analyzeGranules() {
		GranulesAnalyzer analyzer = new GranulesAnalyzer(data);
		analyzer.analyze();
	}

	public static void main(String...args) throws IOException, DataConversionException, NMLReader.NMLReaderIOException {
		Context context = new Context();
		String projectPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/";
		new BetaSegAnalyzer(projectPath, "high_c1", context).analyze();
		new BetaSegAnalyzer(projectPath, "high_c2", context).analyze();
		new BetaSegAnalyzer(projectPath, "high_c3", context).analyze();
//		new BetaSegAnalyzer(projectPath, "high_c4", context).analyze();
//		new BetaSegAnalyzer(projectPath, "low_c1", context).analyze();
//		new BetaSegAnalyzer(projectPath, "low_c2", context).analyze();
//		new BetaSegAnalyzer(projectPath, "low_c3", context).analyze();
		context.dispose();
	}

}
