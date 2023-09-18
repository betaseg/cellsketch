package de.frauzufall.cellsketch.model;

import net.imglib2.type.numeric.integer.ByteType;
import de.frauzufall.cellsketch.BdvProject;

public class MaskFileItem extends ImageFileItem<ByteType> {
	public MaskFileItem(BdvProject app, String defaultFileName, boolean deletable) {
		super(app, defaultFileName, deletable);
		this.max = 1.;
	}
}
