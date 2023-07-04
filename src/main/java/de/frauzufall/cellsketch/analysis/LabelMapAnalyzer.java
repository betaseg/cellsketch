package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.model.*;
import net.imagej.ops.OpService;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.scijava.app.StatusService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.Table;
import sc.fiji.labeleditor.core.model.LabelEditorModel;

import java.io.IOException;
import java.util.Set;

import static de.frauzufall.cellsketch.analysis.AnalyzeUtils.getTableData;

public class LabelMapAnalyzer {
	private final CellProject project;
	private final OpService opService;
	private final LabelMapItemGroup<? extends IntegerType<?>> labelMap;
	private final float connectedThresholdInUM;

	public LabelMapAnalyzer(CellProject project, LabelMapItemGroup<? extends IntegerType<?>> labelMap, float connectedThresholdInUM) {
		this.project = project;
		this.labelMap = labelMap;
		this.connectedThresholdInUM = connectedThresholdInUM;
		this.opService = project.context().service(OpService.class);
	}

	public void analyze() throws IOException {
		if(labelMap.getLabelMap().exists()) {
			writeTables();
			for (LabelMapItemGroup item : project.getLabelMapItems()) {
				analyzeLabelMapDistanceRelation(item);
			}
			for (LabelMapItemGroup item : project.getFilamentsItems()) {
				analyzeLabelMapDistanceRelation(item);
			}
			for (MaskItemGroup item : project.getMaskItems()) {
				analyzeLabelMapDistanceRelation(item);
			}
			if(project.getBoundary() != null) {
				analyzeLabelMapDistanceRelation(project.getBoundary());
			}
//			exportMasks();
		} else {
			project.context().service(StatusService.class).showStatus("Cannot analyze labels, label map not found.");
		}
	}

	private void exportMasks() {
		labelMap.getLabelMap().getTagItems().forEach(item -> {
			item.exportMask();
			if(item.getTagClass() == Boolean.class) {
				item.exportInvertedMask();
			}
		});
	}

	void writeTables() throws IOException {
		Table detailsTable = labelMap.getIndividualStats().getTable();
		if(detailsTable == null) {
			detailsTable = new DefaultGenericTable();
			labelMap.getIndividualStats().setTable(detailsTable);
		}
		Table summaryTable = labelMap.getOverallStats().getTable();
		if(summaryTable == null) {
			summaryTable = new DefaultGenericTable();
			labelMap.getOverallStats().setTable(summaryTable);
		}
		run(detailsTable, summaryTable, labelMap);
		try {
			labelMap.getOverallStats().save();
			labelMap.getIndividualStats().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		labelMap.saveConfig();
	}

	public void run(Table detailsTable, Table summaryTable, LabelMapItemGroup<? extends IntegerType<?>> labelmap) {
		LabelEditorModel<IntType> model = labelmap.getLabelMap().getModel();
		Set<IntType> labels = model.labeling().getMapping().getLabels();
		double[] sizes = new double[labels.size()];

		LabelRegions<IntType> regions = new LabelRegions<>(model.labeling());
		int i = 0;
		double maxSize = 0;
		int sizeIndex = getColumnIndex(detailsTable, LabelMapTable.getSizeColumnName());
		for (LabelRegion<IntType> region : regions) {
			double size = region.size()*Math.pow(project.getPixelToUM(), 3);
			sizes[i] = size;
			int row = detailsTable.getRowIndex(region.getLabel().toString());
			if(row < 0) {
				row = detailsTable.getRowCount();
				detailsTable.appendRow(region.getLabel().toString());
			}
			if(size > maxSize) maxSize = size;
			detailsTable.set(sizeIndex, row, String.valueOf(size));
			i++;
		}
		LabelTagItem tag = labelmap.addLabelIfNotExists(LabelMapTable.getSizeColumnName(), Double.class, true);
		tag.setMaxValue(maxSize);
		if(summaryTable.getRowIndex("all") < 0) {
			summaryTable.appendRow("all");
		}
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getCountColumnName()), 0, String.valueOf(sizes.length));
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getMeanSizeColumnName()), 0, String.valueOf(new Mean().evaluate(sizes)));
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getStdevSizeColumnName()), 0, String.valueOf(new StandardDeviation().evaluate(sizes)));
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getMedianSizeColumnName()), 0, String.valueOf(new Median().evaluate(sizes)));
//		System.out.println(summaryTable);
//		System.out.println(detailsTable);
	}

	public static int getColumnIndex(Table detailsTable, String columnName) {
		if(detailsTable.getColumnIndex(columnName) < 0) {
			detailsTable.appendColumn(columnName);
		}
		int sizeIndex = detailsTable.getColumnIndex(columnName);
		return sizeIndex;
	}

	private void analyzeLabelMapDistanceRelation(HasDistanceMap item) {
		if(item == null || !item.getDistanceMap().exists()) return;
		if(item == labelMap) return;
		Table table = labelMap.getIndividualStats().getTable();
		if(table == null) {
			table = new DefaultGenericTable();
			labelMap.getIndividualStats().setTable(table);
			try {
				labelMap.getIndividualStats().save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		MatchLabelsDistanceMap matcher = new MatchLabelsDistanceMap();
		try {
			matcher.run(labelMap.getIndividualStats(),
					labelMap,
					item.getName(),
					item.getDistanceMap().getImage(),
					project.getPixelToUM(),
					connectedThresholdInUM);
			labelMap.getIndividualStats().save();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			item.getDistanceMap().unload();
		}
		calculateConnectedPercentages(item);
	}

	private void calculateConnectedPercentages(HasDistanceMap item) {
		Table table = labelMap.getIndividualStats().getTable();
		int labelCount = table.getRowCount();
		int connected = getTableData(table,
				getColumnIndex(table, LabelMapTable.getConnectedToColumnName(item.getName())),
				getColumnIndex(table, LabelMapTable.getConnectedToColumnName(item.getName())), "true").size();
		Table summaryTable = labelMap.getOverallStats().getTable();
		if(summaryTable == null) {
			summaryTable = new DefaultGenericTable();
			labelMap.getOverallStats().setTable(summaryTable);
		}
		String percentage = (int)((float)connected/(float)labelCount*100) + " %";
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getPercentageConnectedToColumnName(item.getName())), 0, percentage);
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getNumberConnectedToColumnName(item.getName())), 0, connected);
		summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getNumberDisconnectedFronColumnName(item.getName())), 0, labelCount-connected);
		try {
			labelMap.getOverallStats().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
