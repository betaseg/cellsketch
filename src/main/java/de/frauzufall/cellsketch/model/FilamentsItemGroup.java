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

import de.frauzufall.cellsketch.BdvProject;

public class FilamentsItemGroup extends LabelMapItemGroup {
	private final FileItem yamlItem;
	private final LabelTagItem tagLength;
	private final LabelTagItem tagTortuosity;

	public FilamentsItemGroup(BdvProject project, String name, String yamlPath, String path) {
		super(project, name, path);
		this.yamlItem = new FileItem(project, yamlPath, true);
		this.tagLength = addLabelIfNotExists(FilamentsTable.getLengthColumnName(), Double.class, true);
		this.tagTortuosity = addLabelIfNotExists(FilamentsTable.getTortuosityColumnName(), Double.class, true);
	}

	public FileItem getFilamentsYamlItem() {
		return yamlItem;
	}

	public LabelTagItem getTagLength() {
		return tagLength;
	}

	public LabelTagItem getTagTortuosity() {
		return tagTortuosity;
	}
}
