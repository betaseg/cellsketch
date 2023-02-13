package de.frauzufall.cellsketch.analysis;

import de.frauzufall.cellsketch.CellProject;
import de.frauzufall.cellsketch.model.*;
import net.imagej.ops.OpService;
import org.jdom2.DataConversionException;

import java.io.IOException;

public class CellAnalyzer {
    private final CellProject project;
    private final OpService ops;
    private final boolean skipExistingDistanceMaps;
    private final double connectedThresholdInUM;

    public CellAnalyzer(CellProject project, boolean skipExistingDistanceMaps, OpService ops, double connectedThresholdInUM){
        this.project = project;
        this.skipExistingDistanceMaps = skipExistingDistanceMaps;
        this.ops = ops;
        this.connectedThresholdInUM = connectedThresholdInUM;
    }
    public void analyze() {
        String progressName = "Running analysis...";
        project.startProgress(progressName);
        try {
            for (Item item : project.getItems()) {
                item.unload();
            }
            project.updateUI();
            for (LabelMapItemGroup mapItemGroup : project.getLabelMapItems()) {
                calculateDistanceTransform(mapItemGroup);
            }
            for (MaskItemGroup maskItemGroup : project.getMaskItems()) {
                calculateDistanceTransform(maskItemGroup);
            }
            for (FilamentsItemGroup filamentsItemGroup : project.getFilamentsItems()) {
                analyzeFilaments(project.getPixelToUM(), filamentsItemGroup);
            }
            for (LabelMapItemGroup labelMapItemGroup : project.getLabelMapItems()) {
                analyzeLabelMaps(labelMapItemGroup);
            }
            project.populateModel();
            project.updateUI();
        } catch (IOException | NMLReader.NMLReaderIOException | DataConversionException e) {
            e.printStackTrace();
        } finally {
            project.endProgress(progressName);
        }
    }

    private void calculateDistanceTransform(HasDistanceMap item) {
        if(!item.distanceMapSource().exists()) return;
        try {
            if(item.getDistanceMap().exists() && this.skipExistingDistanceMaps) {
                System.out.println("Not recalculating already existing distance transform map of " + item.getName());
                return;
            }
            System.out.println("Calculating distance transform map of " + item.getName());
            AnalyzeUtils.calculateDistanceTransform(ops, item.distanceMapSource(), item.getDistanceMap(), !skipExistingDistanceMaps);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            item.distanceMapSource().unload();
            item.getDistanceMap().unload();
        }
    }

    private void analyzeFilaments(double pixelToMicroMeters, FilamentsItemGroup filamentsItemGroup) throws DataConversionException, IOException, NMLReader.NMLReaderIOException {
        calculateDistanceTransform(filamentsItemGroup);
        FilamentsAnalyzer analyzer = new FilamentsAnalyzer(project, filamentsItemGroup);
        analyzer.analyze(pixelToMicroMeters);
    }

    private void analyzeLabelMaps(LabelMapItemGroup labelMap) {
        LabelMapAnalyzer analyzer = new LabelMapAnalyzer(project, labelMap, (float)connectedThresholdInUM);
        try {
            analyzer.analyze();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
