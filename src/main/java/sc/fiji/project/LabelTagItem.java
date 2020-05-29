package sc.fiji.project;

import bdv.util.BdvSource;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import sc.fiji.labeleditor.core.model.colors.LabelEditorValueColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LabelTagItem extends AbstractItem implements DisplayableInBdv {

	private final LabelMapFileItem referenceLabelMap;
	private final TableFileItem referenceTable;
	private int referenceColumn;
	private boolean visible = false;
	private final List<BdvSource> sources = new ArrayList<>();

	private boolean colorMaxValue = true;
	private double maxValue;
	private double minValue;

	public LabelTagItem(BdvProject app, String name, LabelMapFileItem referenceLabelMap, TableFileItem referenceTable, int referenceColumn) {
		super(name);
		this.referenceLabelMap = referenceLabelMap;
		this.referenceTable = referenceTable;
		this.referenceColumn = referenceColumn;
	}

	@Override
	public void addToBdv() {
//		InteractiveTableDisplayViewer viewer = new InteractiveTableDisplayViewer(new BdvAppTable(referenceLabelMap.getModel(), referenceTable.getTable()));
//		viewer.display();
		setVisible(true);
		referenceLabelMap.getModel().colors().pauseListeners();
		referenceLabelMap.getModel().tagging().pauseListeners();
		int column = referenceColumn;
		String tag = getName();
		for (int i = 0; i < referenceTable.getTable().getRowCount(); i++) {
			IntType label = new IntType(Integer.valueOf(referenceTable.getTable().getRowHeader(i)));
			DoubleType value = new DoubleType(Double.parseDouble(referenceTable.getTable().get(column, i).toString()));
			referenceLabelMap.getModel().tagging().addValueToLabel(tag, value, label);
		}
		LabelEditorValueColor<DoubleType> color = referenceLabelMap.getModel().colors().makeValueFaceColor(tag, new DoubleType(minValue), new DoubleType(maxValue));
		if(colorMaxValue) {
			color.setMaxColor(getColor());
			color.setMinColor(0,0,0,0);
		} else {
			color.setMinColor(getColor());
			color.setMaxColor(0,0,0,0);
		}
		referenceLabelMap.getModel().tagging().resumeListeners();
		referenceLabelMap.getModel().colors().resumeListeners();
	}

	@Override
	public void removeFromBdv() {
		//TODO close table
		//TODO I don't think this works
		setVisible(true);
		referenceLabelMap.getModel().colors().pauseListeners();
		referenceLabelMap.getModel().tagging().pauseListeners();
		String tag = getName();
		for (int i = 0; i < referenceTable.getTable().getRowCount(); i++) {
			IntType label = new IntType(Integer.valueOf(referenceTable.getTable().getRowHeader(i)));
			referenceLabelMap.getModel().tagging().removeTagFromLabel(tag, label);
		}
		LabelEditorValueColor<DoubleType> color = referenceLabelMap.getModel().colors().makeValueFaceColor(tag, new DoubleType(minValue), new DoubleType(maxValue));
		if(colorMaxValue) {
			color.setMaxColor(getColor());
			color.setMinColor(0,0,0,0);
		} else {
			color.setMinColor(getColor());
			color.setMaxColor(0,0,0,0);
		}
		referenceLabelMap.getModel().tagging().resumeListeners();
		referenceLabelMap.getModel().colors().resumeListeners();
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
		LabelEditorValueColor<DoubleType> lecolor = (LabelEditorValueColor<DoubleType>) referenceLabelMap.getModel().colors().getFaceColor(tag);
		if(colorMaxValue) {
			lecolor.setMaxColor(getColor());
			lecolor.setMinColor(0,0,0,0);
		} else {
			lecolor.setMinColor(getColor());
			lecolor.setMaxColor(0,0,0,0);
		}
	}

	public void setColorForMaxValues(boolean colorMaxValue) {
		this.colorMaxValue = colorMaxValue;
	}

}
