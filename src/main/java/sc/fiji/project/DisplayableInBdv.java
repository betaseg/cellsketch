package sc.fiji.project;

import bdv.util.BdvSource;

import java.util.List;

public interface DisplayableInBdv extends Item {

	void addToBdv();
	void updateBdvColor();

	void setVisible(boolean visible);
	boolean isVisible();

	List<BdvSource> getSources();

	default void removeFromBdv() {
		if(getSources() == null) return;
		getSources().forEach(BdvSource::removeFromBdv);
		getSources().clear();
		setVisible(false);
	}

	default void displayIfExists() {
		if(exists()) addToBdv();
	}

}
