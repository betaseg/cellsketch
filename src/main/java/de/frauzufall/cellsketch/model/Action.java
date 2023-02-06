package de.frauzufall.cellsketch.model;

import de.frauzufall.cellsketch.model.Item;

import java.util.List;

public interface Action extends Runnable {
	/**
	 * @return the list of items which need to exist in order to execute this action
	 */
	List<Item> getInputItems();

	/**
	 * @return the title of the action
	 */
	String getTitle();

	/**
	 * @return whether this action can be executed (by default, this will be true if all input items of this action exist)
	 */
	default boolean isExecutable() {
		for (Item item : getInputItems()) {
			if(!item.exists()) return false;
		}
		return true;
	}
}
