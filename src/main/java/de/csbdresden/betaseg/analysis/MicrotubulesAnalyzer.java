package de.csbdresden.betaseg.analysis;

import de.csbdresden.betaseg.BetaSegData;
import de.csbdresden.betaseg.MicrotubulesOverviewTable;
import de.csbdresden.betaseg.MicrotubulesTable;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.jdom2.DataConversionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.scijava.Context;
import org.scijava.table.Table;
import sc.fiji.project.DefaultBdvProject;
import sc.fiji.project.SpecificTableBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.csbdresden.betaseg.analysis.AnalyzeUtils.asMask;
import static de.csbdresden.betaseg.analysis.AnalyzeUtils.getTableData;

public class MicrotubulesAnalyzer {

	private final BetaSegData data;

	private static double scale = 0.25;
	private static double pixelToMicroMeters = 0.004*4;
	private static float connectedToGolgiThresholdInMicroMeter = 0.02f;
	private static float connectedToCentriolesThresholdInMicroMeter = 0.2f;

	MicrotubulesAnalyzer(BetaSegData data) {
		this.data = data;
	}

	private void processKnossosMTs(
			String knossosInput,
			String sortedYAML) throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
		File inputFile = new File(data.app().getProjectDir(), knossosInput);
		File outputYAML = new File(data.app().getProjectDir(), sortedYAML);
		fix(data.app().getSourceItem().getImage(), inputFile, outputYAML);
		data.app().save();
	}

	private void render(
			String sortedYAML,
			String indexImage,
			String distanceMapImage) throws IOException {
		File outputYAML = new File(data.app().getProjectDir(), sortedYAML);
		File outputIndexImg = new File(data.app().getProjectDir(), indexImage);
		File outputDistanceMapImg = new File(data.app().getProjectDir(), distanceMapImage);
		render(outputYAML, outputIndexImg, outputDistanceMapImg);
		data.app().save();
	}

	void analyze(
			String sortedYAML,
			String sumTable,
			String individualTable) throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
		File outputYAML = new File(data.app().getProjectDir(), sortedYAML);
		File sumTableFile = new File(data.app().getProjectDir(), sumTable);
		File individualTableFile = new File(data.app().getProjectDir(), individualTable);
		List<List<Point>> mts = read(outputYAML);
		writeTables(data, mts, sumTableFile, individualTableFile);
		data.app().save();
	}

	private void writeTables(BetaSegData data, List<List<Point>> mts, File sumTableFile, File individualTableFile) throws DataConversionException, NMLReader.NMLReaderIOException, IOException {
		Table summaryTable = SpecificTableBuilder.build(new MicrotubulesOverviewTable());
		data.getMicrotubulesStatsItem().setTable(summaryTable);
		Table detailsTable = SpecificTableBuilder.build(new MicrotubulesTable());
		data.getMicrotubulesIndividualStatsItem().setTable(detailsTable);
		writeTables(detailsTable, summaryTable, mts);
		int centriolesColumn1 = MicrotubulesTable.getDistanceEnd1CentriolesColumn();
		int centriolesColumn2 = MicrotubulesTable.getDistanceEnd2CentriolesColumn();
		int centriolesConnectedColumn = MicrotubulesTable.getConnectedToCentriolesColumn();
		float connectedToCentriolesThreshold = connectedToCentriolesThresholdInMicroMeter;
		calculateDistanceConnected(detailsTable, centriolesColumn1, centriolesColumn2, centriolesConnectedColumn, mts, data.getCentriolesDistanceTransformItem().getImage(), connectedToCentriolesThreshold);
		int membraneColumn1 = MicrotubulesTable.getDistanceEnd1MembraneColumn();
		int membraneColumn2 = MicrotubulesTable.getDistanceEnd2MembraneColumn();
		calculateDistance(detailsTable, membraneColumn1, membraneColumn2, mts, data.getMembraneDistanceMapItem().getImage());
		if(data.getGolgiDistanceTransformItem().exists()) {
			int golgiColumn1 = MicrotubulesTable.getDistanceEnd1GolgiColumn();
			int golgiColumn2 = MicrotubulesTable.getDistanceEnd2GolgiColumn();
			int golgiConnectedColumn = MicrotubulesTable.getConnectedToGolgiColumn();
			float connectedToGolgiThreshold = connectedToGolgiThresholdInMicroMeter;
			calculateDistanceConnected(detailsTable, golgiColumn1, golgiColumn2, golgiConnectedColumn, mts, data.getGolgiDistanceTransformItem().getImage(), connectedToGolgiThreshold);
			calculateConnectedToCentriolesPercentages();
			calculateConnectedToGolgiPercentages();
		}
		data.getMicrotubulesStatsItem().setFile(sumTableFile);
		data.getMicrotubulesStatsItem().save();
		data.getMicrotubulesIndividualStatsItem().setFile(individualTableFile);
		data.getMicrotubulesIndividualStatsItem().save();
	}

	private void calculateConnectedToGolgiPercentages() {
		int allMTs = data.getMicrotubulesIndividualStatsItem().getTable().getRowCount();
		int connected = getTableData(data.getMicrotubulesIndividualStatsItem().getTable(), MicrotubulesTable.getConnectedToGolgiColumn(), MicrotubulesTable.getConnectedToGolgiColumn(), "true").size();
		Table summaryTable = data.getMicrotubulesStatsItem().getTable();
		if(summaryTable == null) {
			summaryTable = SpecificTableBuilder.build(new MicrotubulesOverviewTable());
			data.getMicrotubulesStatsItem().setTable(summaryTable);
		}
		String percentage = (int)((float)connected/(float)allMTs*100) + " %";
		summaryTable.set(MicrotubulesOverviewTable.getPercentageConnectedToGolgiColumn(), 0, percentage);
		summaryTable.set(MicrotubulesOverviewTable.getNumberConnectedToGolgiColumn(), 0, connected);
		summaryTable.set(MicrotubulesOverviewTable.getNumberDisconnectedFromGolgiColumn(), 0, allMTs-connected);
		try {
			data.getMicrotubulesStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void calculateConnectedToCentriolesPercentages() {
		int allMTs = data.getMicrotubulesIndividualStatsItem().getTable().getRowCount();
		int connected = getTableData(data.getMicrotubulesIndividualStatsItem().getTable(), MicrotubulesTable.getConnectedToCentriolesColumn(), MicrotubulesTable.getConnectedToCentriolesColumn(), "true").size();
		Table summaryTable = data.getMicrotubulesStatsItem().getTable();
		if(summaryTable == null) {
			summaryTable = SpecificTableBuilder.build(new MicrotubulesOverviewTable());
			data.getMicrotubulesStatsItem().setTable(summaryTable);
		}
		String percentage = (int)((float)connected/(float)allMTs*100) + " %";
		summaryTable.set(MicrotubulesOverviewTable.getPercentageConnectedToCentriolesColumn(), 0, percentage);
		summaryTable.set(MicrotubulesOverviewTable.getNumberConnectedToCentriolesColumn(), 0, connected);
		summaryTable.set(MicrotubulesOverviewTable.getNumberDisconnectedFromCentriolesColumn(), 0, allMTs-connected);
		try {
			data.getMicrotubulesStatsItem().save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeTables(Table table, Table summaryTable, List<List<Point>> mts) throws DataConversionException, NMLReader.NMLReaderIOException {
		double[] lengths = new double[mts.size()];
		double[] tortuosities = new double[mts.size()];
		for (int i = 0; i < mts.size(); i++) {
			List<Point> microtubule = mts.get(i);
			String rowHeader = String.valueOf(i+1);
			int rowIndex = table.getRowIndex(rowHeader);
			if(rowIndex < 0) {
				rowIndex = table.getRowCount();
				table.appendRow(rowHeader);
			}
			double length = 0;
			Point first = microtubule.get(0);
			for (int j = 1; j < microtubule.size(); j++) {
				Point next = microtubule.get(j);
				length += toMicroMeters(distance(first, next), pixelToMicroMeters);
				first = next;
			}
			lengths[i] = length;
			double tortuosity = getTortuosity(length, microtubule.get(0), microtubule.get(microtubule.size() - 1), pixelToMicroMeters);
			tortuosities[i] = tortuosity;
			table.set(MicrotubulesTable.getLengthColumn(), rowIndex, String.valueOf(length));
			table.set(MicrotubulesTable.getTortuosityColumn(), rowIndex, String.valueOf(tortuosity));
		}
//		summaryTable.clear();
		summaryTable.appendRow("all");
		summaryTable.set(MicrotubulesOverviewTable.getCountColumn(), 0, String.valueOf(lengths.length));
		summaryTable.set(MicrotubulesOverviewTable.getTotalLengthColumn(), 0, String.valueOf(new Sum().evaluate(lengths)));
		summaryTable.set(MicrotubulesOverviewTable.getMeanLengthColumn(), 0, String.valueOf(new Mean().evaluate(lengths)));
		summaryTable.set(MicrotubulesOverviewTable.getStdevLengthColumn(), 0, String.valueOf(new StandardDeviation().evaluate(lengths)));
		summaryTable.set(MicrotubulesOverviewTable.getMedianLengthColumn(), 0, String.valueOf(new Median().evaluate(lengths)));
		summaryTable.set(MicrotubulesOverviewTable.getMeanTortuosityColumn(), 0, String.valueOf(new Mean().evaluate(tortuosities)));
		summaryTable.set(MicrotubulesOverviewTable.getStdevTortuosityColumn(), 0, String.valueOf(new StandardDeviation().evaluate(tortuosities)));
		summaryTable.set(MicrotubulesOverviewTable.getMedianTortuosityColumn(), 0, String.valueOf(new Median().evaluate(tortuosities)));
		System.out.println(summaryTable);
	}

	private void render(File outputYAML, File outputIndexImg, File outputDistanceMapImg) throws IOException {
		RandomAccessibleInterval<IntType> rendering = render(outputYAML, 1);
		data.getMicrotubulesLabelMapItem().setFile(outputIndexImg);
		data.getMicrotubulesLabelMapItem().setImage(rendering);
		data.getMicrotubulesLabelMapItem().save();
		data.getMicrotubulesDistanceMapItem().setFile(outputDistanceMapImg);
		AnalyzeUtils.calculateDistanceTransform(data.app().context().service(OpService.class),
				asMask(rendering), data.getMicrotubulesDistanceMapItem());
	}

	private static List<List<Pair<Point, Point>>> fix(RandomAccessibleInterval original, File input, File output) throws DataConversionException, NMLReader.NMLReaderIOException, JSONException, IOException {
		long[] dimensions = NMLReader.getDimensions(input, scale);
		long offset = 0;
		if(dimensions[0] != original.dimension(0) || dimensions[1] != original.dimension(1)) {
			System.out.println("Original does not match MT dimensions");
		} else {
			if(dimensions[2] != original.dimension(2)) {
				offset = original.dimension(2) - dimensions[2];
				System.out.println("Microtubules have offset of " + offset);
			}
		}
		List<List<Pair<Point, Point>>> fixedPointsCompare = NMLReader.toPoints(input, scale);
		List<List<Pair<Point, Point>>> fixedPoints = NMLReader.toPoints(input, scale);
		fixedPoints = correctLineOrder(fixedPoints, 0);
		for (List<Pair<Point, Point>> pairs : fixedPointsCompare) {
			for (Pair<Point, Point> pair : pairs) {
				boolean found = find(pair, fixedPoints);
				if(!found) {
					System.out.println("Bäh");
				}
			}
		}
		JSONArray lines = new JSONArray();
		for (int i = 0; i < fixedPoints.size(); i++) {
			JSONArray points = new JSONArray();
			int j = 0;
			for (; j < fixedPoints.get(i).size(); j++) {
				JSONArray point = new JSONArray();
				point.put(fixedPoints.get(i).get(j).getA().getIntPosition(0));
				point.put(fixedPoints.get(i).get(j).getA().getIntPosition(1));
				point.put(fixedPoints.get(i).get(j).getA().getIntPosition(2)+offset);
				points.put(j, point);
			}
			JSONArray point = new JSONArray();
			point.put(fixedPoints.get(i).get(j-1).getB().getIntPosition(0));
			point.put(fixedPoints.get(i).get(j-1).getB().getIntPosition(1));
			point.put(fixedPoints.get(i).get(j-1).getB().getIntPosition(2)+offset);
			points.put(j, point);
			lines.put(i, points);
		}
		JSONObject out = new JSONObject();
		JSONArray dimensionsList = new JSONArray();
		dimensionsList.put(0, dimensions[0]);
		dimensionsList.put(1, dimensions[1]);
		dimensionsList.put(2, dimensions[2]+offset);
		out.put("dimensions", dimensionsList);
		out.put("lines", lines);
		try (FileWriter file = new FileWriter(output)) {
			out.write(file);
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fixedPoints;
	}

	private static boolean find(Pair<Point, Point> pair, List<List<Pair<Point, Point>>> fixedPoints) {
		for (List<Pair<Point, Point>> fixedPoint : fixedPoints) {
			for (Pair<Point, Point> pointPointPair : fixedPoint) {
				if(pointsEqual(pointPointPair.getA(), pair.getA()) && pointsEqual(pointPointPair.getB(), pair.getB() )) {
					return true;
				}
				if(pointsEqual(pointPointPair.getB(), pair.getA()) && pointsEqual(pointPointPair.getA(), pair.getB() )) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean pointsEqual(Point a, Point b) {
		if(a.getIntPosition(0) != b.getIntPosition(0)) return false;
		if(a.getIntPosition(1) != b.getIntPosition(1)) return false;
		return a.getIntPosition(2) == b.getIntPosition(2);
	}

	public static List<List<Point>> read(File input) throws FileNotFoundException {
		JSONTokener tokener = new JSONTokener(new FileReader(input));
		JSONObject root = new JSONObject(tokener);
		JSONArray microtubules = (JSONArray) root.get("lines");
		List<List<Point>> res = new ArrayList<>();
		for (int i = 0; i < microtubules.length(); i++) {
			List<Point> mt = new ArrayList<>();
			JSONArray points = (JSONArray) microtubules.get(i);
			for (int j = 0; j < points.length(); j++) {
				JSONArray point = (JSONArray) ((JSONArray) microtubules.get(i)).get(j);
				mt.add(new Point(point.getLong(0), point.getLong(1), point.getLong(2)));
			}
			res.add(mt);
		}
		return res;
	}

	public static RandomAccessibleInterval<UnsignedByteType> render(List<List<Pair<Point, Point>>> input, long[] dimensions, int radius) {
		final RandomAccessibleInterval<UnsignedByteType> output = new DiskCachedCellImgFactory<>( new UnsignedByteType() )
				.create(dimensions);

		for (List<Pair<Point, Point>> points : input) {
			for (int j = 1; j < points.size(); j++) {
				Pair<Point, Point> point = points.get(j);
				NMLReader.drawLine(output, point.getA(), point.getB(), radius, 255);
			}
		}
		return output;
	}

	public static RandomAccessibleInterval<IntType> render(File input, int radius) throws FileNotFoundException, JSONException {
		JSONTokener tokener = new JSONTokener(new FileReader(input));
		JSONObject root = new JSONObject(tokener);
		JSONArray dimensions = (JSONArray) root.get("dimensions");
		int x = (int) dimensions.get(0);
		int y = (int) dimensions.get(1);
		int z = (int) dimensions.get(2);
		final RandomAccessibleInterval<IntType> output = new DiskCachedCellImgFactory<>( new IntType() )
				.create(x, y, z);

		JSONArray microtubules = (JSONArray) root.get("lines");

		for (int i = 0; i < microtubules.length(); i++) {
			JSONArray points = (JSONArray) microtubules.get(i);
			JSONArray first = (JSONArray) points.get(0);
			for (int j = 1; j < points.length(); j++) {
				JSONArray point = (JSONArray) ((JSONArray) microtubules.get(i)).get(j);
				drawLine(output, first, point, radius, i+1);
				first = point;
			}
		}
		return output;
	}

	private static void drawLine(RandomAccessibleInterval<IntType> output, JSONArray first, JSONArray point, int radius, int i) throws JSONException {
		Point point1 = new Point(first.getLong(0), first.getLong(1), first.getLong(2));
		Point point2 = new Point(point.getLong(0), point.getLong(1), point.getLong(2));
		NMLReader.drawLine(output, point1, point2, radius, i);
	}

	public static void compare(RandomAccessibleInterval<UnsignedByteType> rendering, RandomAccessibleInterval<UnsignedByteType> rendering2) {
		RandomAccess<UnsignedByteType> ra1 = rendering.randomAccess();
		RandomAccess<UnsignedByteType> ra2 = rendering2.randomAccess();
		ImageJ ij = new ImageJ();
		ij.launch();
//		ij.ui().show("img1", rendering);
//		ij.ui().show("img2", rendering2);
		final RandomAccessibleInterval<UnsignedByteType> difference = new DiskCachedCellImgFactory<>( new UnsignedByteType() )
				.create(rendering);
		RandomAccess<UnsignedByteType> raDiff = difference.randomAccess();
		for (int i = 0; i < rendering.dimension(0); i++) {
			for (int j = 0; j < rendering.dimension(1); j++) {
				for (int k = 0; k < rendering.dimension(2); k++) {
					if(ra1.setPositionAndGet(i, j, k).get() != ra2.setPositionAndGet(i, j, k).get()) {
						System.out.println(i + " " + j + " " + k);
						raDiff.setPositionAndGet(i, j, k).set(255);
					}
				}
			}
		}
//		ij.ui().show("diff", difference);
	}

	static List<List<Pair<Point, Point>>> correctLineOrder(List<List<Pair<Point, Point>>> microtubules, float mergeDistance) {
		List<List<Pair<Point, Point>>> res = new ArrayList<>();
		for (List<Pair<Point, Point>> microtubule : microtubules) {
			List<List<Pair<Point, Point>>> lines = new ArrayList<>();
			List<Pair<Point, Point>> currentLine = new ArrayList<>();
			Point lastEnd = null;
			for (Pair<Point, Point> line : microtubule) {
				if(lastEnd == null || distance(line.getA(), lastEnd) <= mergeDistance) {
					currentLine.add(line);
//					currentLine.add(line.getB());
				} else {
					lines.add(currentLine);
					currentLine = new ArrayList<>();
					currentLine.add(line);
				}
				lastEnd = line.getB();
			}
			if(lastEnd != null) {
				lines.add(currentLine);
			}
			mergeEnds(lines, mergeDistance);
			res.addAll(lines);
		}
		return res;
	}

	private static void mergeEnds(List<List<Pair<Point, Point>>> lines, float mergeDistance) {
		boolean changedList = false;
//		List<List<Pair<Point, Point>>> linesCopy = new ArrayList<>(lines);
//		List<List<Pair<Point, Point>>> toRemove = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			List<Pair<Point, Point>> lineA = lines.get(i);
			for (int j = 0; j < lines.size(); j++) {
				List<Pair<Point, Point>> lineB = lines.get(j);
				if (lineA == lineB) continue;
				Point startA = lineA.get(0).getA();
				Point endA = lineA.get(lineA.size() - 1).getB();
				Point startB = lineB.get(0).getA();
				Point endB = lineB.get(lineB.size() - 1).getB();
				if (distance(endA, startB) <= mergeDistance) {
					lineA.addAll(lineB);
					lines.remove(lineB);
					changedList = true;
					break;
				}
				if (distance(endB, startA) <= mergeDistance) {
					lineB.addAll(lineA);
					lines.remove(lineA);
					changedList = true;
					break;
				}
				if (distance(startA, startB) <= mergeDistance) {
					reverse(lineA);
					lineA.addAll(lineB);
					lines.remove(lineB);
					changedList = true;
					break;
				}
				if (distance(endA, endB) <= mergeDistance) {
					reverse(lineB);
					lineA.addAll(lineB);
					lines.remove(lineB);
					changedList = true;
					break;
				}
			}
			if (changedList) break;
		}
		if(changedList) mergeEnds(lines, mergeDistance);
	}

	private static void reverse(List<Pair<Point, Point>> line) {
		ArrayList<Pair<Point, Point>> copy = new ArrayList<>(line);
		line.clear();
		Collections.reverse(copy);
		for (Pair<Point, Point> pair : copy) {
			line.add(new ValuePair<>(pair.getB(), pair.getA()));
		}
	}

	private static double getTortuosity(double lengthInMicroMeters, Point start, Point end, double pixelToMicroMeters) {
		double distance = distance(start, end);
		double distanceInMicroMeters = toMicroMeters(distance, pixelToMicroMeters);
		return 1 - (distanceInMicroMeters / lengthInMicroMeters);
	}

	private static double toMicroMeters(double value, double scale) {
		return value * scale;
	}

	private static double distance(Point a, Point b) {
		return distance(a.getDoublePosition(0), a.getDoublePosition(1), a.getDoublePosition(2),
				b.getDoublePosition(0), b.getDoublePosition(1), b.getDoublePosition(2));
	}

	private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
	}

	private void calculateDistance(Table table, int distanceFirstColumn, int distanceSecondColumn, List<List<Point>> mts, RandomAccessibleInterval<? extends RealType> distance) {
		Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = computeDistance(mts, distance);
		writeResultToTable(table, distanceFirstColumn, distanceSecondColumn, analysis);
	}

	private void calculateDistanceConnected(Table table,
	                                        int distance1Column,
	                                        int distance2Column,
	                                        int connectedColumn,
	                                        List<List<Point>> mts,
	                                        RandomAccessibleInterval<? extends RealType> distance,
	                                        float connectedThresholdInMicroMeter) {
		Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = computeDistance(mts, distance);
		writeResultToTableConnected(table, distance1Column, distance2Column, connectedColumn, analysis, connectedThresholdInMicroMeter);
	}

	private void writeResultToTable(Table table, int distanceFirstColumn, int distanceSecondColumn, Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis) {
		for (Map.Entry<Object, Pair<ClosestPoint, ClosestPoint>> entry : analysis.entrySet()) {
			Object label = entry.getKey();
			ClosestPoint p1 = entry.getValue().getA();
			ClosestPoint p2 = entry.getValue().getB();
			int rowIndex = table.getRowIndex(label.toString());
			if(rowIndex < 0) {
				rowIndex = table.getRowCount();
				table.appendRow(label.toString());
			}
			table.set(distanceFirstColumn, rowIndex, Double.toString(pixelToMicroMeters*p1.distance));
			table.set(distanceSecondColumn, rowIndex, Double.toString(pixelToMicroMeters*p2.distance));
		}
	}

	private void writeResultToTableConnected(Table table, int distance1Column, int distance2Column, int connectedColumn, Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis, float connectedThresholdInMicroMeter) {
		for (Map.Entry<Object, Pair<ClosestPoint, ClosestPoint>> entry : analysis.entrySet()) {
			Object label = entry.getKey();
			ClosestPoint p1 = entry.getValue().getA();
			ClosestPoint p2 = entry.getValue().getB();
			int rowIndex = table.getRowIndex(label.toString());
			if(rowIndex < 0) {
				rowIndex = table.getRowCount();
				table.appendRow(label.toString());
			}
			table.set(distance1Column, rowIndex, Double.toString(pixelToMicroMeters*p1.distance));
			table.set(distance2Column, rowIndex, Double.toString(pixelToMicroMeters*p2.distance));
			table.set(connectedColumn, rowIndex, pixelToMicroMeters*p1.distance < connectedThresholdInMicroMeter);
		}
	}

	private static Map<Object, Pair<ClosestPoint, ClosestPoint>> computeDistance(List<List<Point>> mts, RandomAccessibleInterval<? extends RealType> distanceTransform) {
		System.out.println("start analysis..");
		Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = new HashMap<>();
		RandomAccess<? extends RealType> ra = distanceTransform.randomAccess();
		for (int i = 0; i < mts.size(); i++) {
			List<Point> mt = mts.get(i);
			ClosestPoint close = new ClosestPoint();
			ClosestPoint far = new ClosestPoint();
			double aDistance = ra.setPositionAndGet(mt.get(0)).getRealDouble();
			double bDistance = ra.setPositionAndGet(mt.get(mt.size()-1)).getRealDouble();
			if(aDistance < bDistance) {
				close.point = mt.get(0);
				close.distance = aDistance;
				far.point = mt.get(mt.size()-1);
				far.distance = bDistance;
			} else {
				far.point = mt.get(0);
				far.distance = aDistance;
				close.point = mt.get(mt.size()-1);
				close.distance = bDistance;
			}
			analysis.put(i+1, new ValuePair<>(close, far));
		}
		return analysis;
	}


	private static void run(String knossosSource, String cell) throws IOException, DataConversionException, NMLReader.NMLReaderIOException {
		String projectPath = "/home/random/Development/imagej/project/3DAnalysisFIBSegmentation/owncloud/data_for_paper/" + cell;
		String sortedYAML = cell + "_microtubules_sorted.yaml";
		String indexImage = cell + "_microtubules.tif";
		String distanceMapImage = cell + "_microtubules_distance_map.tif";
		String sumTable = cell + "_microtubules.csv";
		String individualTable = cell + "_microtubules_individual.csv";
		Context context = new Context();
		DefaultBdvProject app = new DefaultBdvProject(new File(projectPath), context);
		BetaSegData data = new BetaSegData(app);
		app.loadProject();
		MicrotubulesAnalyzer analyzer = new MicrotubulesAnalyzer(data);
//		analyzer.processKnossosMTs(knossosSource, sortedYAML);
//		analyzer.render(sortedYAML, indexImage, distanceMapImage);
		analyzer.analyze(sortedYAML, sumTable, individualTable);
		app.dispose();
		context.dispose();
		System.out.println("Done.");
	}

	public static void main(String...args) throws DataConversionException, JSONException, NMLReader.NMLReaderIOException, IOException {
		run("HC_1_MTs_minus_MTOC.xml", "high_c1");
//		run("HC_2_MTs_minus_MTOC.xml", "high_c2");
//		run("HC_3_MTs_minus_MTOC.xml", "high_c3");
//		run("HC_4_MTs_minus_MTOC.xml", "high_c4");
//		run("LC_1_MTs_minus_MTOC.xml", "low_c1");
//		run("LC_2_MTs_minus_MTOC.xml", "low_c2");
//		run("LC_3_MTs_minus_MTOC.xml", "low_c3");
	}

}
