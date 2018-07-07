package app.tools.topology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

import app.tools.Log;
import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.structure.GeometryMap;

/** Octahedron.java
 * <br>
 * Represents a single Octahedron object. Used to provide access to
 * face data, subdivision, and adjacency data for generating cross
 * sections.
 * 
 * @author Don McCurdy
 *
 */
public class Octahedron extends Polyhedron {

	/** Initializes an Octahedron, given a material and vertices. 
	 * @param verts
	 * @param material
	 */
	public Octahedron (int[] verts, int material) {
		super (verts, material);
	}

	/** Returns the indexes of the faces of this Octahedron. For example,
	 * the first index of the second face would be referenced as:
	 * tri[1][0]
	 * An 8xN array should be provided, where N doesn't matter.
	 * @param array_to_fill
	 */
	public void getFaceIndexes(int[][] tri) {

		tri[0]=		new int [] {vertices[0], vertices[1], vertices[2] };	//NOTE: This ENTIRELY depends on the ...
		tri[1]=		new int [] {vertices[0], vertices[3], vertices[1] };	//...expected order of the vertices. 
		tri[2]=		new int [] {vertices[1], vertices[5], vertices[2] };	
		tri[3]=		new int [] {vertices[2], vertices[4], vertices[0] }; 

		tri[4]=		new int [] {vertices[3], vertices[4], vertices[5] };
		tri[5]=		new int [] {vertices[3], vertices[0], vertices[4] };
		tri[6]=		new int [] {vertices[4], vertices[2], vertices[5] };
		tri[7]=		new int [] {vertices[5], vertices[1], vertices[3] };

	}
	
	public ArrayList<Vertex> getEdges (Geometry g) {
		ArrayList<Vertex> edges = new ArrayList<Vertex>();
		
		edges.add(g.get(vertices[0]));		////////
		edges.add(g.get(vertices[1]));

		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[2]));
		
		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[3]));
		
		edges.add(g.get(vertices[0]));
		edges.add(g.get(vertices[4]));
		
		
		edges.add(g.get(vertices[1]));		////////
		edges.add(g.get(vertices[2]));
		
		edges.add(g.get(vertices[1]));
		edges.add(g.get(vertices[3]));
		
		edges.add(g.get(vertices[1]));
		edges.add(g.get(vertices[5]));

		edges.add(g.get(vertices[2]));
		edges.add(g.get(vertices[4]));
		
		
		edges.add(g.get(vertices[2]));		////////
		edges.add(g.get(vertices[5]));

		edges.add(g.get(vertices[3]));
		edges.add(g.get(vertices[4]));
		
		edges.add(g.get(vertices[3]));
		edges.add(g.get(vertices[5]));

		edges.add(g.get(vertices[4]));
		edges.add(g.get(vertices[5]));
		
		return edges;
	}

	/** Returns the number of faces in this Polyhedron (8)
	 * @return numFaces
	 */
	public int getNumFaces() {
		return 8;
	}

	/** Subdivides this octahedron into 6 octahedra and 8 tetrahedra.
	 * @param geometry
	 * @param geometryMap
	 */
	protected ArrayList<Polyhedron> subdivideHelper(Geometry g, GeometryMap hash) {
		ArrayList<Polyhedron> newPolys = new ArrayList<Polyhedron> ();

		/** Original vertices */ int[] v1 = new int[6]; 
		/** Midpoint vertices*/ int[] v2 = new int[13];

		v1[0] = vertices[0];	// Face:	 	0 1 3 5
		v1[1] = vertices[1];	//				0 1 2 7
		v1[2] = vertices[2];	//				0 2 3 6
		v1[3] = vertices[3];	//				1 4 5 7
		v1[4] = vertices[4];	//				3 4 5 6
		v1[5] = vertices[5];	// 			2 4 6 7

		v2[0] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[1])));		// Face:		0 1
		v2[1] = hash.get(Vertex.midpoint(g.get(v1[1]),g.get(v1[2])));		// 			0 2
		v2[2] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[2])));		// 			0 3
		v2[3] = hash.get(Vertex.midpoint(g.get(v1[3]),g.get(v1[4])));		// 			4 5

		v2[4] = hash.get(Vertex.midpoint(g.get(v1[4]),g.get(v1[5])));		// 			4 6
		v2[5] = hash.get(Vertex.midpoint(g.get(v1[3]),g.get(v1[5])));		// 			4 7
		v2[6] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[3])));		// 			1 5
		v2[7] = hash.get(Vertex.midpoint(g.get(v1[1]),g.get(v1[3])));		// 			1 7

		v2[8] = hash.get(Vertex.midpoint(g.get(v1[1]),g.get(v1[5])));		// 			2 7
		v2[9] = hash.get(Vertex.midpoint(g.get(v1[2]),g.get(v1[5])));		// 			2 6
		v2[10] = hash.get(Vertex.midpoint(g.get(v1[2]),g.get(v1[4])));	// 			3 6	
		v2[11] = hash.get(Vertex.midpoint(g.get(v1[0]),g.get(v1[4])));	// 			3 5

		v2[12] = hash.get(this.getCentroid(g));									//				NA

		HashSet<Short> map = new HashSet<Short>();
		for (short s : creaseFaces)
			map.add(s);

		//																0135		0 1		0 3			1 5			3 5			
		Octahedron o1 = new Octahedron(new int[] { 	v1[0], 	v2[0], 	v2[2], 		v2[6], 		v2[11], 		v2[12] }, this.material);
		newPolys.add(o1);
		if (map.contains((short)0)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)1)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
		if (map.contains((short)3)) newPolys.get(newPolys.size()-1).addCreaseFace(3);
		if (map.contains((short)5)) newPolys.get(newPolys.size()-1).addCreaseFace(5);

		//																0127		0 2		0 1			2 7			1 7			
		Octahedron o2 = new Octahedron(new int[] { 	v1[1], 	v2[1], 	v2[0], 		v2[8], 		v2[7], 		v2[12] }, this.material);
		newPolys.add(o2);
		if (map.contains((short)0)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)1)) newPolys.get(newPolys.size()-1).addCreaseFace(3);
		if (map.contains((short)2)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
		if (map.contains((short)7)) newPolys.get(newPolys.size()-1).addCreaseFace(5);

		//																0236		0 3		0 2			3 6			2 6			
		Octahedron o3 = new Octahedron(new int[] { 	v1[2], 	v2[2], 	v2[1], 		v2[10], 		v2[9], 		v2[12] }, this.material);
		newPolys.add(o3);
		if (map.contains((short)0)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)2)) newPolys.get(newPolys.size()-1).addCreaseFace(3);
		if (map.contains((short)3)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
		if (map.contains((short)6)) newPolys.get(newPolys.size()-1).addCreaseFace(5);

		//																1457		4 5		4 7			1 5			1 7			
		Octahedron o4 = new Octahedron(new int[] { 	v1[3], 	v2[3], 	v2[5], 		v2[6], 		v2[7], 		v2[12] }, this.material);
		newPolys.add(o4);
		if (map.contains((short)1)) newPolys.get(newPolys.size()-1).addCreaseFace(5);
		if (map.contains((short)4)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)5)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
		if (map.contains((short)7)) newPolys.get(newPolys.size()-1).addCreaseFace(3);

		//																3456		4 6		4 5			3 6			3 5			
		Octahedron o5 = new Octahedron(new int[] { 	v1[4], 	v2[4], 	v2[3], 		v2[10], 		v2[11], 		v2[12] }, this.material);
		newPolys.add(o5);
		if (map.contains((short)3)) newPolys.get(newPolys.size()-1).addCreaseFace(5);
		if (map.contains((short)4)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)5)) newPolys.get(newPolys.size()-1).addCreaseFace(3);
		if (map.contains((short)6)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
	
		//																2467		4 7		4 6			2 7			2 6			
		Octahedron o6 = new Octahedron(new int[] { 	v1[5], 	v2[5], 	v2[4], 		v2[8], 		v2[9], 		v2[12] }, this.material);
		newPolys.add(o6);
		if (map.contains((short)2)) newPolys.get(newPolys.size()-1).addCreaseFace(5);
		if (map.contains((short)4)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		if (map.contains((short)6)) newPolys.get(newPolys.size()-1).addCreaseFace(3);
		if (map.contains((short)7)) newPolys.get(newPolys.size()-1).addCreaseFace(1);
	
		//																0 1		0 2		0 3							
		Tetrahedron t1 = new Tetrahedron(new int[] { 	v2[0], 	v2[1], 	v2[2], 		v2[12] }, this.material);
		newPolys.add(t1);
		if (map.contains((short)0)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																0 1		1 5		1 7							
		Tetrahedron t2 = new Tetrahedron(new int[] { 	v2[0], 	v2[6], 	v2[7], 		v2[12] }, this.material);
		newPolys.add(t2);
		if (map.contains((short)1)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																0 2		2 7		2 6							
		Tetrahedron t3 = new Tetrahedron(new int[] { 	v2[1], 	v2[8], 	v2[9], 		v2[12] }, this.material);
		newPolys.add(t3);
		if (map.contains((short)2)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																0 3		3 6		3 5							
		Tetrahedron t4 = new Tetrahedron(new int[] { 	v2[2], 	v2[10], 	v2[11], 		v2[12] }, this.material);
		newPolys.add(t4);
		if (map.contains((short)3)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																4 5		4 6		4 7						
		Tetrahedron t5 = new Tetrahedron(new int[] { v2[3], 	v2[4], 	v2[5], 	v2[12] }, this.material);
		newPolys.add(t5);
		if (map.contains((short)4)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																4 5		1 5		3 5							
		Tetrahedron t6 = new Tetrahedron(new int[] { v2[3], 	v2[6], 	v2[11], 	v2[12] }, this.material);
		newPolys.add(t6);
		if (map.contains((short)5)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																4 6		3 6		2 6							
		Tetrahedron t7 = new Tetrahedron(new int[] { v2[4], 	v2[10], 	v2[9], 	v2[12] }, this.material);
		newPolys.add(t7);
		if (map.contains((short)6)) newPolys.get(newPolys.size()-1).addCreaseFace(0);

		//																4 7		2 7		1 7							
		Tetrahedron t8 = new Tetrahedron(new int[] { v2[5], 	v2[8], 	v2[7], 	v2[12] }, this.material);
		newPolys.add(t8);
		if (map.contains((short)7)) newPolys.get(newPolys.size()-1).addCreaseFace(0);
		
		return newPolys;
	}

	/** Static method: given the local (between 0 and 5) indexes of two points along an edge, returns
	 * local (between 0 and 7) indexes of the two faces on this Octahedron that are adjacent to the given edge.
	 * Used for generating cross-sections, by traversing the outer surface on the Octahedron.
	 * @param vertexA
	 * @param vertexB
	 * @return twoAdjacentFaces
	 */
	public static short[] getAdjacentFaces(short a, short b) {
		a++; b++;	//Just to keep things pretty.

		if (a > b) {
			short temp = b;
			b = a;
			a = temp;
		}
		short[] adjacentFaces = new short[]{-99};

		switch (a) {
		case 1 :
			if 			(b == 2) 	adjacentFaces = new short[] 		{1,2};
			else 	if 	(b == 3) 	adjacentFaces = new short[] 		{1,4};
			else 	if 	(b == 4)	adjacentFaces = new short[] 		{2,6};
			else 	if 	(b == 5) 	adjacentFaces = new short[] 		{4,6};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 2 :
			if 			(b == 3) 	adjacentFaces = new short[] 		{1,3};
			else 	if 	(b == 4) 	adjacentFaces = new short[] 		{2,8};
			else 	if 	(b == 6)	adjacentFaces = new short[] 		{3,8};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 3 :
			if 			(b == 5) 	adjacentFaces = new short[] 		{4,7};
			else 	if 	(b == 6) 	adjacentFaces = new short[] 		{3,7};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 4 :
			if 			(b == 5) 	adjacentFaces = new short[] 		{5,6};
			else 	if 	(b == 6) 	adjacentFaces = new short[] 		{5,8};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		case 5 :
			if 			(b == 6) 	adjacentFaces = new short[] 		{5,7};
			else throw new NoSuchElementException("Invalid point B, " + b + ", not on edge. (found point"+a+")");
			break;
		default :
			throw new NoSuchElementException("Invalid point A, " + a + ", not on edge.");
		}
		if (adjacentFaces[0] == -99) Log.p("Ummm, no adjacent faces found?");

		return new short[] {--adjacentFaces[0] , --adjacentFaces[1]};
	}

}


