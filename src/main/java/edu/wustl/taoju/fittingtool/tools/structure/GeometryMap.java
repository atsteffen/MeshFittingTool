package app.tools.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import app.tools.Log;
import app.tools.math.Vertex;

/** GeometryMap.java
 * <br>
 * Extension of the HashMap class, used to make sure that no vertices are duplicated during subdivision.
 * 
 * @author Don McCurdy
 *
 */
public class GeometryMap extends HashMap<Vertex, Integer> {
	/** Default */ 	private static final long serialVersionUID = 5905633241203078789L;
	private Geometry geometry;

	@SuppressWarnings("unused")
	private GeometryMap(){Log.p("ERROR: ILLEGAL CONSTRUCTOR ON GEOMETRYMAP.");}

	
	/** Initializes the map, given an instance of the Geometry class.
	 * 
	 * @param geometry
	 */
	public GeometryMap(Geometry geometry) {
		this.geometry = new Geometry(new ArrayList<Vertex>());
		this.putAll(geometry.getPoints());
		this.geometry = geometry;
	}

	@Override
	/** Returns the index of the given Vertex, adding it if necessary.
	 * @param vertex
	 * @return index
	 */
	public Integer get (Object key) {
		if (!(key instanceof Vertex))
			return null;
		else if (! this.containsKey(key)) {
			geometry.add((Vertex) key);
			this.put((Vertex)key, geometry.size()-1);
		}
		return super.get(key);
	}

	/** Same as HashMap implementation, except that existing values are NOT overwritten. 
	 * @param vertices
	 */
	public void putAll (Collection<Vertex> vertices) {
		ArrayList<Vertex> newVerts = new ArrayList<Vertex> ();
		for (Vertex v : vertices) 
			if (!containsKey(v)) 
				newVerts.add(v);
		for (Vertex v : newVerts) {
			geometry.add(v);
			this.put(v, geometry.size() -1 );
		}
	}
}
