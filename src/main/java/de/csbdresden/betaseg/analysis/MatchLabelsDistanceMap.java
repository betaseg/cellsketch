package de.csbdresden.betaseg.analysis;

import net.imagej.ops.OpService;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.table.Table;
import org.scijava.ui.UIService;
import sc.fiji.labeleditor.core.model.LabelEditorModel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MatchLabelsDistanceMap {

	private final OpService opService;
	private final IOService ioService;
	private final UIService uiService;
	private Map<Object, ClosestPoint> analysis;

	MatchLabelsDistanceMap(Context context) {
		this.opService = context.getService(OpService.class);
		this.ioService = context.getService(IOService.class);
		this.uiService = context.getService(UIService.class);
	}

	void run(LabelEditorModel<IntType> model, RandomAccessibleInterval<FloatType> distanceTransform) {
		this.analysis = computeDistance(model, distanceTransform);
	}

	void writeResultToTable(Table table, int xColumn, int yColumn, int zColumn, int distanceColumn, double pixelToMicroMeters) {
		for (Map.Entry<Object, ClosestPoint> entry : analysis.entrySet()) {
			Object label = entry.getKey();
			ClosestPoint relation = entry.getValue();
			int rowIndex = table.getRowIndex(label.toString());
			if(rowIndex < 0) {
				rowIndex = table.getRowCount();
				table.appendRow(label.toString());
			}
			table.set(xColumn, rowIndex, relation.point.getLongPosition(0));
			table.set(yColumn, rowIndex, relation.point.getLongPosition(1));
			table.set(zColumn, rowIndex, relation.point.getLongPosition(2));
			table.set(distanceColumn, rowIndex, Double.toString(pixelToMicroMeters*relation.distance));
		}
	}

	private static Map<Object, ClosestPoint> computeDistance(LabelEditorModel<IntType> model, RandomAccessibleInterval<? extends RealType> distanceTransform) {
		System.out.println("start analysis..");
		LabelRegions<IntType> newregions = new LabelRegions<>(model.labeling());
		Iterator<LabelRegion<IntType>> iterator = newregions.iterator();
		Map<Object, ClosestPoint> analysis = new HashMap<>();
		ExecutorService pool = Executors.newFixedThreadPool(6);
		CompletionService<Object> ecs = new ExecutorCompletionService<>(pool);
		final int size = model.labeling().getMapping().getLabels().size();
		while(iterator.hasNext()) {
			LabelRegion<IntType> labelRegion = iterator.next();
			ecs.submit(Executors.callable(closestPointRunnable(analysis, size, distanceTransform, labelRegion)));
		}
		for (int i = 0; i < size; ++i) {
			try {
				ecs.take().get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		return analysis;
	}

	private static Runnable closestPointRunnable(Map<Object, ClosestPoint> analysis, int size, RandomAccessibleInterval<? extends RealType> distanceTransform, LabelRegion<IntType> labelRegion) {
		return () -> {
			ClosestPoint nearest = getClosestPointMembrane(distanceTransform, labelRegion);
			analysis.put(labelRegion.getLabel(), nearest);
			System.out.println("Distance " + labelRegion.getLabel() + " (total " + size + ") : " + nearest.distance);
		};
	}

	private static ClosestPoint getClosestPointMembrane(RandomAccessibleInterval<? extends RealType> distanceTransform, LabelRegion<IntType> labelRegion) {
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
