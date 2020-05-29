package sc.fiji.project;

import org.scijava.Context;

import java.io.File;
import java.io.IOException;

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

	void save() throws IOException;
	Context context();

	void updateUI();

	boolean isEditable();
	void setEditable(boolean editable);
}
