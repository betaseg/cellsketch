package sc.fiji.project;

import bdv.util.BdvSource;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.ui.DataSelection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageFileItem<T extends NumericType<T>> extends FileItem implements DisplayableInBdv {

	private RandomAccessibleInterval<T> img;
	private List<BdvSource> sources = new ArrayList<>();
	private boolean visible = false;
	protected Double max;

	public ImageFileItem(BdvProject app, String name) {
		this(app, name, null);
	}

	public ImageFileItem(BdvProject app, String name, String defaultFileName) {
		super(app, name, "tif", defaultFileName);
	}

	@Override
	public boolean importAsFile(File file) throws IOException {
		if(file == null) return false;
		project().addImageFile(file.toPath(), getDefaultFileName());
		setFile(new File(getDefaultFileName()));
		project().updateUI();
		return true;
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

	@Override
	public boolean load() throws IOException {
		DataSelection selection = project().getDataSelection(getDefaultFileName());
		if(selection != null) {
			CachedCellImg img = N5Utils.open(new N5FSReader(project().getProjectDir().getAbsolutePath()), this.getDefaultFileName());
			setImage(img);
			return true;
		}
		return false;
	}

	@Override
	public void unload() {
		setImage(null);
	}

	public void setImage(RandomAccessibleInterval<T> img) {

		this.img = img;
	}

	@Override
	public boolean save() throws IOException {
		System.out.println("Saving loaded " + getName() + " dataset to " + getDefaultFileName() + "..");
		N5CosemMetadataParser metaWriter = new N5CosemMetadataParser();
		project().writeImage(getDefaultFileName(), getImage(), null, null, this.max);
		setFile(new File(getDefaultFileName()));
		project().updateUI();
		System.out.println("Successfully saved " + getName() + ".");
		return true;
	}

	@Override
	public void addToBdv() {
		if(isVisible()) return;
		System.out.println("add image to bdv: " + getName());
		String defaultFileName = this.getDefaultFileName();
		if(defaultFileName == null) return;
		DataSelection dataSelection = project().getDataSelection(defaultFileName);
		if (dataSelection != null) {
			try {
				System.out.println("loading " + defaultFileName);
				project().projectData().put(defaultFileName, dataSelection);
				List<BdvSource> sources = project().viewer().addData(dataSelection);
				this.setSources(sources);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setVisible(true);
		updateBdvColor();
		project().updateUI();
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public boolean exists() {
		return project().getDataSelection(getDefaultFileName()) != null;
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
		updateBdvColor();
	}

	@Override
	public void removeFromBdv() {
		System.out.println("remove from bdv: " + getName());
		DisplayableInBdv.super.removeFromBdv();
		setVisible(false);
		setImage(null);
	}

	@Override
	public void setColor(int color) {
		super.setColor(color);
	}

	@Override
	public void updateBdvColor() {
		getSources().forEach(bdvSource -> bdvSource.setColor(new ARGBType(getColor())));
		if(max != null) {
			getSources().forEach(bdvSource -> bdvSource.setDisplayRange(0, max));
		}
	}

	public void setMaxValue(double max) {
		this.max = max;
	}
}
