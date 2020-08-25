package de.csbdresden.betaseg.export;

import net.imagej.mesh.Triangle;
import net.imagej.mesh.Vertex;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.service.Service;
import sc.fiji.project.FileItem;
import sc.fiji.project.ImageFileItem;
import sc.fiji.project.LabelMapFileItem;
import sc.fiji.project.MaskFileItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlyExporter extends Thread {
	private final FileItem item;
	private final OpService opService;

	private int meshCount;
	private int faceCount;
	private int vertexCount;

	public PlyExporter(OpService opService, MaskFileItem item) {
		this.opService = opService;
		this.item = item;
	}

	public <S extends Service> PlyExporter(OpService opService, LabelMapFileItem item) {
		this.opService = opService;
		this.item = item;
	}

	private void reset() {
		meshCount = 0;
		faceCount = 0;
		vertexCount = 0;
	}

	@Override
	public void run() {
		export();
	}

	public void export() {
		File exportDir = new File(item.project().getProjectDir(), "export");
		export(exportDir, item.nameToFileName(), ((ImageFileItem)item).getImage());
	}

	public void export(File exportDir, String name, RandomAccessibleInterval image) {
		try {
			exportDir = new File(exportDir, name);
			File colorFile = new File(exportDir, "colors.txt");
			exportDir.mkdirs();
			for (File file : exportDir.listFiles()) file.delete();

			ImgFactory<FloatType> floatFactory = new ArrayImgFactory<>(new FloatType());

			ExecutorService pool = Executors.newFixedThreadPool(4);
			CompletionService<MyMesh> ecs = new ExecutorCompletionService<>(pool);
			MaskMeshCreator creator = new MaskMeshCreator(opService, image, floatFactory);
			ecs.submit(creator);
			try (PrintWriter colorWriter = new PrintWriter(new FileOutputStream(colorFile, true))) {
				MyMesh mesh = ecs.take().get();
				if (mesh != null) writeMesh(mesh, exportDir, colorWriter, 0.01f);
				colorWriter.flush();
			}

		} catch (InterruptedException | IOException | ExecutionException ex) {
			ex.printStackTrace();
		}
	}

	private void appendFile(File file, PrintWriter writer) {
		Scanner sc = null;
		try {
			String lineSeparator = System.getProperty("line.separator");
			sc = new Scanner(file);
			while(sc.hasNextLine()) {
				String s = sc.nextLine()+lineSeparator;
				writer.write(s);
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if(sc != null) {
				sc.close();
			}
			if(writer != null) {
				writer.flush();
			}
		}
	}

	synchronized void writeMesh(MyMesh mesh, File exportDir, PrintWriter colorWriter, float scale) throws IOException {
		reset();
		File plyFile = new File(exportDir, mesh.label + ".ply");
//			File mtlFile = new File(item.app().getProjectDir(), item.getFileName() + ".mtl");
		File tmpFacesFile = Files.createTempFile("faces", ".ply").toFile();
//			File tmpNormalsFile = Files.createTempFile("normals", ".ply").toFile();
		File tmpVerticesFile = Files.createTempFile("vertices", ".ply").toFile();
		plyFile.delete();
		plyFile.createNewFile();
//			mtlFile.delete();
//			mtlFile.createNewFile();
		try (PrintWriter plyWriter = new PrintWriter(new FileOutputStream(plyFile, true));
		     PrintWriter facesWriter = new PrintWriter(new FileOutputStream(tmpFacesFile, true));
		     PrintWriter verticesWriter = new PrintWriter(new FileOutputStream(tmpVerticesFile, true))) {

//				this.materialWriter = materialWriter;

			plyWriter.append("ply\n");
			plyWriter.append("format ascii 1.0\n");
			plyWriter.append("comment ").append(item.getName()).append("\n");

			StringBuilder faces = new StringBuilder();
			StringBuilder vertices = new StringBuilder();
	//		int count = indexMappingStart;

			for (Vertex vertex : mesh.mesh.vertices()) {
				appendVertex(vertices, vertex, scale);
				vertexCount++;
			}
			for (Triangle triangle : mesh.mesh.triangles()) {
				long i1 = triangle.vertex0();
				long i2 = triangle.vertex1();
				long i3 = triangle.vertex2();
				if(i1 == i2 || i2 == i3 || i1 == i3) continue;
				faces.append("3 ").append(i1)
						.append(" ").append(i2)
						.append(" ").append(i3).append("\n");
				faceCount++;
			}

			facesWriter.print(faces);
			verticesWriter.print(vertices);
			facesWriter.flush();
			verticesWriter.flush();

			plyWriter.append("element vertex ").append(String.valueOf(vertexCount)).append("\n");
			plyWriter.append("property float x\n");
			plyWriter.append("property float y\n");
			plyWriter.append("property float z\n");
			plyWriter.append("element face ").append(String.valueOf(faceCount)).append("\n");
			plyWriter.append("property list uchar int vertex_index\n");
			plyWriter.append("end_header\n");
			plyWriter.flush();
			appendFile(tmpVerticesFile, plyWriter);
			plyWriter.flush();
			appendFile(tmpFacesFile, plyWriter);

			colorWriter.append(mesh.label.toString()).append(" ")
					.append(String.valueOf(ARGBType.red(mesh.color))).append(" ")
					.append(String.valueOf(ARGBType.green(mesh.color))).append(" ")
					.append(String.valueOf(ARGBType.blue(mesh.color))).append(" ")
					.append(String.valueOf(255)).append("\n");

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Exported mask");
	}

	private void appendColor(PrintWriter plyWriter, int color) {
		int ambientRed = ARGBType.red(color);
		int ambientGreen = ARGBType.green(color);
		int ambientBlue = ARGBType.blue(color);
//		float ambientCoeff = 1.0f;
//		int diffuseRed = ambientRed;
//		int diffuseGreen = ambientGreen;
//		int diffuseBlue = ambientBlue;
//		float diffuseCoeff = 1.0f;
//		int specularRed = 112;
//		int specularGreen = 112;
//		int specularBlue = 112;
//		float specularCoeff = 1.0f;
//		float specularPower = 18.00f;
//		plyWriter.append(String.valueOf(ambientRed)).append(" ")
//				.append(String.valueOf(ambientGreen)).append(" ")
//				.append(String.valueOf(ambientBlue)).append(" ")
//				.append(String.valueOf(ambientCoeff)).append(" ")
//				.append(String.valueOf(diffuseRed)).append(" ")
//				.append(String.valueOf(diffuseGreen)).append(" ")
//				.append(String.valueOf(diffuseBlue)).append(" ")
//				.append(String.valueOf(diffuseCoeff)).append(" ")
//				.append(String.valueOf(specularRed)).append(" ")
//				.append(String.valueOf(specularGreen)).append(" ")
//				.append(String.valueOf(specularBlue)).append(" ")
//				.append(String.valueOf(specularCoeff)).append(" ")
//				.append(String.valueOf(specularPower)).append("\n");

		plyWriter.append(String.valueOf(ambientRed)).append(" ")
				.append(String.valueOf(ambientGreen)).append(" ")
				.append(String.valueOf(ambientBlue)).append("\n");
	}

	private synchronized void appendVertex(StringBuilder vertices, RealLocalizable point, float scale) {
		vertices.append(point.getFloatPosition(0)*scale).append(" ")
				.append(point.getFloatPosition(1)*scale).append(" ")
				.append(point.getFloatPosition(2)*scale).append("\n");
	}
}
