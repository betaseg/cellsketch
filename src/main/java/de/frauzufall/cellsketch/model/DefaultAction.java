package de.frauzufall.cellsketch.model;

import java.util.List;

public class DefaultAction implements Action {
	private List<Item> inputItems;
	private Runnable action;
	private String title;

	public DefaultAction(String title, List<Item> inputItems, Runnable action) {
		this.title = title;
		this.inputItems = inputItems;
		this.action = action;
	}

	@Override
	public List<Item> getInputItems() {
		return inputItems;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void run() {
		action.run();
	}
}
