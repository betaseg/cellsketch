package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.model.FilamentsOverviewTable;
import de.frauzufall.cellsketch.model.FilamentsTable;
import de.frauzufall.cellsketch.model.*;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.jdom2.DataConversionException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.scijava.app.StatusService;
import org.scijava.table.DefaultGenericTable;
import org.scijava.table.Table;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.frauzufall.cellsketch.analysis.AnalyzeUtils.getTableData;
import static de.frauzufall.cellsketch.analysis.LabelMapAnalyzer.getColumnIndex;

public class FilamentsAnalyzer {

    private final CellProject project;
    private final FilamentsItemGroup item;

//    private static float connectedToGolgiThresholdInMicroMeter = 0.02f;
//    private static float connectedToCentriolesThresholdInMicroMeter = 0.2f;

    public FilamentsAnalyzer(CellProject data, FilamentsItemGroup item) {
        this.project = data;
        this.item = item;
    }

    private FileItem getFilamentsYamlItem() {
        return this.item.getFilamentsYamlItem();
    }

    private LabelMapFileItem getFilamentsLabelMap() {
        return this.item.getLabelMap();
    }

    public void analyze(double pixelToMicroMeters) throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
        if(this.item != null && getFilamentsLabelMap().exists()) {
            File outputYAML = new File(project.getProjectDir(), getFilamentsYamlItem().getDefaultFileName());
            File sumTableFile = new File(project.getProjectDir(), this.item.getOverallStats().getDefaultFileName());
            File individualTableFile = new File(project.getProjectDir(), this.item.getIndividualStats().getDefaultFileName());
            List<List<Point>> mts = read(outputYAML);
            writeTables(project, mts, sumTableFile, individualTableFile, pixelToMicroMeters);
        } else {
            project.context().service(StatusService.class).showStatus("Cannot analyze filaments, label map missing.");
        }
    }

    private void writeTables(CellProject data, List<List<Point>> mts, File sumTableFile, File individualTableFile, double pixelToMicroMeters) throws DataConversionException, NMLReader.NMLReaderIOException, IOException {
        Table summaryTable = new DefaultGenericTable();
        this.item.getOverallStats().setTable(summaryTable);
        Table detailsTable = new DefaultGenericTable();
        this.item.getIndividualStats().setTable(detailsTable);
        writeTables(detailsTable, summaryTable, mts, pixelToMicroMeters);
        for (MaskItemGroup item : data.getMaskItems()) {
            calculateDistanceMapRelationship(mts, detailsTable, item, pixelToMicroMeters);
        }
        for (LabelMapItemGroup item : data.getLabelMapItems()) {
            calculateDistanceMapRelationship(mts, detailsTable, item, pixelToMicroMeters);
        }
        if(data.getBoundary() != null) {
            calculateDistanceMapRelationship(mts, detailsTable, data.getBoundary(), pixelToMicroMeters);
        }
        this.item.getOverallStats().setFile(sumTableFile);
        this.item.getOverallStats().save();
        this.item.getIndividualStats().setFile(individualTableFile);
        this.item.getIndividualStats().save();
        this.item.saveConfig();
    }

    private void calculateDistanceMapRelationship(List<List<Point>> mts, Table detailsTable, HasDistanceMap distanceItem, double pixelToMicroMeters) {
        String distanceEnd1ToColumnName = FilamentsTable.getDistanceEnd1ToColumnName(distanceItem.getName());
        int distanceEnd1Column = getColumnIndex(detailsTable, distanceEnd1ToColumnName);
        String distanceEnd2ToColumnName = FilamentsTable.getDistanceEnd2ToColumnName(distanceItem.getName());
        int distanceEnd2Column = getColumnIndex(detailsTable, distanceEnd2ToColumnName);
        String connectedToColumnName = FilamentsTable.getConnectedToColumnName(distanceItem.getName());
        int connectedToColumn = getColumnIndex(detailsTable, connectedToColumnName);
        Double connectedToThreshold = distanceItem.getConnectedToFilamentsEndThresholdInUM();
        ValuePair maxValues;
        if(connectedToThreshold != null) {
            maxValues = calculateDistanceConnected(detailsTable, distanceEnd1Column, distanceEnd2Column, connectedToColumn, mts, distanceItem.getDistanceMap(), connectedToThreshold, pixelToMicroMeters);
            calculateConnectedToPercentages(distanceItem);
            this.item.addLabelIfNotExists(connectedToColumnName, Boolean.class, true);
        } else {
            maxValues = calculateDistance(detailsTable, distanceEnd1Column, distanceEnd2Column, mts, distanceItem.getDistanceMap().getImage(), pixelToMicroMeters);
        }
        LabelTagItem label = this.item.addLabelIfNotExists(distanceEnd1ToColumnName, Double.class, false);
        label.setMaxValue((Double) maxValues.getA());
    }

    private void calculateConnectedToPercentages(HasDistanceMap distanceItem) {
        Table table = this.item.getIndividualStats().getTable();
        int allMTs = table.getRowCount();
        int connected = getTableData(table,
                getColumnIndex(table, FilamentsTable.getConnectedToColumnName(distanceItem.getName())),
                        getColumnIndex(table, FilamentsTable.getConnectedToColumnName(distanceItem.getName())), "true").size();
        Table summaryTable = this.item.getOverallStats().getTable();
        if (summaryTable == null) {
            summaryTable = new DefaultGenericTable();
            this.item.getOverallStats().setTable(summaryTable);
        }
        String percentage = (int) ((float) connected / (float) allMTs * 100) + " %";
        summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getPercentageConnectedToColumnName(distanceItem.getName())), 0, percentage);
        summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getNumberConnectedToColumnName(distanceItem.getName())), 0, connected);
        summaryTable.set(getColumnIndex(summaryTable, LabelMapOverviewTable.getNumberDisconnectedFronColumnName(distanceItem.getName())), 0, allMTs - connected);
        try {
            this.item.getOverallStats().save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTables(Table table, Table summaryTable, List<List<Point>> mts, double pixelToMicroMeters) {
        double[] lengths = new double[mts.size()];
        double[] tortuosities = new double[mts.size()];
        for (int i = 0; i < mts.size(); i++) {
            List<Point> filament = mts.get(i);
            String rowHeader = String.valueOf(i + 1);
            int rowIndex = table.getRowIndex(rowHeader);
            if (rowIndex < 0) {
                rowIndex = table.getRowCount();
                table.appendRow(rowHeader);
            }
            double length = 0;
            List<RealPoint> subsampled_mt = new ArrayList<>();
            Point first = filament.get(0);
            for (int j = 1; j < filament.size(); j++) {
                Point next = filament.get(j);
                double length_line = distance(first, next);
                length += toMicroMeters(length_line, pixelToMicroMeters);
                subsampled_mt.add(new RealPoint(next.getDoublePosition(0), next.getDoublePosition(1), next.getDoublePosition(2)));
                first = next;
            }
            double tortuosity = 0;
            double sum_length = 0;
            Point a = filament.get(0);
            Point b = filament.get(1);
            double length_line = distance(a, b) * pixelToMicroMeters;
//			int start = 0;
            for (int j = 1; j < filament.size() - 1; j++) {
                Point c = filament.get(j + 1);
                length_line += distance(b, c) * pixelToMicroMeters;
                if (length_line > 1 || j == filament.size() - 2) {
//					System.out.println(start + " -> " + j);
                    double linetortuosity = getTortuosity(length_line, a, c, pixelToMicroMeters);
                    tortuosity += linetortuosity * length_line;
                    sum_length += length_line;
                    length_line = distance(b, c) * pixelToMicroMeters;
                    a = b;
//					start = j;
                }
                b = c;
            }
            if (sum_length > 0) {
                tortuosity /= sum_length;
            } else {
                tortuosity = 1;
            }
            lengths[i] = length;
            tortuosities[i] = tortuosity;
            table.set(getColumnIndex(table, FilamentsTable.getLengthColumnName()), rowIndex, String.valueOf(length));
            table.set(getColumnIndex(table, FilamentsTable.getTortuosityColumnName()), rowIndex, String.valueOf(tortuosity));
        }
//		summaryTable.clear();
        summaryTable.appendRow("all");
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getCountColumnName()), 0, String.valueOf(lengths.length));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getTotalLengthColumnName()), 0, String.valueOf(new Sum().evaluate(lengths)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getMeanLengthColumnName()), 0, String.valueOf(new Mean().evaluate(lengths)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getStdevLengthColumnName()), 0, String.valueOf(new StandardDeviation().evaluate(lengths)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getMedianLengthColumnName()), 0, String.valueOf(new Median().evaluate(lengths)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getMeanTortuosityColumnName()), 0, String.valueOf(new Mean().evaluate(tortuosities)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getStdevTortuosityColumnName()), 0, String.valueOf(new StandardDeviation().evaluate(tortuosities)));
        summaryTable.set(getColumnIndex(summaryTable, FilamentsOverviewTable.getMedianTortuosityColumnName()), 0, String.valueOf(new Median().evaluate(tortuosities)));
//        System.out.println(summaryTable);
    }

    public static List<List<Point>> read(File input) throws FileNotFoundException {
        JSONTokener tokener = new JSONTokener(new FileReader(input));
        JSONObject root = new JSONObject(tokener);
        JSONArray filaments = (JSONArray) root.get("lines");
        List<List<Point>> res = new ArrayList<>();
        for (int i = 0; i < filaments.length(); i++) {
            List<Point> mt = new ArrayList<>();
            JSONArray points = (JSONArray) filaments.get(i);
            for (int j = 0; j < points.length(); j++) {
                JSONArray point = (JSONArray) ((JSONArray) filaments.get(i)).get(j);
                mt.add(new Point(point.getLong(0), point.getLong(1), point.getLong(2)));
            }
            res.add(mt);
        }
        return res;
    }

    private static double getTortuosity(double lengthInMicroMeters, Point start, Point end, double pixelToMicroMeters) {
        double distance = distance(start, end);
        double distanceInMicroMeters = toMicroMeters(distance, pixelToMicroMeters);
        return lengthInMicroMeters / distanceInMicroMeters;
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

    private ValuePair calculateDistance(Table table, int distanceFirstColumn, int distanceSecondColumn, List<List<Point>> mts, RandomAccessibleInterval<? extends RealType> distance, double pixelToMicroMeters) {
        Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = computeDistance(mts, distance);
        return writeResultToTable(table, distanceFirstColumn, distanceSecondColumn, analysis, pixelToMicroMeters);
    }

    private ValuePair calculateDistanceConnected(Table table,
                                                 int distance1Column,
                                                 int distance2Column,
                                                 int connectedColumn,
                                                 List<List<Point>> mts,
                                                 ImageFileItem distance,
                                                 double connectedThresholdInUM,
                                                 double pixelToUM) {
        Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = computeDistance(mts, distance.getImage());
        distance.unload();
        return writeResultToTableConnected(table, distance1Column, distance2Column, connectedColumn, analysis, connectedThresholdInUM, pixelToUM);
    }

    private ValuePair writeResultToTable(Table table, int distanceFirstColumn, int distanceSecondColumn, Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis, double pixelToMicroMeters) {
        double maxDistanceP1 = 0;
        double maxDistanceP2 = 0;
        for (Map.Entry<Object, Pair<ClosestPoint, ClosestPoint>> entry : analysis.entrySet()) {
            Object label = entry.getKey();
            ClosestPoint p1 = entry.getValue().getA();
            ClosestPoint p2 = entry.getValue().getB();
            int rowIndex = table.getRowIndex(label.toString());
            if (rowIndex < 0) {
                rowIndex = table.getRowCount();
                table.appendRow(label.toString());
            }
            table.set(distanceFirstColumn, rowIndex, Double.toString(pixelToMicroMeters * p1.distance));
            table.set(distanceSecondColumn, rowIndex, Double.toString(pixelToMicroMeters * p2.distance));
            if(p2.distance > maxDistanceP2) maxDistanceP2 = p2.distance;
            if(p1.distance > maxDistanceP1) maxDistanceP1 = p1.distance;
        }
        return new ValuePair(maxDistanceP1, maxDistanceP2);
    }

    private ValuePair writeResultToTableConnected(Table table, int distance1Column, int distance2Column, int connectedColumn, Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis, double connectedThresholdInMicroMeter, double pixelToMicroMeters) {
        double maxDistanceP1 = 0;
        double maxDistanceP2 = 0;
        for (Map.Entry<Object, Pair<ClosestPoint, ClosestPoint>> entry : analysis.entrySet()) {
            Object label = entry.getKey();
            ClosestPoint p1 = entry.getValue().getA();
            ClosestPoint p2 = entry.getValue().getB();
            int rowIndex = table.getRowIndex(label.toString());
            if (rowIndex < 0) {
                rowIndex = table.getRowCount();
                table.appendRow(label.toString());
            }
            table.set(distance1Column, rowIndex, Double.toString(pixelToMicroMeters * p1.distance));
            table.set(distance2Column, rowIndex, Double.toString(pixelToMicroMeters * p2.distance));
            table.set(connectedColumn, rowIndex, pixelToMicroMeters * p1.distance < connectedThresholdInMicroMeter);
            if(p2.distance > maxDistanceP2) maxDistanceP2 = p2.distance;
            if(p1.distance > maxDistanceP1) maxDistanceP1 = p1.distance;
        }
        return new ValuePair(maxDistanceP1, maxDistanceP2);
    }

    private static Map<Object, Pair<ClosestPoint, ClosestPoint>> computeDistance(List<List<Point>> mts, RandomAccessibleInterval<? extends RealType> distanceTransform) {
        Map<Object, Pair<ClosestPoint, ClosestPoint>> analysis = new HashMap<>();
        RandomAccess<? extends RealType> ra = distanceTransform.randomAccess();
        for (int i = 0; i < mts.size(); i++) {
            List<Point> mt = mts.get(i);
            ClosestPoint close = new ClosestPoint();
            ClosestPoint far = new ClosestPoint();
            Point mt1 = mt.get(0);
            Point mt2 = mt.get(mt.size() - 1);
            double distance1 = ra.setPositionAndGet(mt1).getRealDouble();
            double distance2 = ra.setPositionAndGet(mt2).getRealDouble();
            if (distance1 < distance2) {
                close.point = mt1;
                close.distance = distance1;
                far.point = mt2;
                far.distance = distance2;
            } else {
                far.point = mt1;
                far.distance = distance1;
                close.point = mt2;
                close.distance = distance2;
            }
            analysis.put(i + 1, new ValuePair<>(close, far));
        }
        return analysis;
    }

}
