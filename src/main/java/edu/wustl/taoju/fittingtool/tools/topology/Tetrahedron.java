package app.tools.topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

import app.tools.Log;
import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.structure.GeometryMap;

/** Tetrahedron.java
 * <br>
 * Represents a single Tetrahedron object, providing access to
 * face data, subdivision, and adjacency helper methods 
 * used to generate cross sections. 
 * 
 * @author Don McCurdy
 *
 */
public class Tetrahedron extends Polyhedron {
	/** Initializes a Tetrahedron, given a material and vertices
	 * @param verts
	 * @param material
	 */
	public Tetrahedron (int[] verts, int material) {
		super (verts, material);
	}

	/** Returns the indexes of the faces of this Tetrahedron. For example,
	 * the first index of the second face would be referenced as:
	 * tri[1][0]
	 * A 4xN array should be provided. (N doesn't matter).
	 * @param array_to_fill
	 */
	public void getFaceIndexes(int[][] tri) {

		tri[0]=		new int[] {vertices[0], vertices[1], vertices[2]};
		tri[1]=		new int[] {vertices[0], vertices[2], vertices[3]};
		tri[2]=		new int[] {vertices[0], vertices[3], vertices[1]};
		tri[3]=		new int[] {vertices[1], vertices[3], vertices[2]};

	}
	
	public ArrayList<Vertex> getEdges (Geometry g) {
		ArrayList<Vertex> edges = new ArrayList<Vertex>();
		
		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[1]));
		
		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[2]));
		
		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[3]));
		
		edges.add(g.get(vertices[1]));
		edges.add(g.get(vertices[2]));
		
		edges.add(g.get(vertices[1]));
		edges.add(g.get(vertices[3]));
		
		edges.add(g.get(vertices[2]));
		edges.add(g.get(vertices[3]));
		
		return edges;
	}

	/** Returns the number of faces on this Polyhedron. (4)
	 * @return numFaces
	 */
	public int getNumFaces() {
		return 4;
	}

	/** Subdivides this tetrahedron into 4 tetrahedra and 1 octahedron.
	 * @param geometry
	 * @param geometryMap
	 * @return newPolyhedra
	 */
	protected ArrayList<Polyhedron> subdivideHelper (Geometry g, GeometryMap hash) {
		ArrayList<Polyhedron> newPolys = new ArrayList<Polyhedron> ();

		/** Original vertices */ int[] v1 = new int[4]; 
		/** Midpoint vertices*/ int[] v2 = new int[6];

		v1[0] = vertices[0];	// 0 1 2 	//faces
		v1[1] = vertices[1];	// 0 2 3
		v1[2] = vertices[2];	// 0 1 3
		v1[3] = vertices[3];	// 1 2 3

		v2[0] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[1])));		// 0 or 2		//faces
		v2[1] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[2])));		// 0 or 1
		v2[2] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[3])));		// 1 or 2
		v2[3] = hash.get(Vertex.midpoint(g.get(v1[1]),g.get(v1[2])));	 	// 0 or 3
		v2[4] = hash.get(Vertex.midpoint(g.get(v1[1]),g.get(v1[3])));		// 2 or 3
		v2[5] = hash.get(Vertex.midpoint(g.get(v1[2]),g.get(v1[3])));		// 1 or 3

		HashSet<Short> map = new HashSet<Short>();
		for (short s : creaseFaces)
			map.add(s);

		//																		012		0 2		0 1		1 2
		Tetrahedron t1 = new Tetrahedron(new int[] { 	v1[0], 	v2[0], 	v2[1], 	v2[2] }, this.material);
		if (map.contains((short)0)) t1.addCreaseFace(0);
		if (map.contains((short)1)) t1.addCreaseFace(1);
		if (map.contains((short)2)) t1.addCreaseFace(2);

		//																		023		0 3		0 2		2 3
		Tetrahedron t2 = new Tetrahedron(new int[] { 	v1[1], 	v2[3], 	v2[0], 	v2[4] }, this.material);
		if (map.contains((short)0)) t2.addCreaseFace(0);
		if (map.contains((short)2)) t2.addCreaseFace(1);
		if (map.contains((short)3)) t2.addCreaseFace(2);

		//																		013		0 1		0 3		1 3
		Tetrahedron t3 = new Tetrahedron(new int[] { 	v1[2], 	v2[1], 	v2[3], 	v2[5] }, this.material);
		if (map.contains((short)0)) t3.addCreaseFace(0);
		if (map.contains((short)1)) t3.addCreaseFace(2);
		if (map.contains((short)3)) t3.addCreaseFace(1);

		//																		123		1 2		1 3		2 3
		Tetrahedron t4 = new Tetrahedron(new int[] { 	v1[3], 	v2[2], 	v2[5], 	v2[4] }, this.material);
		if (map.contains((short)1)) t4.addCreaseFace(0);
		if (map.contains((short)2)) t4.addCreaseFace(2);
		if (map.contains((short)3)) t4.addCreaseFace(1);

		//																		0 2		0 1		1 2		0 3		2 3		1 3
		Octahedron o1 = new Octahedron(new int[] { 		v2[0], 	v2[1], 	v2[2], 	v2[3], 	v2[4], 	v2[5] }, this.material);
		if (map.contains((short)0)) o1.addCreaseFace(1);
		if (map.contains((short)1)) o1.addCreaseFace(2);
		if (map.contains((short)2)) o1.addCreaseFace(3);
		if (map.contains((short)3)) o1.addCreaseFace(4);

		newPolys.add(t1); 
		newPolys.add(t2); 
		newPolys.add(t3); 
		newPolys.add(t4);	
		newPolys.add(o1);
	
		return newPolys;
	}

	/** Static method: given the local (between 0 and 3) indexes of two vertices of this
	 * Tetrahedron, returns local (again, between 0 and 3) indexes of faces on this Tetrahedron
	 * that are adjacent to this edge. (Used for generating cross-sections by traversal)
	 * @param vertexA
	 * @param vertexB
	 * @return twoAdjacentFaces
	 */
	public static short[] getAdjacentFaces(short a, short b) {	
		a++; b++;	//Keeps things pretty.

		if (a > b) {
			short temp = b;
			b = a;
			a = temp;
		}
		short[] adjacentFaces = new short[]{-99};

		switch (a) {
		case 1 :
			if 			(b == 2) 	adjacentFaces = new short[] 		{1,3};
			else 	if 	(b == 3) 	adjacentFaces = new short[] 		{1,2};
			else 	if 	(b == 4)	adjacentFaces = new short[] 		{2,3};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 2 :
			if 			(b == 3) 	adjacentFaces = new short[] 		{1,4};
			else 	if 	(b == 4) 	adjacentFaces = new short[] 		{3,4};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 3 :
			if 			(b == 4) 	adjacentFaces = new short[] 		{2,4};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		default :
			throw new NoSuchElementException("Invalid point A, " + a + ", not on edge.");
		}
		if (adjacentFaces[0] == -99) Log.p("Ummm, no adjacent faces found?");

		return new short[] {--adjacentFaces[0] , --adjacentFaces[1]};
	}
}



