package app.tools.structure;

import java.util.ArrayList;

import app.tools.math.Vertex;

/** Geometry.java
 * <br>
 * Stores the vertices for a mesh. The indexes stored by topological elements may be used as the 
 * parameter to call "get(int i)" in this class, returning the Vertex object for that element.
 * 
 * @author Don McCurdy
 * Modified by RBA
 *
 */
public class Geometry {
	/** Vertices */ private ArrayList<Vertex> points;
	
	/** Constructor taking a list of vertices. 
	 * 
	 * @param vertices
	 */
	public Geometry (ArrayList<Vertex> v) {
		points = v;
	}
	
	public Geometry (ArrayList<Vertex> v, ArrayList<ArrayList<Integer>> neighbors) {
		points = v;
		//this.neighbors = neighbors;
	}
	
	/** Add a vertex to the end of the geometry list
	 * 
	 * @param vertex
	 */
	public void add (Vertex v) {
		points.add(v);
	}
	
	/** Replace the vertex at the given index.
	 * 
	 * @param index
	 * @param vertex
	 */
	public void set (int i , Vertex v) {
		points.set(i,v);
	}
	
	/** Return the vertex at the given index.
	 * 
	 * @param index
	 * @return vertex
	 */
	public Vertex get (int i) {
		return points.get(i);
	}
	
	/** Returns the number of vertices in this geometry list.
	 * 
	 * @return numVertices
	 */
	public int size() {
		return points.size();
	}

	/** Returns a list of all the vertices in this geometry list.
	 * 
	 * @return arrayOfPoints
	 */
	public ArrayList<Vertex> getPoints() {
		return points;
	}
}
