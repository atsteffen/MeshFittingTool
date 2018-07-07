package app.tools.topology;

import java.util.ArrayList;


/** Topology.java
 * <br>
 * Stores ArrayLists containing the different features of a Mesh. 
 * Includes crease points, crease edges, crease faces, and all polyhedra.
 * 
 * @author Don McCurdy
 *
 */
public class Topology {
	private ArrayList<Integer> 			points;
	private ArrayList<Edge> 				edges;
	private ArrayList<Face>				faces;
	private ArrayList<Polyhedron>	polyhedra;
	
	/** Initializes mesh topology, given arraylists for each type of element.
	 * @param vertices
	 * @param edges
	 * @param faces
	 * @param polyhedra
	 */
	public Topology (ArrayList<Integer> v, ArrayList<Edge> e, ArrayList<Face> f, ArrayList<Polyhedron> p) {
		points = v;
		edges = e;
		faces = f;
		polyhedra = p;
	}

	/** Returns a list of all polyhedra in the mesh.
	 * 
	 * @return polyhedra
	 */
	public ArrayList<Polyhedron> getPolyhedra() {
		return polyhedra;
	}
	
	/** Returns a list of all crease points in the mesh.
	 * 
	 * @return crease_points
	 */
	public ArrayList<Integer> getPoints() {
		return points;
	}
	
	/** Returns a list of all crease edges in the mesh.
	 * 
	 * @return crease_edges
	 */
	public ArrayList<Edge> getEdges() {
		return edges;
	}
	
	/** Returns a list of all crease faces in the mesh.
	 * 
	 * @return crease_faces
	 */
	public ArrayList<Face> getFaces() {
		return faces;
	}
	
	
}
