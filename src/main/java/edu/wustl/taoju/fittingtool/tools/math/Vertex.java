package app.tools.math;

import app.tools.structure.Mesh;

/** 
 * Vertex.java
 * <br>
 * Stores the coordinates of a point in 3-space.
 * 
 * @author by Don McCurdy
 * @date June 2010
 */
public class Vertex
{
	/** X-coordinate */ private float x;
	/** Y-coordinate */ private float y;
	/** Z-coordinate */ private float z;
	
	/** Identifier for an isolated vertex */ 			public static final int ISOLATED 				= -1;
	/** Identifier for a "normal" vertex  */ 			public static final int NORMAL 				=   3;
	/** Identifier for a vertex on a crease face*/ 	public static final int CREASE_FACE 			=   2;
	/** Identifier for a vertex on a crease edge*/	public static final int CREASE_EDGE 			=   1;
	/** Identifier for a crease point */					public static final int CREASE_POINT 		=   0;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTORS
	//
	//******************************************************************************************************************************
	
	/** Instantiates a vertex, given its coordinates
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vertex(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	//******************************************************************************************************************************
	//
	//			ACCESSORS
	//
	//******************************************************************************************************************************

	/** Return the X coordinate
	 * @return x
	 */                
	public float getX() { return x; }
	/** Return the Y coordinate
	 * @return y
	 */                
	public float getY() { return y; }
	/** Return the Z coordinate
	 * @return z
	 */                
	public float getZ() { return z; }

	/** Returns the Vertex information in an array of floats
	 * @return coordsInArray
	 */                
	public float[] getXYZ() { float[] f = { x, y, z, 0,0 }; return f; }
	
	//******************************************************************************************************************************
	//
	//			MODIFIERS
	//
	//******************************************************************************************************************************

	/** Sets the X coordinate to x
	 * @param x
	 */                
	public void setX(float x) { this.x = x; }
	/** Set the Y coordinate to y
	 * @param y
	 */                
	public void setY(float y) { this.y = y; }
	/** Set the Z coordinate to z
	 * @param z
	 */                
	public void setZ(float z) { this.z = z; }
	/** Set all three coordinates using an array
	 * @param array
	 * */
	public void setXYZ(float[] xyz) { this.x = xyz[0]; this.y = xyz[1]; this.z= xyz[2]; }

	/** Return a new vertex, with the coordinates of the current vertex scaled by a given scalar
	 * @param scalar
	 * @return scaled_vertex
	 */
	public Vertex getScaled(float scalar) {
		return new Vertex (x*scalar, y*scalar, z*scalar);
	}

	//******************************************************************************************************************************
	//
	//			OPERATORS
	//
	//******************************************************************************************************************************

	/** Computes and returns the sum of the "this" Vertex and a (parameter) Vector 
	 * @param otherVector
	 */
	public Vertex plus(Vector v) {
		return new Vertex (this.x + v.getX(), this.y + v.getY(), this.z + v.getZ());
	}
	
	/** Computes the sum of two points. Not geometrically meaningful, but hey. */
	public Vertex plus(Vertex other) { return new Vertex(this.x+ other.x, this.y+ other.y, this.z+ other.z); }
	
	public Vector minus(Vertex other) {
		return new Vector(this.x - other.x, this.y - other.y, this.z - other.z);
	}
	
	/** Static method. Computes the normal vector, if 'v1,' 'v2,' and 'v3,' are corners of a polygon, given in counter-clockwise order.
	 * @param a
	 * @param b
	 * @param c
	 * @return normal_vector
	 */
	public static Vector normal(Vertex v1, Vertex v2, Vertex v3) {
		return ((new Vector(v1, v2)).crossProduct(new Vector(v2, v3))).getNormalized();
	}

	/** Static method. Computes the Vertex midpoint of two (parameter) Vertices
	 * @param a
	 * @param b
	 * @return midpoint
	 */
	public static Vertex midpoint(Vertex a, Vertex b) {
		return new Vertex((a.x+b.x)/2, (a.y+b.y)/2, (a.z+b.z)/2);
	}

	//******************************************************************************************************************************
	//
	//			WHATEVER
	//
	//******************************************************************************************************************************

	/** Gives a string representation of a vertex.
	 * @return vertexAsString
	 */
	public String toString() {
		return "( " + x + ", " + y + ", " + z + ")";
	}

	/** Returns true if and only if two vertices have the same coordinates. 
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
		Vertex other = (Vertex) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
			return false;
		if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
			return false;
		if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
			return false;
		return true;
	}

	/** Calculates a hash code based on a point's x/y/z coordinates.
	 * @return hashCode
	 */
	@Override
	public int hashCode() {		
		//Log.p("Warning: Deprecated vertex hashCode() method is being used.");
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		result = prime * result + Float.floatToIntBits(z);
		return result;
	}

	@Deprecated
	/** Returns a long hash code. Deprecated in favor of hashCode().
	 * @return long_hashCode
	 */
	public long longHashCode() {
		final long buffer = Mesh.DEFAULT_BUFFER;
		long result = 1;
		result  = buffer*result + Float.floatToIntBits(x);
		result  = buffer*result + Float.floatToIntBits(y);
		result  = buffer*result + Float.floatToIntBits(z);
		return result;
	}

	public double[] getXYZDouble() {
		double[] f = { (double)x, (double)y, (double)z }; return f;
	}
}