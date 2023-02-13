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
