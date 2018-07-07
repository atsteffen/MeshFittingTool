package app.tools.topology;

import java.util.ArrayList;

import app.tools.Log;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.structure.GeometryMap;

/** Polyhedron.java
 * 
 * Represents a single Polyhedron. (May be either a Tetrahedron or an Octahedron.) <br>
 * <br>
 * Contains tags remembering what material a polyhedron consists of, and whether or not it
 * is selected.
 * 
 * @author Don McCurdy
 *
 */
public abstract class Polyhedron {
	protected int[] vertices;
	protected short[] creaseFaces;
	protected int material;
	protected boolean selected = false;
	public static final short NONE = 0, LOW = 1, MODERATE = 2, HIGH = 3;
	protected short selectionExpr = (short)( Math.random()*4d );
//	protected Expression exprData = null;
	
	/** Instantiates a new polyhedron. 
	 * @param verts
	 * @param mat
	 */
	public Polyhedron (int[] verts, int mat) {
		vertices = verts;
		material = mat;
		creaseFaces = new short[]{};
	}

	/** Returns the indexes of the vertices of the polyhedron. 
	 * @return indexes
	 */
	public int[] getVertices() {
		return vertices;
	}

	/** Returns the material of the polyhedron. 
	 * @return material
	 */
	public int getMaterial() {return material;	}

	/** Sets the material of the polyhedron. 
	 * @param material
	 */
	public void setMaterial(int material) { this.material = material; }

	public short[] getCreaseFaces () {
		return creaseFaces;
	}

	/** Sets the expression strength for this polyhedron.
	 * @param expr
	 */
	public void setSelectionExpression(short expr) { 
		switch (expr) {
		case NONE :
		case LOW :
		case MODERATE:
		case HIGH :
			break;
		default:
			Log.p("WARNING: Expression strength applied to polyhedron " + this.toString() + " is invalid. R");
		}
		selectionExpr = expr; 
//		exprData = null;
	}

	/** Returns true if the polyhedron is "selected," false otherwise.
	 * @return isSelected
	 */
	public boolean isSelected() { return selected; }

	public void addCreaseFace (int index) {
		short[] old = creaseFaces;
		creaseFaces = new short[old.length+1];
		for (int i = 0; i < old.length; ++i)
			creaseFaces[i] = old[i];
		creaseFaces[old.length] = (short)index;
	}

	/** Set the polyhedron to be "selected" or "unselected." (true/false respectively)
	 * @param isSelected
	 */
	public void setSelected(boolean isSelected, short selectionExpr) { 
		this.selected = isSelected; 
		setSelectionExpression(selectionExpr); 
	}

	/** Returns the indexes of the faces of the polyhedron, along with the normal vectors.
	 * @param g
	 * @param faces
	 * @param normals
	 */
	public void getFaceData(Geometry g, int[][] faces, Vector[] normals) {
		getFaceIndexes(faces);
		getNormals(g, faces, normals);
	}

	/** Returns the number of faces in the polyhedron. 
	 * @return numFaces
	 */
	public abstract int getNumFaces();

	/** Returns the indexes of the vertices, organized by face. 
	 * @param faces
	 */
	public abstract void getFaceIndexes(int[][] faces);

	/** Returns the external edges of the polyhedron, such that for resulting array ARR,
	 * (ARR[2n-1],ARR[2n]) gives the n'th edge. */
	public abstract ArrayList<Vertex> getEdges(Geometry g);

	/** Returns the normal vectors of the faces.
	 * @param g
	 * @param faceIndexes
	 * @param normals
	 */
	protected void getNormals(Geometry g, int[][] faceIndexes, Vector[] normals) {
		Vertex[][] faces = new Vertex[faceIndexes.length][faceIndexes[0].length];
		for (int j = 0; j < faces.length; ++j) {
			for (int i = 0; i < faces[0].length; ++i) 
				faces[j][i] = g.get(faceIndexes[j][i]);
			normals[j] = Vertex.normal(faces[j][0], faces[j][1], faces[j][2]).getInverse();
		}
	}

	/** Returns the centroid of the polyhedron.
	 * @param g
	 * @return centroid
	 */
	public Vertex getCentroid(Geometry g) {
		Vertex centroid =  new Vertex(0,0,0);
		for (int i = 0; i < vertices.length; ++i) {
			centroid.setX(		centroid.getX() 		+ 		g.get(vertices[i]).getX() 		);
			centroid.setY(		centroid.getY() 		+ 		g.get(vertices[i]).getY() 		);
			centroid.setZ(		centroid.getZ() 		+ 		g.get(vertices[i]).getZ() 		);
		}
		return new Vertex(	centroid.getX()/vertices.length,		centroid.getY()/vertices.length, 			centroid.getZ()/vertices.length		);
	}

	public float getVolume(Geometry g) {
		float volume = 0;
		if (this instanceof Tetrahedron) {
			//((V1Xv2).V3)/6
			Vertex a = g.get(vertices[0]);
			Vertex b = g.get(vertices[1]);
			Vertex c = g.get(vertices[2]);
			Vertex d = g.get(vertices[3]);
			Vector shift = new Vector (7,4,5);
			Vector v1 = new Vector (a,b);
			Vector v2 = new Vector (a,c);
			Vector v3 = new Vector (a,d);
			v1=v1.plus(shift);
			v2=v2.plus(shift);
			v3=v3.plus(shift);
			Vector cross = v1.crossProduct(v2).plus(shift);
			volume = (cross.dotProduct(v3))/6f;
		}
		else if (this instanceof Octahedron) {
			int a = vertices[0];
			int b = vertices[1];
			int c = vertices[2];
			int d = vertices[3];
			int e = vertices[4];
			int f = vertices[5];
			float vol1 = new Tetrahedron(new int[] {a,b,c,e},getMaterial()).getVolume(g);
			float vol2 = new Tetrahedron(new int[] {a,b,d,e},getMaterial()).getVolume(g);
			float vol3 = new Tetrahedron(new int[] {f,b,c,e},getMaterial()).getVolume(g);
			float vol4 = new Tetrahedron(new int[] {f,b,d,e},getMaterial()).getVolume(g);
			volume = vol1+vol2+vol3+vol4;
		}
		return (volume < 0) ? 0.01f : volume;
	}
	
	/** Subdivides the polyhedron, resulting in an array of new polyhedra.
	 * @param g
	 * @param hash
	 * @return newPolyhedra
	 */
	public ArrayList<Polyhedron> subdivide(Geometry g, GeometryMap hash) {
		ArrayList<Polyhedron> results = subdivideHelper(g,hash);
		return results;
	}

	protected abstract ArrayList<Polyhedron> subdivideHelper(Geometry g, GeometryMap hash);

}
