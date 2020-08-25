package sc.fiji.project;

import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import sc.fiji.project.command.ImportFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FileItem extends AbstractItem {

	private final String fileType;
	private File file;
	private final BdvProject project;

	public FileItem(BdvProject project, String name, String fileType) {
		super(name);
		this.fileType = fileType;
		this.project = project;
		if(project.isEditable()) {
			getActions().add(new DefaultAction(
					"Import from file",
					Collections.emptyList(),
					this::importAsFile
			));
		}
	}

	protected boolean load() throws IOException { return false; }
	public boolean save() throws IOException { return false; }

	protected boolean importAsFile() {
		try {
			CommandModule res = null;
			try {
				res = project().context().getService(CommandService.class).run(ImportFile.class, true).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			if(res == null || res.isCanceled()) {
				return false;
			}
			File file = (File) res.getInput("file");
			boolean copy = (boolean) res.getInput("copy");

			if(file == null) return false;
			if(copy) {
				File copiedFile = new File(project.getProjectDir(), getDefaultFileName());
				Files.copy(file.toPath(), copiedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				this.file = copiedFile;
			} else {
				this.file = file;
			}
			project.save();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		project().updateUI();
		return true;
	}

	public String getFileType() {
		return fileType;
	}

	@Override
	public void loadConfigFrom(Map<String, Object> data) {
//		System.out.println(getName() + " " + data);
		if(data.containsKey("path")) file = new File(project().getProjectDir(), (String) data.get("path"));
	}

	@Override
	public void saveConfigTo(Map<String, Object> data) {
		if(file != null) {
			Path pathBase = project().getProjectDir().toPath();
			Path pathAbsolute = file.toPath();
			Path pathRelative = pathBase.normalize().relativize(pathAbsolute);
			data.put("path", pathRelative.toString());
		}
	}

	public String getFileName() {
		if(getFile() == null) return getDefaultFileName();
		return getFile().getName();
	}

	public String getDefaultFileName() {
		return nameToFileName() + "." + getFileType();
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
