package app.tools.topology;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.structure.GeometryMap;
import app.tools.structure.Mesh;

/** Edge.java
 * <br>
 * Represents a single segment of a crease edge. Stores indexes relative to an instance of the Geometry class.
 * 
 * @author Don McCurdy
 *
 */
public class Edge {
	/** Two vertices */ 																int[] vertices;
	/** The three or more materials adjacent to this edge.*/ 		ArrayList<Integer> materials;
	
	/** Initializes a crease edge, given the two endpoints of the edge. 
	 * @param pointIndexes
	 */
	public Edge (int [] pointIndexes) {
		if (pointIndexes.length != 2)
			throw new InvalidParameterException("ERROR: An edge must be initialized with the indexes of *two* points.");
		vertices = pointIndexes;
		materials = new ArrayList<Integer> (3);
	}
	
	/** Adds a material identifier to the list of materials adjacent to the edge.
	 * 
	 * @param material
	 */
	public void addMaterial(int material) {
		if (!materials.contains(material))
			materials.add(material); 
	}
	
	/** Returns a list of all materials adjacent to this edge. */
	public ArrayList<Integer> getMaterials() {
		return materials;
	}
	
	/** Returns an array with the (two) vertices on this edge. 
	 * 
	 * @return vertices
	 */
	public int[] getVertices() {
		return vertices;
	}
	
	/** Computes hashcode.
	 * Any two Edge objects with the same vertex indexes have the same hashcodes.
	 * @return hashcode
	 */
	public long longHashCode() {
		long buffer = Mesh.DEFAULT_BUFFER;	//Maximum allowable number of vertices in a mesh.
		int[] info = vertices.clone();
		Arrays.sort(info);
		long result = 0;
		for (int i = 0; i < info.length; ++i) 	{	//Ought to guarantee that no two polygons have the same hashcode...
			result *= buffer;
			result +=  info[i];							//...unless they have references to the same vertices.
		}
		return result;
	}
	
	@Override
	/** Returns true if and only if these edges reference the same indexes. */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		int[] v1 =this.vertices.clone();
		int[] v2 =other.vertices.clone();
		Arrays.sort(v1);
		Arrays.sort(v2);
		return 		(v1[0] == v2[0])
		&&	(v1[1] == v2[1]);
	}

	/** Subdivides this Edge object into two new Edge objects, adding 
	 * the necessary points to the GeometryMap/Geometry.
	 * @param geometry
	 * @param hash
	 * @return two_crease_edges
	 */
	public ArrayList<Edge> subdivide(Geometry g, GeometryMap hash) {
		ArrayList<Edge> newEdges = new ArrayList<Edge> ();
		
		int midpoint = hash.get(Vertex.midpoint(g.get(vertices[0]), g.get(vertices[1])));
		
		newEdges.add(new Edge(new int[] {vertices[0], midpoint }));
		newEdges.add(new Edge(new int[] {vertices[1], midpoint }));
		
		return newEdges;
	}
}
