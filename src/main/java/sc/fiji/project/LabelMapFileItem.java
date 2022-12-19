package sc.fiji.project;

import sc.fiji.project.export.PlyExporter;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.scijava.io.IOService;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LabelMapFileItem<T extends IntegerType<T>> extends ImageFileItem<T> {

	private LabelEditorModel<IntType> model;
	private DefaultInteractiveLabeling<IntType> labeling;
	private final List<LabelTagItem> tagItems = new ArrayList<>();

	public LabelMapFileItem(BdvProject app, String name, String defaultName) {
		super(app, name, defaultName);
		if(project().isEditable()) {
			PlyExporter plyExporter = new PlyExporter(project().context().service(OpService.class), this);
			getActions().add(new DefaultAction(
					"Export as PLY",
					Collections.singletonList(this),
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

	@Override
	public boolean importAsFile(File file) throws IOException {
		if(file == null) return false;
		project().addImageFile(file.toPath(), getDefaultFileName(), "int");
		setFile(new File(getDefaultFileName()));
		project().updateUI();
		return true;
	}

	private RandomAccessibleInterval getTagMask() throws IOException {
		File exportMask = new File(getExportDir(), getExportName() + ".tif");
		IOService io = project().context().service(IOService.class);
		DatasetService datasetService = project().context().service(DatasetService.class);
		if(exportMask.exists()) {
			return (RandomAccessibleInterval) io.open(exportMask.getAbsolutePath());
		} else {
			RandomAccessibleInterval tagMask = asMask(getImage());
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
		System.out.println("add labelmap to bdv: " + getName());
		String defaultFileName = this.getDefaultFileName();
		if(defaultFileName == null) return;
		DataSelection dataSelection = project().getDataSelection(getDefaultFileName());
		if(dataSelection != null) {
			try {
				load();
				LabelEditorModel<IntType> model = getModel();
				model.setName(getName());
				labeling = project().labelEditorInterface().control(model);
				project().projectData().put(defaultFileName, labeling);
				this.setSources(null);
				setVisible(true);
				updateBdvColor();
				project().updateUI();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
			ImgLabeling<IntType, IntType> labels = makeLabeling(getImage());
			model = new DefaultLabelEditorModel<>(labels);
			for (LabelTagItem tagItem : tagItems) {
				tagItem.addTag();
			}
		}
		return model;
	}

	private ImgLabeling<IntType, IntType> makeLabeling(RandomAccessibleInterval labelMap) {
		final ImgLabeling< IntType, IntType > labeling = new ImgLabeling( labelMap );
		RealType max = project().context().service(OpService.class).stats().max(Views.iterable(labelMap));
		final ArrayList<Set<IntType>> labelSets = new ArrayList<>();

		labelSets.add( new HashSet<>() ); // empty 0 label
		for (int label = 1; label <= max.getRealDouble(); ++label) {
			final HashSet< IntType > set = new HashSet< >();
			set.add( new IntType(label) );
			labelSets.add( set );
		}

		new LabelingMapping.SerialisationAccess<IntType>(labeling.getMapping()) {
			{
				super.setLabelSets(labelSets);
			}
		};

		return labeling;
	}

	public LabelTagItem addLabel(String title, TableFileItem referenceTable, int column, Class columnClass) {
		LabelTagItem labelTagItem = new LabelTagItem(title, this, referenceTable, column, columnClass);
		tagItems.add(labelTagItem);
		return labelTagItem;
	}

	public static <T extends IntegerType<T>> RandomAccessibleInterval<ByteType> asMask(RandomAccessibleInterval<T> image) {
		return Converters.convert(image, (in, out) -> {
			if(in.getInteger() != 0) out.setOne();
			else out.setZero();
		}, new ByteType());
	}
}
