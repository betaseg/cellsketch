package de.frauzufall.cellsketch.model;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import de.frauzufall.cellsketch.BdvProject;
import org.scijava.app.StatusService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class FileItem extends AbstractItem {

	private File file;
	private boolean deletable;
	protected String defaultFileName;
	private final BdvProject project;

	public FileItem(BdvProject project, String defaultFileName, boolean deletable) {
		this.project = project;
		this.deletable = deletable;
		this.defaultFileName = defaultFileName;
		if(defaultFileName!= null) {
			this.file = new File(project.getProjectDir(), defaultFileName);
		}
		if(deletable) {
			getActions().add(new DefaultAction(
					"Delete",
					Collections.singletonList(this),
					() -> {
						try {
							this.delete();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			));
		}
	}

	@Override
	public boolean load() throws IOException { return false; }

	@Override
	public void delete() throws IOException {
		project().deleteFileItem(this);
	}

	public boolean save() throws IOException { return false; }

	@Override
	public void loadConfig() throws IOException {
		if(!new File(getConfigPath()).exists()) return;
		N5Reader reader = new N5FSReader(getConfigPath());
		readAttributes(reader);
		reader.close();
	}

	protected void readAttributes(N5Reader reader) throws IOException {}

	protected String getConfigPath() {
		return new File(project().getProjectDir(), this.getDefaultFileName()).getAbsolutePath();
	}

	@Override
	public void saveConfig() throws IOException {
		if(!new File(getConfigPath()).exists()) return;
		N5Writer writer = new N5FSWriter(getConfigPath());
		writeAttributes(writer);
		writer.close();
		project.context().service(StatusService.class).showStatus("written config to " + getConfigPath());
	}

	protected void writeAttributes(N5Writer writer) throws IOException {}

	@Override
	public void display() {

	}

	public String getFileName() {
		if(getFile() == null) return getDefaultFileName();
		return getFile().getName();
	}

	public String getDefaultFileName() {
		return defaultFileName;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	@Override
	public boolean exists() {
		return getFile() != null && getFile().exists();
	}

	@Override
	public String toString() {
		return getFileName();
	}

	public BdvProject project() {
		return project;
	}

}
