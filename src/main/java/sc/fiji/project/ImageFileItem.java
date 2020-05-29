package sc.fiji.project;

import bdv.util.AxisOrder;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import sc.fiji.project.command.GuessAxes;
import sc.fiji.project.command.ImportStackSlice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ImageFileItem<T extends NumericType<T>> extends FileItem implements DisplayableInBdv {

	private AxisOrder axisOrder;
	private Img<T> img;
	private final List<BdvSource> sources = new ArrayList<>();
	private boolean visible = false;

	public ImageFileItem(BdvProject app, String name) {
		super(app, name, "tif");
		if(project().isEditable()) {
			getActions().add(new DefaultAction(
					"Import from stack slice",
					Collections.emptyList(),
					this::importFromStackSlice
			));
		}
	}

	private boolean importFromStackSlice() {
		CommandModule res = null;
		try {
			res = project().context().getService(CommandService.class).run(ImportStackSlice.class, true).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		if(res == null || res.isCanceled()) {
			return false;
		}
		File stackFile = (File) res.getInput("stackFile");
		int dimension = (int) res.getInput("dimension");
		long position = (long) res.getInput("position");
		if(stackFile == null) return false;
		try {
			Img<T> img = (Img<T>) project().context().getService(IOService.class).open(stackFile.getAbsolutePath());
			IntervalView<T> slice = Views.hyperSlice(img, dimension, position);
			Img<T> newImg = img.factory().create(slice);
			project().context().getService(OpService.class).copy().rai(newImg, slice);
			setImage(newImg);
			save();
			project().save();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		project().updateUI();
		return true;
	}

	@Override
	protected boolean importAsFile() {
		boolean imported = super.importAsFile();
		if(!imported) return false;
		try {
			Img<T> rai = (Img<T>) project().context().getService(IOService.class).open(getFile().getAbsolutePath());
			setImageAskAxisType(rai);
			project().save();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		project().updateUI();
		return true;
	}

	private void setImageAskAxisType(Img<T> rai) {
		String axes = askForAxes(rai);
		axisOrder = convertToAxisTypes(rai, axes);
		setImage(rai);
	}

	private AxisOrder convertToAxisTypes(Img<T> rai, String axes) {
		if(axes == null || axes.length() != rai.numDimensions()) {
			axes = "XYZCT".substring(0, rai.numDimensions());
		}
		return AxisOrder.valueOf(axes);
	}

	private String askForAxes(Img<T> rai) {
		CommandModule res = null;
		try {
			String dimensions = Arrays.toString(Intervals.dimensionsAsIntArray(rai));
			System.out.println(dimensions);
			res = project().context().getService(CommandService.class).run(
					GuessAxes.class, true,
					"dimensions", "Dimensions: " + dimensions).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		if(res == null || res.isCanceled()) {
			return null;
		}
		return (String) res.getInput("axes");
	}

	public RandomAccessibleInterval<T> getImage() {
		if(exists() && img == null) {
			try {
				load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return img;
	}

	public void setImage(Img<T> img) {
		this.img = img;
	}

	@Override
	public boolean save() throws IOException {
		Dataset dataset = project().context().service(DatasetService.class).create(getImage());
		project().context().service(IOService.class).save(dataset, getFile().getAbsolutePath());
		project().save();
		load();
		removeFromBdv();
		return true;
	}

	@Override
	protected boolean load() throws IOException {
		if(getFile() == null || !getFile().exists()) {
			System.out.println("Could not load image " + getName() + " from " + getFile().getAbsolutePath());
			return false;
		}
		System.out.println("Loading image " + getName() + " from " + getFile().getAbsolutePath());
		Img<T> img = (Img<T>) project().context().getService(IOService.class).open(getFile().getAbsolutePath());
		System.out.println("Image dimensions: " + Arrays.toString(Intervals.dimensionsAsIntArray(img)));
		if(axisOrder == null) {
			setImageAskAxisType(img);
			project().save();
		} else {
			setImage(img);
		}
		project().updateUI();
		return true;
	}

	@Override
	public void addToBdv() {
		if(isVisible()) return;
		getSources().add(BdvFunctions.show(getImage(), getName(), BdvOptions.options().addTo(project().getBdv()).axisOrder(axisOrder)));
		updateBdvColor();
		setVisible(true);
		project().updateUI();
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
	public void setColor(int color) {
		super.setColor(color);
	}

	@Override
	public void updateBdvColor() {
		getSources().forEach(bdvSource -> bdvSource.setColor(new ARGBType(getColor())));
	}

	@Override
	public void loadConfigFrom(Map<String, Object> data) {
		super.loadConfigFrom(data);
		if(data.containsKey("axes")) {
			String axes = (String) data.get("axes");
			axisOrder = AxisOrder.valueOf(axes);
		}
	}

	@Override
	public void saveConfigTo(Map<String, Object> data) {
		super.saveConfigTo(data);
		if(axisOrder != null) data.put("axes", axisOrder.name());
	}

}
