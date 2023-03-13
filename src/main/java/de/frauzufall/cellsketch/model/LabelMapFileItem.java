package de.frauzufall.cellsketch.model;

import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.scijava.app.StatusService;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import de.frauzufall.cellsketch.BdvProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LabelMapFileItem<T extends IntegerType<T>> extends ImageFileItem<T> {

	private LabelEditorModel<IntType> model;
	private DefaultInteractiveLabeling<IntType> labeling;
	private final List<LabelTagItem> tagItems = new ArrayList<>();

	public LabelMapFileItem(BdvProject app, String defaultName, boolean deletable) {
		super(app, defaultName, deletable);
	}

	public List<LabelTagItem> getTagItems() {
		return tagItems;
	}

	@Override
	public void addToBdv() {
		if(isVisible()) return;
		project().context().service(StatusService.class).showStatus("Adding labelmap to BDV: " + getName());
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
		if(model != null) {
			model.colors().getDefaultFaceColor().set(ARGBType.rgba(
					ARGBType.red(getColor()),
					ARGBType.green(getColor()),
					ARGBType.blue(getColor()),
					0.2));
			model.colors().getDefaultBorderColor().set(getColor());
		}
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

	public LabelTagItem addLabel(String title, TableFileItem referenceTable, String columnName, Class columnClass) {
		for (LabelTagItem tagItem : tagItems) {
			if(tagItem.getReferenceTable().getDefaultFileName().equals(referenceTable.getDefaultFileName())
					&& tagItem.getReferenceColumnName().equals(columnName)) {
				System.out.println("Tag " + title + " already exists for labling " + this.getName());
				return tagItem;
			}
		}
		LabelTagItem labelTagItem = new LabelTagItem(title, this, referenceTable, columnName, columnClass);
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
