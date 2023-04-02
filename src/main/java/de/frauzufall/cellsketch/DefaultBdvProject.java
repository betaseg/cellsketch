package de.frauzufall.cellsketch;

import bdv.ui.BdvDefaultCards;
import bdv.util.BdvHandle;
import de.frauzufall.cellsketch.model.*;
import de.frauzufall.cellsketch.ui.ProjectActionsCard;
import de.frauzufall.cellsketch.ui.ProjectItemsCard;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.janelia.saalfeldlab.n5.*;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.*;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMetadataParser;
import org.janelia.saalfeldlab.n5.metadata.imagej.CosemToImagePlus;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.app.event.StatusEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.ui.UIService;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultBdvProject extends DefaultItemGroup implements BdvProject {

	@Parameter
	private IOService ioService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private UIService uiService;

	@Parameter
	private Context context;

	@Parameter
	private EventService eventService;

	private boolean displayAll = false;
	private final File projectDir;
	private ProjectItemsCard itemsCard;
	private BdvHandle bdvHandlePanel;
	private BdvInterface labelEditorInterface;

	private ImageFileItem sourceItem;
	private final Map<String, Object> projectData = new HashMap<>();

	private boolean editable = false;
	private N5LabelViewer viewer;
	protected ProjectActionsCard actionsCard;
	private double pixelToUM;
	private final List<String> processes = new ArrayList<>();

	public DefaultBdvProject(File parent, String title, Context context) {
		super(title, false);
		parent.mkdirs();
		this.projectDir = new File(parent, title + ".n5");
		context.inject(this);
	}

	public DefaultBdvProject(File projectDir, Context context) {
		super(StringUtils.stripEnd(projectDir.getName(), ".n5"), false);
		this.projectDir = projectDir;
		context.inject(this);
	}

	@Override
	public void setSourceItem(ImageFileItem item) {
		sourceItem = item;
	}

	@Override
	public ImageFileItem getSourceItem() {
		return sourceItem;
	}



	@Override
	public File getProjectDir() {
		return projectDir;
	}

	@Override
	public void run() {
		try {
			DataSelection dataSelection = getDataSelection(sourceItem.getDefaultFileName());
			if (dataSelection == null) return;
			labelEditorInterface = new BdvInterface(context);
			projectData.put(sourceItem.getDefaultFileName(), dataSelection);
			viewer = new N5LabelViewer(dataSelection, labelEditorInterface, this);
			sourceItem.setSources(viewer.getSourceSources());
			bdvHandlePanel = viewer.getBdv();
			appendLabelEditor();
			sourceItem.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		sourceItem.display();
	}

	@Override
	public DataSelection getDataSelection(String raw_name) {
		final ArrayList<N5Metadata> selectedMetadata = new ArrayList<>();
		N5Importer.N5ViewerReaderFun n5Fun = new N5Importer.N5ViewerReaderFun();
		N5Reader n5 = n5Fun.apply(projectDir.getAbsolutePath());
		ExecutorService loaderExecutor = Executors.newCachedThreadPool();
		final N5MetadataParser<?>[] groupParsers = new N5MetadataParser[]{
				new N5CosemMultiScaleMetadata.CosemMultiScaleParser(),
				new N5ViewerMultiscaleMetadataParser(),
				new CanonicalMetadataParser(),
				new N5ViewerMultichannelMetadata.N5ViewerMultichannelMetadataParser()
		};

		final N5MetadataParser<?>[] parsers = new N5MetadataParser[]{
				new N5CosemMetadataParser(),
				new N5SingleScaleMetadataParser(),
				new CanonicalMetadataParser(),
				new N5GenericSingleScaleMetadataParser()
		};

		final List<N5MetadataParser<?>> groupParserList = Arrays.asList(groupParsers);
		final List<N5MetadataParser<?>> parserList = Arrays.asList(parsers);
		N5DatasetDiscoverer datasetDiscoverer = new N5DatasetDiscoverer(n5, loaderExecutor, parserList, groupParserList);
		N5TreeNode node = datasetDiscoverer.parse(raw_name);
		if (node.isDataset() && node.getMetadata() != null) {
			selectedMetadata.add(node.getMetadata());
		}

		if (!node.isDataset() || node.getMetadata() == null) {
//			JOptionPane.showMessageDialog(null, "Could not find a dataset / metadata at the provided path at " + raw_name);
			return null;
		}

		DataSelection dataSelection = new DataSelection(n5, selectedMetadata);
		return dataSelection;
	}

	public ProjectActionsCard getProgressCard() {
		return actionsCard;
	}

	private void appendLabelEditor() {
		actionsCard = new ProjectActionsCard();
		actionsCard.build("Actions", this);
		itemsCard = new ProjectItemsCard();
		populateModel();
		itemsCard.build(getName());
		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_VIEWERMODES_CARD, false);
		bdvHandlePanel.getCardPanel().addCard("Actions", actionsCard, true );
		bdvHandlePanel.getCardPanel().addCard("Items", itemsCard, true );
		bdvHandlePanel.getSplitPanel().updateUI();
		bdvHandlePanel.getCardPanel().removeCard(BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD);
		bdvHandlePanel.getCardPanel().removeCard(BdvDefaultCards.DEFAULT_SOURCES_CARD);
		bdvHandlePanel.getCardPanel().removeCard(BdvDefaultCards.DEFAULT_VIEWERMODES_CARD);
		bdvHandlePanel.getViewerPanel().setMinimumSize(new Dimension(600, 600));
		bdvHandlePanel.getSplitPanel().setCollapsed(false);
	}

	public void populateModel() {
		if(itemsCard != null) {
			itemsCard.clear();
			getItems().forEach(item -> itemsCard.addItem(item));
		}
	}

	@Override
	public boolean load() throws IOException {
		for(Item item : getItems()) item.load();
		return true;
	}

	@Override
	public Context context() {
		return context;
	}

	@Override
	public BdvInterface labelEditorInterface() {
		return labelEditorInterface;
	}

	@Override
	public void updateUI() {
		if(itemsCard != null) SwingUtilities.invokeLater(itemsCard.getItemsModel()::fireTableDataChanged);
	}

	@Override
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@Override
	public void create(File input, double pixelToUM, double scaleX, double scaleY, double scaleZ) {
		ImagePlus imp = IJ.openImage(input.getAbsolutePath());
		try {
			writeToN5(imp, sourceItem.getDefaultFileName(), scaleX, scaleY, scaleZ);
			imp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.pixelToUM = pixelToUM;
	}

	@Override
	public void loadConfig() throws IOException {
		super.loadConfig();
		N5Reader reader = new N5FSReader(getConfigPath());
		Double pixelToUM = reader.getAttribute(File.separator, "pixelToUM", Double.class);
		if(pixelToUM != null) {
			this.pixelToUM = pixelToUM;
		}
		reader.close();
	}

	@Override
	public void saveConfig() throws IOException {
		super.saveConfig();
		N5Writer writer = new N5FSWriter(getConfigPath());
		writer.setAttribute(File.separator, "pixelToUM", pixelToUM);
		writer.close();
	}

	protected String getConfigPath() {
		return getProjectDir().getAbsolutePath();
	}

	@Override
	public void addFile(Path file, String fileName) throws IOException {
		FileUtils.copyFile(file.toFile(), new File(projectDir, fileName));
	}

	@Override
	public double getPixelToUM() {
		return this.pixelToUM;
	}

	@Override
	public void addImageFile(Path file, String fileName, String type, double scaleX, double scaleY, double scaleZ) throws IOException {
		context().service(StatusService.class).showStatus("Importing dataset " + fileName + " from " + file + "..");
		ImagePlus imp = IJ.openImage(file.toAbsolutePath().toString());
		writeToN5(imp, fileName, type, scaleX, scaleY, scaleZ);
		imp.close();
		context().service(StatusService.class).showStatus("Successfully imported dataset " + fileName + ".");
	}

	@Override
	public String getDefaultFileName(String directory, String name) {
		return File.separator + directory + File.separator + this.getName() + "_" + name;
	}

	@Override
	public String getDefaultFileName(String name) {
		return File.separator + this.getName() + "_" + name;
	}

	private void writeToN5(ImagePlus imp, String raw_name, double scaleX, double scaleY, double scaleZ) throws IOException {
		writeToN5(imp, raw_name, null, scaleX, scaleY, scaleZ);
	}

	private void writeToN5(ImagePlus imp, String raw_name, String type, double scaleX, double scaleY, double scaleZ) throws IOException {
		if(imp.getNDimensions() != 3) {
			throw new RuntimeException("Can only process datasets with 3 dimensions.");
		}
		Img img = ImageJFunctions.wrap(imp);
		if(type != null) {
			if(type.equals("int")) {
				img = context().service(OpService.class).convert().int16(img);
			} else {
				if(type.equals("byte")) {
					img = context().service(OpService.class).convert().bit(img);
				}
			}
		}
		InterpolatorFactory interpolator = new NearestNeighborInterpolatorFactory();
		RandomAccessibleInterval rai = context.service(OpService.class).transform().scaleView(img, new double[]{scaleX, scaleY, scaleZ}, interpolator);
		N5CosemMetadataParser metaWriter = new N5CosemMetadataParser();
		CosemToImagePlus metadata2IJ = new CosemToImagePlus();
		final N5CosemMetadata metadata = metadata2IJ.readMetadata(imp);
		double max = metadata.maxIntensity();
		double min = metadata.minIntensity();
		// NOTE not using the imp metadata because we scale the image and that messes with the correctness of the metadata
		writeImage(raw_name, rai, null, null, min, max);

	}

	public void writeImage(String raw_name, RandomAccessibleInterval img, N5CosemMetadataParser metaWriter, N5CosemMetadata metadata, Double min, Double max) throws IOException {
		int[] blocksize = new int[]{64,64,64};
		N5Writer writer = new N5FSWriter(projectDir.getAbsolutePath());
		N5Utils.save(img, writer, raw_name, blocksize, new RawCompression());
		if (metaWriter != null) {
			try {
				metaWriter.writeMetadata(metadata, writer, raw_name);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if(max != null) {
			writer.setAttribute(raw_name, "max", max);
			writer.setAttribute(raw_name, "min", min);
		}
		writer.close();
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public void dispose() {
		unload();
		if(bdvHandlePanel != null) bdvHandlePanel.close();
		context().dispose();
	}

	@Override
	public Map<String, Object> projectData() {
		return projectData;
	}

	@Override
	public N5LabelViewer viewer() {
		return viewer;
	}

	@Override
	public void deleteFileItem(FileItem fileItem) throws IOException {
		if(fileItem.exists()) {
			fileItem.unload();
			if(fileItem.getFile().isDirectory()) {
				FileUtils.deleteDirectory(fileItem.getFile());
			} else {
				Files.delete(fileItem.getFile().toPath());
			}
			updateUI();
		}
	}

	@Override
	public void deleteItemGroup(BdvItemGroup bdvItemGroup) throws IOException {
		bdvItemGroup.unload();
	}

	@EventHandler
	protected void onEvent(StatusEvent event) {
//		int val = event.getProgressValue();
//		int max = event.getProgressMaximum();
		String message = this.uiService.getStatusMessage(event);
		System.out.println(message);
	}

	public void endProgress(String progressName) {
		removeProgress(progressName);
	}

	public void startProgress(String progressName) {
		addProgress(progressName);
	}

	public void addProgress(String progressName) {
		this.processes.add(progressName);
		if(getProgressCard() != null) {
			actionsCard.setEnabled(false);
		}
		context().service(StatusService.class).showStatus(progressName);
	}

	public void removeProgress(String progressName) {
		this.processes.remove(progressName);
		if(this.processes.size() == 0) {
			if(getProgressCard() != null) {
				actionsCard.setEnabled(true);
			}
			context().service(StatusService.class).clearStatus();
		}
	}

}
