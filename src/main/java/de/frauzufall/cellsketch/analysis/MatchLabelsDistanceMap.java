package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.model.LabelTagItem;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.table.Table;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import de.frauzufall.cellsketch.model.LabelMapItemGroup;
import de.frauzufall.cellsketch.model.LabelMapTable;
import de.frauzufall.cellsketch.model.TableFileItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static de.frauzufall.cellsketch.analysis.LabelMapAnalyzer.getColumnIndex;

public class MatchLabelsDistanceMap {

	public void run(final TableFileItem table, final LabelMapItemGroup<? extends IntegerType<?>> labelMapItem, String name, final RandomAccessibleInterval<FloatType> distanceTransform, double pixelToUM, 	float connectedThresholdInUM) throws IOException {
		Map<Object, ClosestPoint> analysis = computeDistance(labelMapItem.getLabelMap().getModel(), distanceTransform);
		writeResultToTable(labelMapItem, table, name, analysis, pixelToUM, connectedThresholdInUM);
	}

	private void writeResultToTable(LabelMapItemGroup<? extends IntegerType<?>> labelMapItem, final TableFileItem tableItem, String name, Map<Object, ClosestPoint> analysis, double pixelToUM, double connectedThresholdInUM) throws IOException {
		Table table = tableItem.getTable();
		double max = 0;
		for (Map.Entry<Object, ClosestPoint> entry : analysis.entrySet()) {
			Object label = entry.getKey();
			ClosestPoint relation = entry.getValue();
			int rowIndex = table.getRowIndex(label.toString());
			if(rowIndex < 0) {
				rowIndex = table.getRowCount();
				table.appendRow(label.toString());
			}
			double value = pixelToUM * relation.distance;
			if(value > max) max = value;
			table.set(getColumnIndex(table, LabelMapTable.getDistanceToColumnName(name)), rowIndex, Double.toString(value));
			table.set(getColumnIndex(table, LabelMapTable.getConnectedToColumnName(name)), rowIndex, value < connectedThresholdInUM);
		}
		LabelTagItem label = labelMapItem.addLabelIfNotExists(LabelMapTable.getDistanceToColumnName(name), Double.class, false);
		label.setMaxValue(max);
		labelMapItem.addLabelIfNotExists(LabelMapTable.getConnectedToColumnName(name), Boolean.class, true);
		labelMapItem.saveConfig();
	}

	private static Map<Object, ClosestPoint> computeDistance(LabelEditorModel<IntType> model, RandomAccessibleInterval<? extends RealType> distanceTransform) {
		LabelRegions<IntType> newregions = new LabelRegions<>(model.labeling());
		Iterator<LabelRegion<IntType>> iterator = newregions.iterator();
		Map<Object, ClosestPoint> analysis = new HashMap<>();
//		ExecutorService pool = Executors.newFixedThreadPool(6);
//		CompletionService<Object> ecs = new ExecutorCompletionService<>(pool);
		final int size = model.labeling().getMapping().getLabels().size();
		while(iterator.hasNext()) {
			LabelRegion<IntType> labelRegion = iterator.next();
			closestPointRunnable(analysis, size, distanceTransform, labelRegion).run();
//			ecs.submit(Executors.callable(closestPointRunnable(analysis, size, distanceTransform, labelRegion)));
		}
//		for (int i = 0; i < size; ++i) {
//			try {
//				ecs.take().get();
//			} catch (InterruptedException | ExecutionException e) {
//				e.printStackTrace();
//			}
//		}
		return analysis;
	}

	private static Runnable closestPointRunnable(Map<Object, ClosestPoint> analysis, int size, RandomAccessibleInterval<? extends RealType> distanceTransform, LabelRegion<IntType> labelRegion) {
		return () -> {
			ClosestPoint nearest = getClosestPoint(distanceTransform, labelRegion);
			analysis.put(labelRegion.getLabel(), nearest);
//			System.out.println("Distance " + labelRegion.getLabel() + " (total " + size + ") : " + nearest.distance);
		};
	}

	private static ClosestPoint getClosestPoint(RandomAccessibleInterval<? extends RealType> distanceTransform, LabelRegion<IntType> labelRegion) {
		ClosestPoint res = new ClosestPoint();
		Point onGranule = new Point(distanceTransform.numDimensions());
		double minDistance = Double.MAX_VALUE;

		LabelRegionCursor cursor = labelRegion.localizingCursor();
		RandomAccess<? extends RealType> distanceAccess = distanceTransform.randomAccess();
		while(cursor.hasNext()) {
			cursor.next();
			distanceAccess.setPosition(cursor);
			if(distanceAccess.get().getRealDouble() < minDistance) {
				onGranule.setPosition(cursor);
				minDistance = distanceAccess.get().getRealDouble();
			}
		}
		res.point = onGranule;
		res.distance = minDistance;
		return res;
	}

}
