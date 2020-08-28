package sc.fiji.project;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import org.scijava.Context;
import org.scijava.io.IOService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.ui.UIService;
import org.yaml.snakeyaml.Yaml;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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
	private BdvHandlePanel bdvHandlePanel;
	private BdvInterface labelEditorInterface;

	private static final String projectFileName = "bdv-project.yaml";

	private ImageFileItem sourceItem;
	private String title;
	private Map<String, Object> projectData;

	private boolean editable;

	public DefaultBdvProject(File projectDir, Context context) {
		super("");
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
			loadProject();
		} catch (IOException e) {
			e.printStackTrace();
		}
		makeFrame();
	}

	public void loadProject() throws IOException {
		File projectFile = new File(projectDir, projectFileName);
		System.out.println("project file: " + projectFile.getAbsolutePath());

		if(projectFile.exists()) {
			InputStream stream = new FileInputStream(projectFile);
			Yaml yaml = new Yaml();
			projectData = yaml.load(stream);
			System.out.println(projectData);
			loadItemConfig(projectData, sourceItem);
			title = (String) projectData.get("title");
			loadConfigFrom(projectData);
		}
	}

	private void makeFrame() {
		JFrame frame = new JFrame();
		labelEditorInterface = new BdvInterface(context);
		bdvHandlePanel = new BdvHandlePanel(frame, new BdvOptions().accumulateProjectorFactory(labelEditorInterface.projector()));
		labelEditorInterface.setup(bdvHandlePanel);
		card = new BdvProjectCard();
		getItems().forEach(item -> card.addItem(item));
		card.build(title);
//		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD, false);
//		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCES_CARD, false);
//		bdvHandlePanel.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_VIEWERMODES_CARD, false);
//		bdvHandlePanel.getCardPanel().addCard("BDV Project", card, true );
//		bdvHandlePanel.getSplitPanel().updateUI();
		bdvHandlePanel.getViewerPanel().setMinimumSize(new Dimension(600, 600));
		JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, bdvHandlePanel.getViewerPanel(), card);
		splitPane.setDividerLocation(0.3);
		splitPane.revalidate();
		frame.setContentPane(splitPane);
		frame.pack();
		frame.setMinimumSize(new Dimension(900, 600));
//		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
//		bdvHandlePanel.getSplitPanel().setCollapsed(false);
//		bdvHandlePanel.getSplitPanel().setDividerLocation(0.6);
	}

	public void save() throws IOException {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("title", title);
		saveConfigTo(data);
		System.out.println(data);

		Yaml yaml = new Yaml();
		File file = new File(projectDir, projectFileName);
		if(!file.exists()) file.createNewFile();
		FileWriter writer = new FileWriter(file);
		yaml.dump(data, writer);
	}

	@Override
	public Context context() {
		return context;
	}

	@Override
	public Bdv getBdv() {
		return bdvHandlePanel;
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
	public boolean isEditable() {
		return editable;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public void dispose() {
		if(bdvHandlePanel != null) bdvHandlePanel.close();
	}
}
