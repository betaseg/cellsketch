package sc.fiji.project;

import de.csbdresden.betaseg.analysis.AnalyzeUtils;
import de.csbdresden.betaseg.export.PlyExporter;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.io.IOService;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.view.DefaultLabelEditorView;
import sc.fiji.labeleditor.core.view.LabelEditorView;
import sc.fiji.labeleditor.plugin.renderers.DefaultLabelEditorRenderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LabelMapFileItem<T extends IntegerType<T>> extends ImageFileItem<T> {

	private LabelEditorView<IntType> view;
	private LabelEditorModel<IntType> model;
	private DefaultInteractiveLabeling<IntType> labeling;
	private final List<LabelTagItem> tagItems = new ArrayList<>();

	public LabelMapFileItem(BdvProject app, String name) {
		super(app, name);
		if(project().isEditable()) {
			PlyExporter plyExporter = new PlyExporter(project().context().service(OpService.class), this);
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
		IOService io = project().context().service(IOService.class);
		DatasetService datasetService = project().context().service(DatasetService.class);
		if(exportMask.exists()) {
			return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
		} else {
			RandomAccessibleInterval tagMask = AnalyzeUtils.asMask(getImage());
			exportMask.getParentFile().mkdirs();
			io.save(datasetService.create(tagMask), exportMask.getAbsolutePath());
		}
		return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
	}

	private String getExportName() {
		return nameToFileName();
	}

	private File getExportDir() {
		return new File(project().getProjectDir(), "export");
	}

	@Override
	public void addToBdv() {
		if(isVisible()) return;
		view = new DefaultLabelEditorView<>(getModel());
		if(project().context() != null) project().context().inject(view);
		view.addDefaultRenderers();
//		view.add(new DefaultLabelEditorRenderer<>());
		getModel().setName(getName());
		labeling = project().labelEditorInterface().control(getModel(), view);
		updateBdvColor();
		setVisible(true);
		project().updateUI();
	}

	@Override
	public void removeFromBdv() {
		if(project().labelEditorInterface() != null) {
			project().labelEditorInterface().remove(labeling);
		}
		setVisible(false);
	}

	@Override
	public void updateBdvColor() {
		model.colors().getDefaultFaceColor().set(0x000000);
		model.colors().getDefaultBorderColor().set(getColor());
	}

	public LabelEditorModel<IntType> getModel() {
		if(model == null && getImage() != null) {
			model = DefaultLabelEditorModel.initFromLabelMap(getImage());
			for (LabelTagItem tagItem : tagItems) {
				tagItem.addTag();
			}
		}
		return model;
	}

	public LabelTagItem addLabel(String title, TableFileItem referenceTable, int column, Class columnClass) {
		LabelTagItem labelTagItem = new LabelTagItem(title, this, referenceTable, column, columnClass);
		tagItems.add(labelTagItem);
		return labelTagItem;
	}
}
