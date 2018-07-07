package app.tools.cross_sections;

import java.util.ArrayList;
import java.util.HashSet;

import app.RegistrationTool;
import app.tools.Log;
import app.tools.cross_sections.SimpleGeometry.*;
import app.tools.math.*;
import app.tools.structure.Geometry;
import app.tools.topology.*;

/** CrossSectionMaker.java
 * <br>
 * Once initialized with a CuttingTool object (providing information about the model and cutting plane),
 * the primary method ( getCrossSection(...) ) calculates and returns the polygonal cross-section of
 * a given polyhedron.
 * 
 * @author Don McCurdy
 * @date August 5, 2010
 *
 */
public class CrossSectionMaker {
//	/** Represents the cutting plane */ 		CuttingTool cutter;
	/** Stores mesh's vertex information */ 	Geometry g;

	/** A point lying on the cutting plane */ 	Vertex p;
	/** A vector normal to the cutting plane*/ 	Vector n;
	RegistrationTool app;

	/** Instantiates a new CrossSectionMaker.
	 * The CuttingTool must be provided, for access to the Mesh and cutting plane data.
	 * @param cuttingtool
	 */
	public CrossSectionMaker(RegistrationTool app) {
//		this.cutter = cutter; 
		this.g = app.getMesh().getGeometry(); //cutter.getMesh().getGeometry();
	}

	//******************************************************************************************************************************
	//
	//			CROSS-SECTIONING ROUTINE
	//
	//******************************************************************************************************************************

	/** Creates a polygonal cross-section of the given polyhedron.
	 * 
	 * @param polyhedron
	 * @return polygonal cross-section
	 */
	public CrossSection getCrossSection(Polyhedron polyhedron) {
		SimpleGeometry handle = new SimpleGeometry(g);
		SimplePolyhedron simpPoly = handle.new SimplePolyhedron(polyhedron.getVertices(),polyhedron.getCreaseFaces());

		//Nudge cutting plane until no existing vertices coincide with the surface.
		initPlane(polyhedron);

		//Set up data structures to keep track of which faces intersect,
		//  so we'll know if any faces are missed. (E.g. in concave cases)
		HashSet<SimpleFace> intersectingFaces = getIntersectingFaces(simpPoly);

		//Set up the loop invariants
		ArrayList<Vertex> 	contour1 = new ArrayList<Vertex>();				//List of intersecting points (i.e. corners of the cross section)
		ArrayList<Boolean> 	creaseTags1 = new ArrayList<Boolean>();		//Boolean corresponding to each edge identifies as crease/non-crease.
		ArrayList<Vertex> 	contour2 = new ArrayList<Vertex>();
		ArrayList<Boolean> 	creaseTags2 = new ArrayList<Boolean>();		
		SimpleFace startFace = getNextFace(simpPoly, null, null);				//startFace will be null if there's no intersection

		//Traverse the surface of the polyhedron until we end up back where we started, finding intersections along the way.
		HashSet<SimpleFace> traversed = traverse(contour1, creaseTags1, handle, simpPoly, startFace);

		if (traversed.size() < intersectingFaces.size()) {					//If there are faces left to visit, traverse the other contour.
			for (SimpleFace f : intersectingFaces) 
				if (!traversed.contains(f)) {
					startFace = f;
					break;
				}
			traverse(contour2, creaseTags2, handle, simpPoly, startFace);
		}

		//Return the result.
		CrossSection result = new CrossSection (
				contour1.toArray(new Vertex[contour1.size()]), 
				creaseTags1.toArray(new Boolean[creaseTags1.size()]), 
				contour2.toArray(new Vertex[contour2.size()]),
				creaseTags2.toArray(new Boolean[creaseTags2.size()])
		);
		result.tag = polyhedron;
		return result;
	}
	
	//******************************************************************************************************************************
	//
	//			HELPER METHODS
	//
	//******************************************************************************************************************************
	/**Traverse the surface of the polyhedron until we end up back where we started, 
	 * finding intersections along the way.
	 * @param arraylist for vertices
	 * @param the_instance_of_SimpleGeometry
	 * @param the_polyhedron_in_question
	 * @param starting face
	 * @return hash-set of traversed faces
	 */
	private HashSet<SimpleFace> traverse(ArrayList<Vertex> contour, ArrayList<Boolean> creaseTags, SimpleGeometry handle, SimplePolyhedron simpPoly, SimpleFace startFace) {
		SimpleEdge lastEdge = handle.new SimpleEdge(new int[] {-1,-1}, new int[]{-1,-1}); //Dummy edge
		SimpleFace currentFace = startFace;
		HashSet<SimpleFace> traversedFaces = new HashSet<SimpleFace>();

		while (	(currentFace != null) 		&& 	((!startFace.equals(currentFace)) 	^ 		(lastEdge.localIndexes[0] == -1))		) {
			traversedFaces.add(currentFace);
			lastEdge = getNextEdge(currentFace, lastEdge);
			contour.add(getIntersection(lastEdge));
			creaseTags.add(currentFace.isCrease);
			currentFace = getNextFace(simpPoly, currentFace, lastEdge);
		}

		return traversedFaces;
	}

	/** Returns the "next" edge on a given face that intersects the cutting plane.
	 * @param face
	 * @param lastEdge
	 * @return next edge
	 */
	private SimpleEdge getNextEdge(SimpleFace face, SimpleEdge lastEdge) {
		for (SimpleEdge edge : face.edges){
			if (edge.equals(lastEdge)) continue;
			if (planeEq(edge.getVertex(0))*planeEq(edge.getVertex(1)) < 0 )
				return edge;		
		}
		throw new ArrayIndexOutOfBoundsException("Can't find an intersecting edge. :( ");
	}

	/** Returns the next face across the given edge, given a "starting" face.
	 * @param polyhedron
	 * @param lastFace
	 * @param edge
	 * @return
	 */
	private SimpleFace getNextFace(SimplePolyhedron poly, SimpleFace lastFace, SimpleEdge edge) {
		if (lastFace == null) {		//Case 1: We just any intersecting face, to start from. 
			for (SimpleFace f : poly.faces) {
				for (SimpleEdge e : f.edges)
					if (planeEq(e.getVertex(0))*planeEq(e.getVertex(1)) < 0) return f;
			}
			return null;
		}

		SimpleFace[] adjFaces = poly.getAdjacentFaces(edge);		//Case 2: We need the next face along the path.

		if (adjFaces[0].equals(lastFace))	
			return adjFaces[1];
		else 
			return adjFaces[0];
	}

	/** Given an edge, calculates the intersection point of this edge and the cutting plane. 
	 * Results are not guaranteed to be useful if the edge doesn't actually intersect.
	 * @param edge
	 * @return intersection point
	 */
	private Vertex getIntersection(SimpleEdge edge) {
		float f1 = planeEq(edge.getVertex(0));
		float f2 = planeEq(edge.getVertex(1));

		if (f1*f2 >= 0) Log.p("Factor product is " + (f1*f2) + " ... something wrong in getIntersection().");

		float factor = f1/(f1-f2);

		float x = edge.getVertex(0).getX() + factor*(edge.getVertex(1).getX() - edge.getVertex(0).getX());
		float y = edge.getVertex(0).getY() + factor*(edge.getVertex(1).getY() - edge.getVertex(0).getY());
		float z = edge.getVertex(0).getZ() + factor*(edge.getVertex(1).getZ() - edge.getVertex(0).getZ());

		return new Vertex(x,y,z);
	}

	/** Inputs the given Point into the plane equation for the cutting plane.
	 * @param point
	 * @return output of plane equation
	 */
	private float planeEq(Vertex q) {
		Vector pq = new Vector(p, q);
		float dot = pq.dotProduct(n);

		return dot;
	}

	/** Temporarily nudge the cutting plane if any points on the plane coincide with the cutting plane.
	 * 
	 * @param polyhedron
	 */
	private void initPlane(Polyhedron poly) {
		boolean allVertexesClear = false;
		p = app.getCutter().getP();
		n = app.getCutter().getN();

		Vector nudge =n.getScaled(1/10000.0f);		//Get a vector orthogonal to the cutting plane, with minimal magnitude.

		if (n.dotProduct(new Vector(p,poly.getCentroid(g))) < 0)	//Make sure it points INTO the polyhedron.
			nudge = nudge.getInverse();

		while (!allVertexesClear) {							//Nudge the cutting plane over until no points coincide
			allVertexesClear = true;
			for (int i : poly.getVertices()) {
				if (planeEq(g.get(i)) != 0) continue;
				allVertexesClear = false;
				break;
			}
			if (allVertexesClear) break;

			p = p.plus(nudge);
		}
	}
	
	/** Returns a HashSet containing all faces in the mesh that intersect the cutting plane
	 * @param simpPoly
	 * @return hashSet_of_simpFaces
	 */
	private HashSet<SimpleFace> getIntersectingFaces(SimplePolyhedron simpPoly) {
		HashSet<SimpleFace> hash = new HashSet<SimpleFace>();
		for (SimpleFace f : simpPoly.faces) 
			for (SimpleEdge e : f.edges)
				if (planeEq(e.getVertex(0))*planeEq(e.getVertex(1)) < 0)
					hash.add(f);
		return hash;
	}
}
