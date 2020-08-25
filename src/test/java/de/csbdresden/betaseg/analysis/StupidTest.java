package de.csbdresden.betaseg.analysis;

import net.imagej.ImageJ;

import java.io.IOException;

public class StupidTest {

	public static void main(String...args) throws IOException {
		ImageJ ij = new ImageJ();
		Object img = ij.io().open("https://samples.fiji.sc/blobs.png");
		ij.ui().show(img);
	}

}
