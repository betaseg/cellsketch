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
package de.frauzufall.cellsketch.model;

import bdv.util.BdvSource;
import de.frauzufall.cellsketch.ui.PlotUtil;
import net.imagej.DatasetService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import org.scijava.app.StatusService;
import org.scijava.io.IOService;
import sc.fiji.labeleditor.core.model.colors.LabelEditorColor;
import sc.fiji.labeleditor.core.model.colors.LabelEditorValueColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static de.frauzufall.cellsketch.analysis.LabelMapAnalyzer.getColumnIndex;

public class LabelTagItem extends AbstractItem implements DisplayableInBdv {

	private final LabelMapFileItem referenceLabelMap;
	private final TableFileItem referenceTable;
	private Class tagClass;
	private String referenceColumnName;
	private boolean visible = false;
	private List<BdvSource> sources = new ArrayList<>();

	private boolean colorMaxValue = true;
	private double maxValue;
	private double minValue;
	private String tag;

	LabelTagItem(String name, LabelMapFileItem referenceLabelMap, TableFileItem referenceTable, String referenceColumnName, Class tagClass) {
		setName(name);
		this.referenceLabelMap = referenceLabelMap;
		this.referenceTable = referenceTable;
		this.referenceColumnName = referenceColumnName;
		this.tagClass = tagClass;
		if(!tagClass.isAssignableFrom(Boolean.class)) {
			getActions().add(new DefaultAction(
					"Plot property",
					Collections.singletonList(this),
					this::plot
			));
		} else {
			getActions().add(new DefaultAction(
					"Export mask",
					Collections.singletonList(this),
					this::exportMask
			));
			getActions().add(new DefaultAction(
					"Export inverted mask",
					Collections.singletonList(this),
					this::exportInvertedMask
			));
		}
	}

	public Class getTagClass() {
		return tagClass;
	}

	public void setTagClass(Class tagClass) {
		this.tagClass = tagClass;
	}

	public TableFileItem getReferenceTable() {
		return referenceTable;
	}

	public String getReferenceColumnName() {
		return referenceColumnName;
	}

	public boolean isColorMaxValue() {
		return colorMaxValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getMinValue() {
		return minValue;
	}

	public void exportMask() {
		try {
			File exportMask = new File(getExportDir(), referenceLabelMap.project().getName() + "_" + getExportName() + ".tif");
			IOService io = referenceLabelMap.project().context().service(IOService.class);
			DatasetService datasetService = referenceLabelMap.project().context().service(DatasetService.class);
			RandomAccessibleInterval tagMask = hasTag();
			exportMask.getParentFile().mkdirs();
			io.save(datasetService.create(tagMask), exportMask.getAbsolutePath());
			io.context().service(StatusService.class).showStatus("Saved mask to " + exportMask.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exportInvertedMask() {
		try {
			File exportMask = new File(getExportDir(), referenceLabelMap.project().getName() + "_" + getInvertedExportName() + ".tif");
			IOService io = referenceLabelMap.project().context().service(IOService.class);
			DatasetService datasetService = referenceLabelMap.project().context().service(DatasetService.class);
			RandomAccessibleInterval tagMask = hasNotTag();
			exportMask.getParentFile().mkdirs();
			io.save(datasetService.create(tagMask), exportMask.getAbsolutePath());
			io.context().service(StatusService.class).showStatus("Saved inverted mask to " + exportMask.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void plot() {
		if(!referenceTable.exists()) {
			System.out.println("Cannot plot table " + referenceTable.getName() + " because it doesn't exist.");
			return;
		}
		int referenceColumn = getColumnIndex(referenceTable.getTable(), referenceColumnName);
		tag = getName();
		String series = referenceLabelMap.getName();
		String title = series + ": " + tag;
		String xLabel = tag;
		String yLabel = "";
		int bins = 20;
		double[] data = PlotUtil.toDoubleArray(referenceTable.getTable().get(referenceColumn));
		PlotUtil.displayHistogram(data, series, title, xLabel, yLabel, bins);
	}

	private String getExportName() {
		return referenceLabelMap.nameToFileName() + "_" + nameToFileName();
	}

	private String getInvertedExportName() {
		return referenceLabelMap.nameToFileName() + "_not_" + nameToFileName();
	}

	private File getExportDir() {
		return new File(referenceLabelMap.project().getProjectDir(), "export");
	}

	private RandomAccessibleInterval<ByteType> hasTag() {
		List labels = referenceLabelMap.getModel().tagging().getLabels(tag);
		Set allLabels = referenceLabelMap.getModel().labeling().getMapping().getLabels();
		Map<Object, Boolean> hasTag = new HashMap<>();
		for (Object label : allLabels) {
			hasTag.put(label, labels.contains(label));
		}
		Converter<LabelingType, ByteType> converter = (input, output) -> {
			if(input.size() > 0 && hasTag.get(input.iterator().next())) output.setOne();
			else output.setZero();
		};
		return Converters.convert((RandomAccessibleInterval<LabelingType>)referenceLabelMap.getModel().labeling(), converter, new ByteType());
	}

	private RandomAccessibleInterval<ByteType> hasNotTag() {
		List labels = referenceLabelMap.getModel().tagging().getLabels(tag);
		Set allLabels = referenceLabelMap.getModel().labeling().getMapping().getLabels();
		Map<Object, Boolean> hasTag = new HashMap<>();
		for (Object label : allLabels) {
			hasTag.put(label, labels.contains(label));
		}
		Converter<LabelingType<?>, ByteType> converter = (input, output) -> {
			if(input.size() > 0 && !hasTag.get(input.iterator().next())) output.setOne();
			else output.setZero();
		};
		return Converters.convert((RandomAccessibleInterval<LabelingType<?>>)referenceLabelMap.getModel().labeling(), converter, new ByteType());
	}

	@Override
	public void addToBdv() {
//		InteractiveTableDisplayViewer viewer = new InteractiveTableDisplayViewer(new BdvAppTable(referenceLabelMap.getModel(), referenceTable.getTable()));
//		viewer.display();
		if(!referenceTable.exists()) return;
		if(!referenceLabelMap.isVisible()) {
			referenceLabelMap.addToBdv();
		}
		setVisible(true);
		updateBdvColor();
	}

	void addTag() {
		if(referenceTable == null || referenceTable.getTable() == null) return;
//		referenceLabelMap.getModel().colors().pauseListeners();
//		referenceLabelMap.getModel().tagging().pauseListeners();
		int column = getColumnIndex(referenceTable.getTable(), referenceColumnName);
		tag = getName();
		for (int i = 0; i < referenceTable.getTable().getRowCount(); i++) {
			IntType label = new IntType(Integer.valueOf(referenceTable.getTable().getRowHeader(i)));
			Object rowValueObj = referenceTable.getTable().get(column, i);
			if(rowValueObj == null) continue;
			String rowValue = rowValueObj.toString();
			if(rowValue.isEmpty()) continue;
			if(tagClass.isAssignableFrom(Double.class)) {
				DoubleType value = new DoubleType(Double.parseDouble(rowValue));
				referenceLabelMap.getModel().tagging().addValueToLabel(tag, value, label);
			}
			if(tagClass.isAssignableFrom(Boolean.class)) {
				boolean value = Boolean.parseBoolean(rowValue);
				if(value) {
					referenceLabelMap.getModel().tagging().addTagToLabel(tag, label);
				}
			}
		}
		if(tagClass.isAssignableFrom(Double.class)) {
			LabelEditorValueColor<DoubleType> color = referenceLabelMap.getModel().colors().makeValueFaceColor(tag, new DoubleType(minValue), new DoubleType(maxValue));
//			if(colorMaxValue) {
//				color.setMaxColor(getColor());
//				color.setMinColor(0,0,0,0);
//			} else {
//				color.setMinColor(getColor());
//				color.setMaxColor(0,0,0,0);
//			}
//		} else {
//			referenceLabelMap.getModel().colors().getFaceColor(tag).set(getColor());
		}
//		referenceLabelMap.getModel().tagging().resumeListeners();
//		referenceLabelMap.getModel().colors().resumeListeners();
	}

	@Override
	public void removeFromBdv() {
		setVisible(false);
		String tag = getName();
		LabelEditorColor faceColor = referenceLabelMap.getModel().colors().getFaceColor(tag);
		if(faceColor.getClass().isAssignableFrom(LabelEditorValueColor.class)) {
			LabelEditorValueColor<DoubleType> lecolor = (LabelEditorValueColor<DoubleType>) faceColor;
			lecolor.setMinColor(0, 0, 0, 0);
			lecolor.setMaxColor(0,0,0,0);
		} else {
			faceColor.set(0,0,0,0);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public List<BdvSource> getSources() {
		return sources;
	}

	@Override
	public void setSources(List<BdvSource> sources) {
		this.sources = sources;
	}

	@Override
	public boolean exists() {
		return referenceTable.exists() && referenceLabelMap.exists();
	}

	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	@Override
	public void setColor(int color) {
		super.setColor(color);
	}

	@Override
	public void updateBdvColor() {
		String tag = getName();
		LabelEditorColor faceColor = referenceLabelMap.getModel().colors().getFaceColor(tag);
		if(faceColor.getClass().isAssignableFrom(LabelEditorValueColor.class)) {
			LabelEditorValueColor<DoubleType> lecolor = (LabelEditorValueColor<DoubleType>) faceColor;
			if(colorMaxValue) {
				lecolor.setMaxColor(getColor());
				lecolor.setMinColor(0,0,0,0);
			} else {
				lecolor.setMinColor(getColor());
				lecolor.setMaxColor(0,0,0,0);
			}
		} else {
			faceColor.set(getColor());
		}
	}

	public void setColorForMaxValues(boolean colorMaxValue) {
		this.colorMaxValue = colorMaxValue;
	}

	public boolean getColorMax() {
		return this.colorMaxValue;
	}
}
