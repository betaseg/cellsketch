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

    void writeImage(String raw_name, RandomAccessibleInterval img, N5CosemMetadataParser metaWriter, N5CosemMetadata metadata, Double min, Double max) throws IOException;
    DataSelection getDataSelection(String name);
	Map<String, Object> projectData();
	N5LabelViewer viewer();

    void deleteFileItem(FileItem fileItem) throws IOException;
    void deleteItemGroup(BdvItemGroup bdvItemGroup) throws IOException;
}
