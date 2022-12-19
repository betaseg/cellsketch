package de.frauzufall.cellviewer.analysis;

import de.frauzufall.cellviewer.CellData;
import de.frauzufall.cellviewer.GranulesOverviewTable;
import de.frauzufall.cellviewer.GranulesTable;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.scijava.table.Table;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.project.SpecificTableBuilder;

import java.io.IOException;
import java.util.Set;

import static de.frauzufall.cellviewer.analysis.AnalyzeUtils.getTableData;
import static sc.fiji.project.LabelMapFileItem.asMask;

public class GranulesAnalyzer {
	private final CellData data;
	private final OpService opService;

	public GranulesAnalyzer(CellData data) {
		this.data = data;
		this.opService = data.app().context().service(OpService.class);
	}

	public void analyze(double pixelToMicroMeters) throws IOException {
		if(data.getGranulesLabelMapItem().exists()) {
			analyzeGranules(pixelToMicroMeters);
			analyzeGranulesMicrotubulesRelation(pixelToMicroMeters);
			calculateConnectedGranulesPercentages();
			analyzeGranulesMembraneRelation(pixelToMicroMeters);
			analyzeGranulesNucleusRelation(pixelToMicroMeters);
			analyzeGranulesGolgiRelation(pixelToMicroMeters);
			calculateSpaceForGranules();
			exportMasks();
		} else {
			System.out.println("Cannot analyze granules, label map not found.");
		}
	}

	private void exportMasks() {
		data.getGranulesConnectedToMicrotubules().exportMask();
		data.getGranulesConnectedToMicrotubules().exportInvertedMask();
	}

	void analyzeGranules(double pixelToMicroMeters) {
		Table detailsTable = data.getGranulesIndividualStatsItem().getTable();
		if(detailsTable == null) {
			detailsTable = SpecificTableBuilder.build(new GranulesTable());
			data.getGranulesIndividualStatsItem().setTable(detailsTable);
		}
		Table summaryTable = data.getGranulesStatsItem().getTable();
		if(summaryTable == null) {
			summaryTable = SpecificTableBuilder.build(new GranulesOverviewTable());
			data.getGranulesStatsItem().setTable(summaryTable);
		}
		run(detailsTable, summaryTable, data.getGranulesLabelMapItem().getModel(), pixelToMicroMeters);
		try {
			data.getGranulesStatsItem().save();
			data.getGranulesIndividualStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run(Table detailsTable, Table summaryTable, LabelEditorModel<IntType> model, double pixelToMicroMeters) {
		Set<IntType> labels = model.labeling().getMapping().getLabels();
		double[] sizes = new double[labels.size()];

		LabelRegions<IntType> regions = new LabelRegions<>(model.labeling());
		int i = 0;
		for (LabelRegion<IntType> region : regions) {
			double size = region.size()*Math.pow(pixelToMicroMeters, 3);
			sizes[i] = size;
			int row = detailsTable.getRowIndex(region.getLabel().toString());
			if(row < 0) {
				row = detailsTable.getRowCount();
				detailsTable.appendRow(region.getLabel().toString());
			}
			detailsTable.set(GranulesTable.getSizeColumn(), row, String.valueOf(size));
			i++;
		}
		if(summaryTable.getRowIndex("all") < 0) {
			summaryTable.appendRow("all");
		}
		summaryTable.set(GranulesOverviewTable.getCountColumn(), 0, String.valueOf(sizes.length));
		summaryTable.set(GranulesOverviewTable.getMeanSizeColumn(), 0, String.valueOf(new Mean().evaluate(sizes)));
		summaryTable.set(GranulesOverviewTable.getStdevSizeColumn(), 0, String.valueOf(new StandardDeviation().evaluate(sizes)));
		summaryTable.set(GranulesOverviewTable.getMedianSizeColumn(), 0, String.valueOf(new Median().evaluate(sizes)));
		System.out.println(summaryTable);
	}

	private void calculateSpaceForGranules() throws IOException {
		Img<ByteType> res = new ArrayImgFactory<>(new ByteType()).create(data.getMembraneFullMaskItem().getImage());
		RandomAccessibleInterval<BitType> membraneFullMask = opService.convert().bit(Views.iterable(data.getMembraneFullMaskItem().getImage()));
		RandomAccessibleInterval<BitType> nucleusMask = opService.convert().bit(Views.iterable(data.getNucleusMaskItem().getImage()));
		RandomAccessibleInterval<ByteType> mtMask = asMask(data.getMicrotubulesLabelMapItem().getImage());
		LoopBuilder.setImages(res, membraneFullMask,
				nucleusMask, mtMask).multiThreaded()
				.forEachPixel((resPixel, cellPixel, nucleusPixel, mtPixel) -> {
					if(cellPixel.get() && !nucleusPixel.get() && mtPixel.getInteger() == 0)
						resPixel.setOne();
				});
		data.getSpaceForGranulesItem().setImage(res);
		try {
			data.getSpaceForGranulesItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		AnalyzeUtils.calculateDistanceTransform(opService, res, data.getSpaceForGranulesDistanceTransformItem());
	}

	private void analyzeGranulesMicrotubulesRelation(double pixelToMicroMeters) {
		Table table = data.getGranulesIndividualStatsItem().getTable();
		if(table == null) {
			table = SpecificTableBuilder.build(new GranulesTable());
			data.getGranulesIndividualStatsItem().setTable(table);
			try {
				data.getGranulesIndividualStatsItem().save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		MatchFullGranulesMicrotubules matcher = new MatchFullGranulesMicrotubules();
		matcher.run(table,
				data.getGranulesLabelMapItem().getModel(),
				data.getMicrotubulesDistanceMapItem().getImage(),
				pixelToMicroMeters);
		try {
			data.getGranulesIndividualStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		data.getMicrotubulesDistanceMapItem().unload();
	}

	private void analyzeGranulesNucleusRelation(double pixelToMicroMeters) {
		Table table = data.getGranulesIndividualStatsItem().getTable();
		if(table == null) {
			table = SpecificTableBuilder.build(new GranulesTable());
			data.getGranulesIndividualStatsItem().setTable(table);
		}
		MatchLabelsDistanceMap matcher = new MatchLabelsDistanceMap(data.app().context());
		matcher.run(data.getGranulesLabelMapItem().getModel(), data.getNucleusDistanceMapItem().getImage());
		matcher.writeResultToTable(table,
				GranulesTable.getDistanceToNucleusColumn(),
				pixelToMicroMeters);
		try {
			data.getGranulesIndividualStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		data.getNucleusDistanceMapItem().unload();
	}

	private void analyzeGranulesGolgiRelation(double pixelToMicroMeters) {
		Table table = data.getGranulesIndividualStatsItem().getTable();
		if(table == null) {
			table = SpecificTableBuilder.build(new GranulesTable());
			data.getGranulesIndividualStatsItem().setTable(table);
		}
		MatchLabelsDistanceMap matcher = new MatchLabelsDistanceMap(data.app().context());
		matcher.run(data.getGranulesLabelMapItem().getModel(), data.getGolgiDistanceTransformItem().getImage());
		matcher.writeResultToTable(table,
				GranulesTable.getDistanceToGolgiColumn(),
				pixelToMicroMeters);
		try {
			data.getGranulesIndividualStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		data.getGolgiDistanceTransformItem().unload();
	}

	private void analyzeGranulesMembraneRelation(double pixelToMicroMeters) {
		Table table = data.getGranulesIndividualStatsItem().getTable();
		if(table == null) {
			table = SpecificTableBuilder.build(new GranulesTable());
			data.getGranulesIndividualStatsItem().setTable(table);
		}
		MatchLabelsDistanceMap matcher = new MatchLabelsDistanceMap(data.app().context());
		matcher.run(data.getGranulesLabelMapItem().getModel(), data.getMembraneDistanceMapItem().getImage());
		matcher.writeResultToTable(table,
				GranulesTable.getDistanceToMembraneColumn(),
				pixelToMicroMeters);
		try {
			data.getGranulesIndividualStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		data.getMembraneDistanceMapItem().unload();
	}

	private void calculateConnectedGranulesPercentages() {
		int allGranules = data.getGranulesIndividualStatsItem().getTable().getRowCount();
		int connected = getTableData(data.getGranulesIndividualStatsItem().getTable(), GranulesTable.getConnectedToMicrotubuleColumn(), GranulesTable.getConnectedToMicrotubuleColumn(), "true").size();
		Table summaryTable = data.getGranulesStatsItem().getTable();
		if(summaryTable == null) {
			summaryTable = SpecificTableBuilder.build(new GranulesOverviewTable());
			data.getGranulesStatsItem().setTable(summaryTable);
		}
		String percentage = (int)((float)connected/(float)allGranules*100) + " %";
		summaryTable.set(GranulesOverviewTable.getPercentageConnectedToMTColumn(), 0, percentage);
		summaryTable.set(GranulesOverviewTable.getNumberConnectedToMTColumn(), 0, connected);
		summaryTable.set(GranulesOverviewTable.getNumberDisconnectedFronMTColumn(), 0, allGranules-connected);
		try {
			data.getGranulesStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
