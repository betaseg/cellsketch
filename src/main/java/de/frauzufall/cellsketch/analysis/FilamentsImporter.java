/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.model.FilamentsItemGroup;
import de.frauzufall.cellsketch.model.FileItem;
import de.frauzufall.cellsketch.model.LabelMapFileItem;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;
import org.jdom2.DataConversionException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilamentsImporter {

    private final CellProject project;
    private final FilamentsItemGroup item;

//    private static float connectedToGolgiThresholdInMicroMeter = 0.02f;
//    private static float connectedToCentriolesThresholdInMicroMeter = 0.2f;

    public FilamentsImporter(CellProject data, FilamentsItemGroup item) {
        this.project = data;
        this.item = item;
    }

    public void processKnossosFilaments(
            File knossosInput,
            double scaleX,
            double scaleY,
            double scaleZ,
            boolean fixZOffset,
            boolean fixLineOrder) throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
        FileItem filamentsYaml = getFilamentsYamlItem();
        fix(project.getSourceItem().getImage(), knossosInput, new File(project.getProjectDir().getAbsolutePath(), filamentsYaml.getDefaultFileName()), scaleX, scaleY, scaleZ, fixZOffset, fixLineOrder);
    }

    private FileItem getFilamentsYamlItem() {
        return this.item.getFilamentsYamlItem();
    }

    public void render(double radius) throws IOException {
        File outputYAML = new File(project.getProjectDir(), getFilamentsYamlItem().getDefaultFileName());
        File outputIndexImg = new File(project.getProjectDir(), getFilamentsLabelMap().getDefaultFileName());
        RandomAccessibleInterval<IntType> rendering = render(outputYAML, radius);
        getFilamentsLabelMap().setFile(outputIndexImg);
        getFilamentsLabelMap().setImage(rendering);
        getFilamentsLabelMap().save();
        getFilamentsLabelMap().unload();
    }

    private LabelMapFileItem getFilamentsLabelMap() {
        return this.item.getLabelMap();
    }


    private static List<List<Pair<Point, Point>>> fix(RandomAccessibleInterval original, File input, File output, double scaleX, double scaleY, double scaleZ, boolean fixZOffset, boolean fixLineOrder) throws DataConversionException, NMLReader.NMLReaderIOException, JSONException, IOException {
        double[] scaleFactors = new double[]{scaleX, scaleY, scaleZ};
        long[] dimensions = NMLReader.getDimensions(input, scaleFactors);
        long offset = 0;
        if(fixZOffset) {
            if (dimensions[0] != original.dimension(0) || dimensions[1] != original.dimension(1)) {
                System.out.println("Original does not match MT dimensions");
            } else {
                if (dimensions[2] != original.dimension(2)) {
                    offset = original.dimension(2) - dimensions[2];
                    System.out.println("Filaments have offset of " + offset);
                }
            }
        }
        List<List<Pair<Point, Point>>> fixedPoints = NMLReader.toPoints(input, scaleFactors);
        System.out.println("MT count from KNOSSOS:" + fixedPoints.size());
        if(fixLineOrder) {
            fixedPoints = FilamentsImporter.correctLineOrder(fixedPoints, 1.5f);
            System.out.println("MT count reordered:" + fixedPoints.size());
        }
        JSONArray lines = new JSONArray();
        for (int i = 0; i < fixedPoints.size(); i++) {
            JSONArray points = new JSONArray();
            int j = 0;
            for (; j < fixedPoints.get(i).size(); j++) {
                JSONArray point = new JSONArray();
                point.put(fixedPoints.get(i).get(j).getA().getIntPosition(0));
                point.put(fixedPoints.get(i).get(j).getA().getIntPosition(1));
                point.put(fixedPoints.get(i).get(j).getA().getIntPosition(2) + offset);
                points.put(j, point);
            }
            JSONArray point = new JSONArray();
            point.put(fixedPoints.get(i).get(j - 1).getB().getIntPosition(0));
            point.put(fixedPoints.get(i).get(j - 1).getB().getIntPosition(1));
            point.put(fixedPoints.get(i).get(j - 1).getB().getIntPosition(2) + offset);
            points.put(j, point);
            lines.put(i, points);
        }
        JSONObject out = new JSONObject();
        JSONArray dimensionsList = new JSONArray();
        dimensionsList.put(0, dimensions[0]);
        dimensionsList.put(1, dimensions[1]);
        dimensionsList.put(2, dimensions[2] + offset);
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

    public static void compare(RandomAccessibleInterval<UnsignedByteType> rendering, RandomAccessibleInterval<UnsignedByteType> rendering2) {
        RandomAccess<UnsignedByteType> ra1 = rendering.randomAccess();
        RandomAccess<UnsignedByteType> ra2 = rendering2.randomAccess();
//        ImageJ ij = new ImageJ();
//        ij.launch();
//		ij.ui().show("img1", rendering);
//		ij.ui().show("img2", rendering2);
        final RandomAccessibleInterval<UnsignedByteType> difference = new DiskCachedCellImgFactory<>(new UnsignedByteType())
                .create(rendering);
        RandomAccess<UnsignedByteType> raDiff = difference.randomAccess();
        for (int i = 0; i < rendering.dimension(0); i++) {
            for (int j = 0; j < rendering.dimension(1); j++) {
                for (int k = 0; k < rendering.dimension(2); k++) {
                    if (ra1.setPositionAndGet(i, j, k).get() != ra2.setPositionAndGet(i, j, k).get()) {
//                        System.out.println(i + " " + j + " " + k);
                        raDiff.setPositionAndGet(i, j, k).set(255);
                    }
                }
            }
        }
//		ij.ui().show("diff", difference);
    }

    public static RandomAccessibleInterval<UnsignedByteType> render(List<List<Pair<Point, Point>>> input, long[] dimensions, int radius) {
        final RandomAccessibleInterval<UnsignedByteType> output = new DiskCachedCellImgFactory<>(new UnsignedByteType())
                .create(dimensions);

        for (List<Pair<Point, Point>> points : input) {
            for (int j = 1; j < points.size(); j++) {
                Pair<Point, Point> point = points.get(j);
                NMLReader.drawLine(output, point.getA(), point.getB(), radius, 255);
            }
        }
        return output;
    }

    public static RandomAccessibleInterval<IntType> render(File input, double radius) throws FileNotFoundException, JSONException {
        JSONTokener tokener = new JSONTokener(new FileReader(input));
        JSONObject root = new JSONObject(tokener);
        JSONArray dimensions = (JSONArray) root.get("dimensions");
        int x = (int) dimensions.get(0);
        int y = (int) dimensions.get(1);
        int z = (int) dimensions.get(2);
        final RandomAccessibleInterval<IntType> output = new DiskCachedCellImgFactory<>(new IntType())
                .create(x, y, z);

        JSONArray filaments = (JSONArray) root.get("lines");

        for (int i = 0; i < filaments.length(); i++) {
            JSONArray points = (JSONArray) filaments.get(i);
            JSONArray first = (JSONArray) points.get(0);
            for (int j = 1; j < points.length(); j++) {
                JSONArray point = (JSONArray) ((JSONArray) filaments.get(i)).get(j);
                drawLine(output, first, point, radius, i + 1);
                first = point;
            }
        }
        return output;
    }

    private static void drawLine(RandomAccessibleInterval<IntType> output, JSONArray first, JSONArray point, double radius, int i) throws JSONException {
        Point point1 = new Point(first.getLong(0), first.getLong(1), first.getLong(2));
        Point point2 = new Point(point.getLong(0), point.getLong(1), point.getLong(2));
        NMLReader.drawLine(output, point1, point2, radius, i);
    }

    static List<List<Pair<Point, Point>>> correctLineOrder(List<List<Pair<Point, Point>>> filaments, float mergeDistance) {
        List<List<Pair<Point, Point>>> res = new ArrayList<>();
        for (List<Pair<Point, Point>> filament : filaments) {
            List<List<Pair<Point, Point>>> lines = new ArrayList<>();
            List<Pair<Point, Point>> currentLine = new ArrayList<>();
            Point lastEnd = null;
            for (Pair<Point, Point> line : filament) {
                if (lastEnd == null || distance(line.getA(), lastEnd) <= mergeDistance) {
                    // the line is either the first line or the beginning of the new line is close to the end of the last line
                    currentLine.add(line);
//					currentLine.add(line.getB());
                } else {
                    // stash current line segments, because the new one doesn't fit on it
                    lines.add(currentLine);
                    // start new current line
                    currentLine = new ArrayList<>();
                    currentLine.add(line);
                }
                lastEnd = line.getB();
            }
            if (lastEnd != null) {
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
        if (changedList) mergeEnds(lines, mergeDistance);
    }

    private static void reverse(List<Pair<Point, Point>> line) {
        ArrayList<Pair<Point, Point>> copy = new ArrayList<>(line);
        line.clear();
        Collections.reverse(copy);
        for (Pair<Point, Point> pair : copy) {
            line.add(new ValuePair<>(pair.getB(), pair.getA()));
        }
    }

    private static double distance(Point a, Point b) {
        return distance(a.getDoublePosition(0), a.getDoublePosition(1), a.getDoublePosition(2),
                b.getDoublePosition(0), b.getDoublePosition(1), b.getDoublePosition(2));
    }

    private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
    }

}
