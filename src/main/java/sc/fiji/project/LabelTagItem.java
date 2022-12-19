package sc.fiji.project;

import bdv.util.BdvSource;
import sc.fiji.project.export.PlyExporter;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import org.scijava.io.IOService;
import sc.fiji.labeleditor.core.model.colors.LabelEditorColor;
import sc.fiji.labeleditor.core.model.colors.LabelEditorValueColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LabelTagItem extends AbstractItem implements DisplayableInBdv {

	private final LabelMapFileItem referenceLabelMap;
	private final TableFileItem referenceTable;
	private final Class tagClass;
	private int referenceColumn;
	private boolean visible = false;
	private List<BdvSource> sources = new ArrayList<>();

	private boolean colorMaxValue = true;
	private double maxValue;
	private double minValue;
	private String tag;

	LabelTagItem(String name, LabelMapFileItem referenceLabelMap, TableFileItem referenceTable, int referenceColumn, Class tagClass) {
		super(name);
		this.referenceLabelMap = referenceLabelMap;
		this.referenceTable = referenceTable;
		this.referenceColumn = referenceColumn;
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
		if(referenceLabelMap.project().isEditable()) {
			PlyExporter plyExporter = new PlyExporter(referenceLabelMap.project().context().service(OpService.class), referenceLabelMap);
			getActions().add(new DefaultAction(
					"Export as PLY",
					Arrays.asList(this),
					() -> {
						try {
							plyExporter.export(getExportDir(), getExportName(), getTagMask());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			));
		}
	}

	public void exportMask() {
		try {
			getTagMask();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exportInvertedMask() {
		try {
			getNoTagMask();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void plot() {
		int column = referenceColumn;
		tag = getName();
		String series = referenceLabelMap.getName();
		String title = series + ": " + tag;
		String xLabel = tag;
		String yLabel = "";
		int bins = 20;
		double[] data = PlotUtil.toDoubleArray(referenceTable.getTable().get(referenceColumn));
		PlotUtil.displayHistogram(data, series, title, xLabel, yLabel, bins);
	}

	private RandomAccessibleInterval getTagMask() throws IOException {
		File exportMask = new File(getExportDir(), referenceLabelMap.project().getName() + "_" + getExportName() + ".tif");
		IOService io = referenceLabelMap.project().context().service(IOService.class);
		DatasetService datasetService = referenceLabelMap.project().context().service(DatasetService.class);
		if(exportMask.exists()) {
			return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
		} else {
			RandomAccessibleInterval tagMask = hasTag();
			exportMask.getParentFile().mkdirs();
			io.save(datasetService.create(tagMask), exportMask.getAbsolutePath());
		}
		return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
	}

	private RandomAccessibleInterval getNoTagMask() throws IOException {
		File exportMask = new File(getExportDir(), referenceLabelMap.project().getName() + "_" + getInvertedExportName() + ".tif");
		IOService io = referenceLabelMap.project().context().service(IOService.class);
		DatasetService datasetService = referenceLabelMap.project().context().service(DatasetService.class);
		if(exportMask.exists()) {
			return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
		} else {
			RandomAccessibleInterval tagMask = hasNotTag();
			exportMask.getParentFile().mkdirs();
			io.save(datasetService.create(tagMask), exportMask.getAbsolutePath());
		}
		return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
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
		int column = referenceColumn;
		tag = getName();
		for (int i = 0; i < referenceTable.getTable().getRowCount(); i++) {
			IntType label = new IntType(Integer.valueOf(referenceTable.getTable().getRowHeader(i)));
			String rowValue = referenceTable.getTable().get(column, i).toString();
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
			if(colorMaxValue) {
				lecolor.setMaxColor(0, 0, 0, 0);
				lecolor.setMinColor(0,0,0,0);
			} else {
				lecolor.setMinColor(0, 0, 0, 0);
				lecolor.setMaxColor(0,0,0,0);
			}
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

	@Override
	public void loadConfigFrom(Map<String, Object> data) {
		//TODO
	}

	@Override
	public void saveConfigTo(Map<String, Object> data) {
		data.put("referenceLabeling", referenceLabelMap.getName());
		data.put("referenceTable", referenceTable.getName());
		data.put("referenceColumn", referenceColumn);
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

}
