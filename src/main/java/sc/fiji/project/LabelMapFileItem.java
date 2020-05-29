package sc.fiji.project;

import bdv.util.BdvSource;
import net.imglib2.type.numeric.integer.IntType;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.view.DefaultLabelEditorView;
import sc.fiji.labeleditor.core.view.LabelEditorView;

import java.util.List;

public class LabelMapFileItem extends ImageFileItem<IntType> {

	private LabelEditorView<IntType> view;
	private LabelEditorModel<IntType> model;
	private DefaultInteractiveLabeling<IntType> labeling;

	public LabelMapFileItem(BdvProject app, String name) {
		super(app, name);
	}

	@Override
	public void addToBdv() {
		if(isVisible()) return;
		view = new DefaultLabelEditorView<>(getModel());
		if(project().context() != null) project().context().inject(view);
		view.addDefaultRenderers();
		getModel().setName(getName());
		labeling = project().labelEditorInterface().control(getModel(), view);
		updateBdvColor();
		setVisible(true);
		project().updateUI();
	}

	@Override
	public void removeFromBdv() {
		project().labelEditorInterface().remove(labeling);
		setVisible(false);
	}

	@Override
	public void updateBdvColor() {
		model.colors().getDefaultFaceColor().set(0x000000);
		model.colors().getDefaultBorderColor().set(getColor());
	}

	@Override
	public List<BdvSource> getSources() {
		return project().labelEditorInterface().getSources().get(view);
	}

	public LabelEditorModel<IntType> getModel() {
		if(model == null && getImage() != null) {
			model = DefaultLabelEditorModel.initFromLabelMap(getImage());
		}
		return model;
	}

}
