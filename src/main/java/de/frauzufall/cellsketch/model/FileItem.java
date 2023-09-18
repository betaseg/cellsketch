/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
