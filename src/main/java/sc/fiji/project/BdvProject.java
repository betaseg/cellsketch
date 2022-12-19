package sc.fiji.project;

import net.imglib2.RandomAccessibleInterval;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.scijava.Disposable;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface BdvProject extends Project, Disposable {

	BdvInterface labelEditorInterface();
	void addImageFile(Path path, String name) throws IOException;
	void addImageFile(Path path, String name, String type) throws IOException;
	void writeImage(String raw_name, RandomAccessibleInterval img, N5CosemMetadataParser metaWriter, N5CosemMetadata metadata, Double max) throws IOException;
	boolean imageFileLoaded(String name);
    DataSelection getDataSelection(String name);
	Map<String, Object> projectData();
	N5LabelViewer viewer();
}
