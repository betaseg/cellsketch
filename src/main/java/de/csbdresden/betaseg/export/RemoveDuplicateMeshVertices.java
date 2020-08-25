package de.csbdresden.betaseg.export;

import net.imagej.mesh.Mesh;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

import java.util.LinkedHashMap;
import java.util.Map;

public class RemoveDuplicateMeshVertices
{

	public static Mesh calculate(Mesh mesh, int precision) {
		Map<String, IndexedVertex> vertices = new LinkedHashMap<>();
		int[][] triangles = new int[(int) mesh.triangles().size()][3];

		int trianglesCount = 0;
		for (net.imagej.mesh.Triangle triangle : mesh.triangles()) {
			RealPoint p1 = new RealPoint(triangle.v0x(), triangle.v0y(), triangle.v0z());
			RealPoint p2 = new RealPoint(triangle.v1x(), triangle.v1y(), triangle.v1z());
			RealPoint p3 = new RealPoint(triangle.v2x(), triangle.v2y(), triangle.v2z());
			triangles[trianglesCount][0] = getVertex(vertices, p1, precision);
			triangles[trianglesCount][1] = getVertex(vertices, p2, precision);
			triangles[trianglesCount][2] = getVertex(vertices, p3, precision);
			trianglesCount++;
		}
		Mesh res = new BufferMesh(vertices.size(), triangles.length);
		vertices.values().forEach(vertex -> {
			res.vertices().add(vertex.point.getFloatPosition(0), vertex.point.getFloatPosition(1), vertex.point.getFloatPosition(2));
		});

		for (int[] triangle : triangles) {
			res.triangles().add(triangle[0], triangle[1], triangle[2]);
		}
		return res;
	}

	private static int getVertex(Map<String, IndexedVertex> vertices, RealPoint point, int precision) {
		String hash = getHash(point, precision);
		IndexedVertex vertex = vertices.get(hash);
		if(vertex == null) return makeVertex(vertices, hash, point);
		return vertex.index;
	}

	private static int makeVertex(Map<String, IndexedVertex> vertices, String hash, RealPoint point) {
		int index = vertices.size();
		IndexedVertex vertex = new IndexedVertex(point, index);
		vertices.put(hash, vertex);
		return index;
	}

	private static String getHash(RealPoint point, int precision) {
		int factor = (int) Math.pow(10, precision);
		return Math.round(point.getFloatPosition(0)*factor) + "-" + Math.round(point.getFloatPosition(1)*factor) + "-" + Math.round(point.getFloatPosition(2)*factor);
	}

	private static class IndexedVertex {

		int index;
		RealLocalizable point;
		IndexedVertex(RealLocalizable pos, int index) {
			this.point = pos;
			this.index = index;
		}

	}

}
