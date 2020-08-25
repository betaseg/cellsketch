package sc.fiji.project;

import de.csbdresden.betaseg.export.PlyExporter;
import net.imagej.ops.OpService;
import net.imglib2.type.numeric.integer.ByteType;

import java.util.Collections;

public class MaskFileItem extends ImageFileItem<ByteType> {

	public MaskFileItem(BdvProject app, String name) {
		super(app, name);
		if(app.isEditable()) {
			PlyExporter plyExporter = new PlyExporter(project().context().service(OpService.class), this);
			getActions().add(new DefaultAction(
					"Export as PLY",
					Collections.singletonList(this),
					plyExporter::export
			));
		}
	}

}
