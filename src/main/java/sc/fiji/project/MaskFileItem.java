package sc.fiji.project;

import net.imagej.ops.OpService;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.project.export.PlyExporter;

import java.util.Collections;

public class MaskFileItem extends ImageFileItem<ByteType> {

	public MaskFileItem(BdvProject app, String name, String defaultFileName) {
		super(app, name, defaultFileName);
		this.max = 1.0;
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
