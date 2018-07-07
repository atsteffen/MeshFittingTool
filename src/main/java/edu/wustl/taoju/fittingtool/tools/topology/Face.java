package app.tools.topology;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.structure.GeometryMap;
import app.tools.structure.Mesh;

/** Face.java
 * <br>
 * Represents a single crease face.
 * 
 * @author Don McCurdy
 *
 */
public class Face {
	/** Array containing the indexes of three vertices in the mesh's geometry */
	private int[] vertices;
	/** Array containing two integers: the material IDs of each of the two "partitions" owning this face. */
	private int[] materials;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTION
	//
	//******************************************************************************************************************************
	
	/** Instantiates the crease face, using the given material and setting the other material to "null_boundary"
	 * @param vertexIndexes
	 * @param material
	 */
	public Face (int[] vertexIndexes, int material) {
		vertices = vertexIndexes;
		materials = new int[] {	material 	,	Mesh.NULL_BOUNDARY 	};
	}

	/** Instantiates the crease faces, using the given vertices and materials.
	 * @param vertexIndexes
	 * @param materials
	 */
	public Face (int[] vertexIndexes, int[] materials) {
		vertices = vertexIndexes;
		this.materials = materials;
	}
	
	//******************************************************************************************************************************
	//
	//			ACCESSORS
	//
	//******************************************************************************************************************************
	
	/** Returns the front/back material for this face. 
	 * @param front_or_back
	 * @return material
	 */
	public int getMaterial (boolean isFront) {
		if (isFront) 
			return materials[0];
		else
			return materials[1];
	}
	/** Returns the color of the given side of the face
	 * @param front_or_back
	 * @return color
	 */
	public Color getColor (boolean isFront) {
		if (isFront)
			return Mesh.getColor(materials[0]);
		else
			return Mesh.getColor(materials[1]);
	}
	/** Returns the normal vector of the face. 
	 * @param geometry
	 * @param front_or_back
	 * @return normalVector
	 */
	public Vector getNormal(Geometry g, boolean isFront) {
		if (!isFront)
			return Vertex.normal(g.get(vertices[0]), g.get(vertices[1]), g.get(vertices[2]));
		else
			return Vertex.normal(g.get(vertices[0]), g.get(vertices[1]), g.get(vertices[2])).getInverse();
	}
	/** Returns the centroid of the face
	 * @param geometry
	 * @return centroid
	 */
	public Vertex getCentroid(Geometry g) {
		float x = 0;
		float y = 0;
		float z = 0;
		for (int i = 0; i < vertices.length; ++i) {
			x += g.get(vertices[i]).getX();
			y += g.get(vertices[i]).getY();
			z += g.get(vertices[i]).getZ();
		}
		x /= vertices.length;
		y /= vertices.length;
		z /= vertices.length;
		
		return new Vertex(x,y,z);
	}

	/** Returns a representation of this face, as a 2D array of floats. 
	 * For example, the z-coordinate of the first vertex would be 
	 * obtained by calling "getFaceData(g, front)[0][2]"
	 * @param geometry
	 * @param front_or_back
	 * @return array
	 */
	public float[][] getFaceData(Geometry g, boolean isFront) {
		float[][] result = new float[vertices.length][3];
		if (isFront)
			for (int i = 0; i < vertices.length; ++i)							//Counterclock-wise
				result[i] = g.get(vertices[i]).getXYZ();
		else
			for (int i = 0; i < vertices.length; ++i)							//Clock-wise
				result[vertices.length-1-i] = g.get(vertices[i]).getXYZ();
		
		return result;
	}
	/** Returns the edges of this face, as a 2D array of floats 
	 * 
	 * @return array
	 */
	public int[][] getEdges() {
		return new int[][] {
				new int[] { 	vertices[0], vertices[1]	},
				new int[] { 	vertices[1],	vertices[2]	},
				new int[] { 	vertices[2], vertices[0]	}
		};
	}
	/** Returns the vertices (by index) of this face 
	 * 
	 * @return array
	 */
	public int[] getPoints() {
		return vertices;
	}
		
	//******************************************************************************************************************************
	//
	//			MUTATORS
	//
	//******************************************************************************************************************************
	
	/** Sets the material for the given side of this crease face.
	 * @param material
	 * @param front_or_back
	 */
	public void setMaterial(int material, boolean isFront) {
		if (isFront)
			materials[0] = material;
		else
			materials[1] = material;
	}

	//******************************************************************************************************************************
	//
	//			SUBDIVISION
	//
	//******************************************************************************************************************************
	
	/** Subdivides this crease face into four new crease faces.
	 * Adds the new vertices to the geometry in the process.
	 * @param geometry
	 * @param hash
	 * @return list_of_crease_faces
	 */
	public ArrayList<Face> subdivide(Geometry g, GeometryMap hash) {
		ArrayList<Face> newFaces = new ArrayList<Face>();

		int a = hash.get(Vertex.midpoint(g.get(vertices[0]), g.get(vertices[1])));
		int b = hash.get(Vertex.midpoint(g.get(vertices[1]), g.get(vertices[2])));
		int c = hash.get(Vertex.midpoint(g.get(vertices[2]), g.get(vertices[0])));
		
		newFaces.add(new Face(new int[] {a,vertices[1], b}, materials));
		newFaces.add(new Face(new int[] {c, b, vertices[2]}, materials));
		newFaces.add(new Face(new int[] {vertices[0], a, c}, materials));
		newFaces.add(new Face(new int[] {a, b, c}, materials));

		
		return newFaces;
	}
	
	//******************************************************************************************************************************
	//
	//			HASHING AND EQUALITY
	//
	//******************************************************************************************************************************

	/** Computes hashcode.
	 * We need any two Face objects with the same vertices to be "equal."
	 * @return hashcode
	 */
	public long longHashCode() {
		long buffer = Mesh.DEFAULT_BUFFER;
		int[] info = vertices.clone();
		Arrays.sort(info);
		long result = 0;
		for (int i = 0; i < info.length; ++i) 	{	//Ought to guarantee that no two polygons have the same hashcode...
			result *= buffer;
			result +=  info[i];							//...unless they have references to the same vertices.
		}
		return result;
	}

	/** Returns true if and only if both Faces reference the same indexes. 
	 * @param obj
	 * @return isEqual
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Face other = (Face) obj;
		
		int[] v1 = this.vertices.clone();
		int[] v2 = other.vertices.clone();
		
		Arrays.sort(v1);
		Arrays.sort(v2);
		
		return 		(v1[0] == v2[0])
		&&	(v1[1] == v2[1])
		&&	(v1[2] == v2[2]);
	}

	public boolean isSurface() {
		if (materials[0] == Mesh.NULL_BOUNDARY) return true;
		if (materials[1] == Mesh.NULL_BOUNDARY) return true;
		return false;
	}
}
