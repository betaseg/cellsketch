package de.frauzufall.cellsketch;

import de.frauzufall.cellsketch.model.BdvItemGroup;
import de.frauzufall.cellsketch.model.FileItem;
import net.imglib2.RandomAccessibleInterval;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadata;
import org.janelia.saalfeldlab.n5.metadata.N5CosemMetadataParser;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.scijava.Disposable;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;
import de.frauzufall.cellsketch.model.Project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface BdvProject extends Project, Disposable {

    BdvInterface labelEditorInterface();
	void addImageFile(Path file, String fileName, String type, double scaleX, double scaleY, double scaleZ) throws IOException;

    String getDefaultFileName(String directory, String name);

    String getDefaultFileName(String name);

    void writeImage(String raw_name, RandomAccessibleInterval img, N5CosemMetadataParser metaWriter, N5CosemMetadata metadata, Double max) throws IOException;
    DataSelection getDataSelection(String name);
	Map<String, Object> projectData();
	N5LabelViewer viewer();

    void deleteFileItem(FileItem fileItem) throws IOException;
    void deleteItemGroup(BdvItemGroup bdvItemGroup) throws IOException;
}
