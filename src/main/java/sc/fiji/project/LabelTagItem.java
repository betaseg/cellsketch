package sc.fiji.project;

import bdv.util.BdvSource;
import de.csbdresden.betaseg.export.PlyExporter;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import org.scijava.io.IOService;
import sc.fiji.labeleditor.core.model.colors.LabelEditorColor;
import sc.fiji.labeleditor.core.model.colors.LabelEditorValueColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LabelTagItem extends AbstractItem implements DisplayableInBdv {

	private final LabelMapFileItem referenceLabelMap;
	private final TableFileItem referenceTable;
	private final Class tagClass;
	private int referenceColumn;
	private boolean visible = false;
	private final List<BdvSource> sources = new ArrayList<>();

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

	private RandomAccessibleInterval getTagMask() throws IOException {
		File exportMask = new File(getExportDir(), getExportName() + ".tif");
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

	private String getExportName() {
		return referenceLabelMap.nameToFileName() + "_" + nameToFileName();
	}

	private File getExportDir() {
		return new File(referenceLabelMap.project().getProjectDir(), "export");
	}

	public RandomAccessibleInterval<ByteType> hasTag() {
		Set labels = referenceLabelMap.getModel().tagging().getLabels(tag);
		Converter<LabelingType, ByteType> converter = (input, output) -> {
			if(input.size() > 0 && labels.contains(input.iterator().next())) output.setOne();
			else output.setZero();
		};
		return Converters.convert((RandomAccessibleInterval<LabelingType>)referenceLabelMap.getModel().labeling(), converter, new ByteType());
	}

	@Override
	public void addToBdv() {
//		InteractiveTableDisplayViewer viewer = new InteractiveTableDisplayViewer(new BdvAppTable(referenceLabelMap.getModel(), referenceTable.getTable()));
//		viewer.display();
		setVisible(true);
		updateBdvColor();
	}

	void addTag() {
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
		if(tagClass.isAssignableFrom(Double.class)) {
			LabelEditorValueColor<DoubleType> lecolor = (LabelEditorValueColor<DoubleType>) referenceLabelMap.getModel().colors().getFaceColor(tag);
			if(colorMaxValue) {
				lecolor.setMaxColor(0, 0, 0, 0);
				lecolor.setMinColor(0,0,0,0);
			} else {
				lecolor.setMinColor(0, 0, 0, 0);
				lecolor.setMaxColor(0,0,0,0);
			}
		} else {
			referenceLabelMap.getModel().colors().getFaceColor(tag).set(0,0,0,0);
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
		if(tagClass.isAssignableFrom(Double.class)) {
			LabelEditorValueColor<DoubleType> lecolor = (LabelEditorValueColor<DoubleType>) referenceLabelMap.getModel().colors().getFaceColor(tag);
			if(colorMaxValue) {
				lecolor.setMaxColor(getColor());
				lecolor.setMinColor(0,0,0,0);
			} else {
				lecolor.setMinColor(getColor());
				lecolor.setMaxColor(0,0,0,0);
			}
		} else {
			referenceLabelMap.getModel().colors().getFaceColor(tag).set(getColor());
		}
	}

	public void setColorForMaxValues(boolean colorMaxValue) {
		this.colorMaxValue = colorMaxValue;
	}

}
