package de.frauzufall.cellsketch.model;

import de.frauzufall.cellsketch.model.ImageFileItem;
import de.frauzufall.cellsketch.model.ItemGroup;
import org.scijava.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface Project extends ItemGroup {

	/**
	 * Assigns one item as the source item (e.g. the raw data) for this project.
	 * This will be used as background e.g. when editing one labeling separately.
	 */
	void setSourceItem(ImageFileItem item);


	/**
	 * Get the item assigned as source (e.g. the raw data)
	 */
	ImageFileItem getSourceItem();

	/**
	 * Start the app
	 */
	void run();

	/**
	 * @return the root directory of the current project
	 */
	File getProjectDir();

	Context context();

	void updateUI();

	boolean isEditable();
	void setEditable(boolean editable);

    void create(File input, double pixelToUM, double scaleX, double scaleY, double scaleZ);

    void addFile(Path file, String fileName) throws IOException;
	void loadConfig() throws IOException;
	void saveConfig() throws IOException;

	double getPixelToUM();
}
