package sc.fiji.project;

import bdv.ui.BdvDefaultCards;
import bdv.util.BdvHandle;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
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
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.ui.UIService;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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

	private boolean displayAll = false;
	private final File projectDir;
	private BdvProjectCard card;
	private BdvHandle bdvHandlePanel;
	private BdvInterface labelEditorInterface;

	private ImageFileItem sourceItem;
	private final Map<String, Object> projectData = new HashMap<>();

	private boolean editable;
	private N5LabelViewer viewer;

	public DefaultBdvProject(File parent, String title, Context context) {
		super(title);
		parent.mkdirs();
		this.projectDir = new File(parent, title + ".n5");
		context.inject(this);
	}

	public DefaultBdvProject(File projectDir, Context context) {
		super(StringUtils.stripEnd(projectDir.getName(), ".n5"));
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
			viewer = new N5LabelViewer(dataSelection, labelEditorInterface);
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

	private void appendLabelEditor() {
		card = new BdvProjectCard();
		getItems().forEach(item -> card.addItem(item));
		card.build(getName());
		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD, false);
		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCES_CARD, false);
		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_VIEWERMODES_CARD, false);
		bdvHandlePanel.getCardPanel().addCard("BDV Project", card, true );
		bdvHandlePanel.getSplitPanel().updateUI();
		bdvHandlePanel.getViewerPanel().setMinimumSize(new Dimension(600, 600));
//		JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, bdvHandlePanel.getViewerPanel(), card);
//		splitPane.setDividerLocation(0.3);
//		splitPane.revalidate();
//		frame.setContentPane(splitPane);
//		frame.pack();
//		frame.setMinimumSize(new Dimension(900, 600));
////		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
//		frame.setVisible(true);
//		bdvHandlePanel.getSplitPanel().setCollapsed(false);
//		bdvHandlePanel.getSplitPanel().setDividerLocation(0.6);
	}

	public void save() throws IOException {
//		Map<String, Object> data = new LinkedHashMap<>();
//		data.put("title", title);
//		saveConfigTo(data);
//		System.out.println(data);
//
//		Yaml yaml = new Yaml();
//		File file = getBdvProjectXmlFile();
//		if(!file.exists()) file.createNewFile();
//		FileWriter writer = new FileWriter(file);
//		yaml.dump(data, writer);
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
		if(card != null) SwingUtilities.invokeLater(card.getItemsModel()::fireTableDataChanged);
	}

	@Override
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@Override
	public void create(File input, ImageFileItem sourceItem) {
		ImagePlus imp = IJ.openImage(input.getAbsolutePath());
//		createBdvProject(imp);
		createBdvN5Project(imp, sourceItem.getDefaultFileName());
	}

	@Override
	public void addFile(Path file, String fileName) throws IOException {
		FileUtils.copyFile(file.toFile(), new File(projectDir, fileName));
	}

	@Override
	public void addImageFile(Path file, String fileName) throws IOException {
		addImageFile(file, fileName, null);
	}

	@Override
	public void addImageFile(Path file, String fileName, String type) throws IOException {
		System.out.println("Importing dataset " + fileName + " from " + file + "..");
		ImagePlus imp = IJ.openImage(file.toAbsolutePath().toString());
		writeToN5(imp, fileName, type);
		imp.close();
		System.out.println("Successfully imported dataset " + fileName + ".");
	}

	@Override
	public boolean imageFileLoaded(String name) {
		return projectData.containsKey(name);
	}

	private void createBdvN5Project(ImagePlus imp, String name) {
		try {
			writeToN5(imp, name);
			imp.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeToN5(ImagePlus imp, String raw_name) throws IOException {
		writeToN5(imp, raw_name, null);
	}

	private void writeToN5(ImagePlus imp, String raw_name, String type) throws IOException {
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
		N5CosemMetadataParser metaWriter = new N5CosemMetadataParser();
		CosemToImagePlus metadata2IJ = new CosemToImagePlus();
		final N5CosemMetadata metadata = metadata2IJ.readMetadata(imp);
		double max = imp.getStatistics().max;
		writeImage(raw_name, img, metaWriter, metadata, max);

	}

	public void writeImage(String raw_name, RandomAccessibleInterval img, N5CosemMetadataParser metaWriter, N5CosemMetadata metadata, Double max) throws IOException {
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
			writer.setAttribute(raw_name, "bounds", new double[]{0, max});
		}
		writer.close();
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public void dispose() {
		if(bdvHandlePanel != null) bdvHandlePanel.close();
	}

	@Override
	public Map<String, Object> projectData() {
		return projectData;
	}

	@Override
	public N5LabelViewer viewer() {
		return viewer;
	}
}
