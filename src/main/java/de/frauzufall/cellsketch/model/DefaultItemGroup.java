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

import java.io.IOException;
import java.util.*;

public class DefaultItemGroup implements ItemGroup {
	private final String name;
	private final List<Item> items;
	private List<Action> actions = new ArrayList<>();

	public DefaultItemGroup(String name, boolean deletable) {
		this.name = name;
		items = new ArrayList<>();
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
	public List<Item> getItems() {
		return items;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Action> getActions() {
		return actions;
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public void setColor(int color) {
	}

	@Override
	public void loadConfig() throws IOException {
		for (Item item : items) {
			item.loadConfig();
		}
	}

	@Override
	public void saveConfig() throws IOException {
		for (Item item : getItems()) {
			item.saveConfig();
		}
	}

	@Override
	public void display() {
		for(Item item : getItems()) item.display();
	}

	@Override
	public boolean load() throws IOException {
		for(Item item : getItems()) item.load();
		return true;
	}

	@Override
	public void unload() {
		for(Item item : getItems()) item.unload();
	}

	@Override
	public void delete() throws IOException {
		for(Item item : getItems()) item.delete();
	}
}
