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
import de.frauzufall.cellsketch.model.*;
import net.imagej.ops.OpService;
import org.jdom2.DataConversionException;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

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
            if(project.getBoundary() != null) {
                calculateDistanceTransformInner(project.getBoundary());
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
                project.context().service(LogService.class).debug("Not recalculating already existing distance transform map of " + item.getName());
                return;
            }
            project.context().service(StatusService.class).showStatus("Calculating distance transform map of " + item.getName());
            AnalyzeUtils.calculateDistanceTransform(ops, item.distanceMapSource(), item.getDistanceMap(), !skipExistingDistanceMaps);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            item.distanceMapSource().unload();
            item.getDistanceMap().unload();
        }
    }

    private void calculateDistanceTransformInner(HasDistanceMap item) {
        if(!item.distanceMapSource().exists()) return;
        try {
            if(item.getDistanceMap().exists() && this.skipExistingDistanceMaps) {
                project.context().service(LogService.class).debug("Not recalculating already existing distance transform map of " + item.getName());
                return;
            }
            project.context().service(StatusService.class).showStatus("Calculating distance transform map of " + item.getName());
            AnalyzeUtils.calculateDistanceTransformInner(ops, item.distanceMapSource(), item.getDistanceMap(), !skipExistingDistanceMaps);
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
