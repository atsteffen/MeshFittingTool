package app.tools.math;

/** Vector.java
 * <br>
 * Represents a vector in 3D space.
 * Stores and operates on a set of three float values.
 * @author Don McCurdy
 * @date June 2010
 *
 */
public class Vector {
	/** X-coordinate */ 	private final float x;
	/** Y-coordinate */ 	private final float y;
	/** Z-coordinate */	private final float z;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTORS
	//
	//******************************************************************************************************************************

	/** Instantiates an instance of the Vector class, given its coordinates.
	 * @param x
	 * @param y
	 * @param z
	 */
	public Vector (float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** Instantiates an instance of the Vector class, given its coordinates in an array 
	 * 
	 * @param xyz
	 */
	public Vector (float [] xyz ) {
		if (xyz.length < 3)
			throw new ArrayIndexOutOfBoundsException("The Vector constructor requires three coordinates.");
		this.x = xyz[0];
		this.y = xyz[1];
		this.z = xyz[2];
	}

	/** Generates a vector, pointing from the first Vertex to the second.
	 * NOTE: Order is kind of important here.
	 * @param tail (source vertex)
	 * @param head (destination vertex)
	 */
	public Vector (Vertex tail, Vertex head) {
		this.x = head.getX() - tail.getX();
		this.y = head.getY() - tail.getY();
		this.z = head.getZ() - tail.getZ();
	}

	/** Copy-constructor 
	 * 
	 * @param vector
	 */
	public Vector (Vector v) {
		this.x = v.x;
		this.y = v.y;
		this.z = v.z;
	}

	//******************************************************************************************************************************
	//
	//			GENERAL ACCESSORS
	//
	//******************************************************************************************************************************

	/**@return x_component */
	public float getX() {
		return x;
	}

	/**@return y_component */
	public float getY() {
		return y;
	}

	/**@return z_component */
	public float getZ() {
		return z;
	}
	/**@return all_components_in_array*/
	public float[] getXYZ() {
		return new float[] {x,y,z};
	}

	/** @return magnitude */
	public float getMagnitude() {						
		return (float)Math.sqrt(x*x+y*y+z*z);
	}

	//******************************************************************************************************************************
	//
	//			VECTOR MUTATORS
	//
	//******************************************************************************************************************************

	/** Returns the scaled vector - does not affect original vector 
	 * @param factor
	 * @return scaledVector
	 */
	public Vector getScaled(float factor) {
		return new Vector(x*factor, y*factor, z*factor);
	}

	/** Returns the inverse of this vector - does not affect original vector
	 * 
	 * @return invertedVector
	 */
	public Vector getInverse() {
		return new Vector(-1*x, -1*y, -1*z);
	}

	/** Returns normalized vector - does not affect original vector
	 * 
	 * @return normalizedVector
	 */
	public Vector getNormalized() {
		return getScaled(1.0f/getMagnitude());
	}

	/** Returns the current vector, multiplied by the given matrix. Does not affect original vector
	 * 
	 * @param matrix
	 * @return vectorMultipliedByMatrix
	 */
	public Vector getMultiplied(float[][] matrix) {
		if (matrix.length == 3 && matrix[0].length == 3) {
			return new Vector(
					matrix[0][0]*x + matrix[0][1]*y + matrix[0][2]*z, 
					matrix[1][0]*x + matrix[1][1]*y + matrix[1][2]*z,
					matrix[2][0]*x + matrix[2][1]*y + matrix[2][2]*z);
		} else throw new UnsupportedOperationException("Provided matrix is not 3x3.");
	}

	//******************************************************************************************************************************
	//
	//			VECTOR OPERATIONS
	//
	//******************************************************************************************************************************

	/** Computes and returns the dot product of two vectors*/
	public float dotProduct(Vector other) {
		return (this.x*other.x + this.y*other.y + this.z*other.z);
	}

	/** Computes and returns the cross product of two vectors*/
	public Vector crossProduct (Vector other) {
		return new Vector (
				this.y*other.z 		- 		this.z*other.y,
				this.z*other.x		-		this.x*other.z,
				this.x*other.y		-		this.y*other.x
		);
	}

	/** Computes and returns the sum of two vectors*/
	public Vector plus(Vector other) {
		return new Vector(x+other.x, y+other.y, z+other.z);
	}

	/** Computes and returns the difference of two vectors*/
	public Vector minus(Vector other) {
		return new Vector(x-other.x, y-other.y, z-other.z);
	}
	
	/** Computes the intersection of a line segment and ray, if it exists. Returns true if the intersection
	 * exists and stores the distance from the ray's origin to the point, and returns false otherwise.
	 * 
	 * Note that for to work with arbitrary rays/lines, some adjustments need to be made here.
	 * 
	 * Blatantly a modified version of a method written by Sun Microsystems:
	 * com.sun.j3d.utils.behaviors.picking.Intersect.lineAndRay()
	 * 
	 * Original code subject to conditions of use appended at bottom of this file. 
	 * 
	 * @param segmentStart
	 * @param segmentEnd
	 * @param rayOrigin
	 * @param rayVector
	 * @param originToIntersectionDistance
	 * @return
	 */
	public static boolean intersectSegmentAndRay(Vertex start, Vertex end, Vertex ori, Vector dir, float dist[]) {
		float m00, m01, m10, m11;
		float mInv00, mInv01, mInv10, mInv11;
		float dmt, t, s, tmp1, tmp2;
		Vector lDir;

		lDir = new Vector(end.getX() - start.getX(), end.getY() - start.getY(), end.getZ() - start.getZ());
		
		int x,y,z;
		if (dir.getX() > dir.getY() && dir.getX() > dir.getZ()) {x = 0; y = 1; z = 2;} 			//Z
		else if (dir.getZ() > dir.getX() && dir.getZ() > dir.getY()) {x = 0; y = 2; z = 1;} 	//Y
		else {x = 2; y = 1; z = 0;}																				//X
		
		m00 = lDir.getXYZ()[x];
		m01 = -dir.getXYZ()[x];
		m10 = lDir.getXYZ()[y];
		m11 = -dir.getXYZ()[y];

		dmt = (m00 * m11) - (m10 * m01); 	// Get the determinant.
		if (dmt == 0f)  	return false;				// No solution, hence no intersect.
		

		tmp1 = 1 / dmt;								// Find the inverse.

		mInv00 = tmp1 * m11;
		mInv01 = tmp1 * (-m01);
		mInv10 = tmp1 * (-m10);
		mInv11 = tmp1 * m00;

		tmp1 = ori.getXYZ()[x] - start.getXYZ()[x];
		tmp2 = ori.getXYZ()[y] - start.getXYZ()[y];

		t = mInv00 * tmp1 + mInv01 * tmp2;
		s = mInv10 * tmp1 + mInv11 * tmp2;

		if (s < 0.0) {return false; }					// Before the origin of ray. 
		if ((t < 0) || (t > 1.0)) {return false;} 	// Before or after the end points of line.

		tmp1 = ori.getXYZ()[z] + s * dir.getXYZ()[z];
		tmp2 = start.getXYZ()[z] + t * lDir.getXYZ()[z];

		//		if ((tmp1 < (tmp2 - Float.MIN_VALUE))
		//				|| (tmp1 > (tmp2 + Float.MIN_VALUE))) {
		//			Log.p("Special case returning FALSE.");
		//			return false;
		//		}
		
		dist[0] = s;										// Distance from ray origin to intersection point.
		return true;
	}

	//******************************************************************************************************************************
	//
	//			GENERAL
	//
	//******************************************************************************************************************************
	/** Identifier used to specify X-axis for rotation. */ public static final int X_AXIS = 1;
	/** Identifier used to specify Y-axis for rotation. */ public static final int Y_AXIS = 2;
	/** Identifier used to specify Z-axis for rotation. */ public static final int Z_AXIS = 3;

	/** Provides a string representation of a vector. @return string */
	public String toString() {
		return "<[V] : " + x + ", " + y + ", " + z + ">";
	}

	/** Returns a rotation matrix, to rotate a vector around the given axis a given number of degrees.
	 * 
	 * @param which_axis
	 * @param angleInDegrees
	 * @return rotationMatrix
	 */
	public static float[][] getRotationMatrix (int axis, float degrees) {
		double radians = (degrees * Math.PI)/180.0f;
		switch (axis) {
		case X_AXIS :
			return new float[][] {
					new float[] { 1.0f,0.0f,0.0f },
					new float[] { 0.0f, (float)Math.cos(radians), (float)Math.sin(radians) },
					new float[] { 0.0f, (-1)*(float)Math.sin(radians), (float)Math.cos(radians) }
			};
		case Y_AXIS :
			return new float[][] {
					new float[] {(float)Math.cos(radians), 0.0f, (-1)*(float)Math.sin(radians)},
					new float[] {0.0f, 1.0f, 0.0f},
					new float[] {(float)Math.sin(radians), 0.0f, (float)Math.cos(radians)}
			};
		case Z_AXIS :
			return new float[][] {
					new float[] { (float) Math.cos(radians), (float)Math.sin(radians), 0.0f },
					new float[] { (-1)*(float)(Math.sin(radians)), (float)Math.cos(radians), 0.0f },
					new float[] {0.0f, 0.0f, 1.0f }
			};
		default :
			throw new UnsupportedOperationException("Please provide a valid axis identifier, E.G. Vector.Y_AXIS");
		}
	}

	public double[] getXYZDouble() {
		return new double[] {(double)x,(double)y,(double)z};
	}

	public float rotationAngle(Vector other) {
		float val = this.dotProduct(other)/this.getMagnitude()/other.getMagnitude();
		return (float) java.lang.Math.acos(val);
	}
	
}

/* The following conditions apply to re-use of the method, 'intersectSegmentAndRay()',
 * which is a modified version of code written by Sun Microsystems.
 * 
 * 
0002:         * $RCSfile: Intersect.java,v $
0003:         *
0004:         * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
0005:         *
0006:         * Redistribution and use in source and binary forms, with or without
0007:         * modification, are permitted provided that the following conditions
0008:         * are met:
0009:         *
0010:         * - Redistribution of source code must retain the above copyright
0011:         *   notice, this list of conditions and the following disclaimer.
0012:         *
0013:         * - Redistribution in binary form must reproduce the above copyright
0014:         *   notice, this list of conditions and the following disclaimer in
0015:         *   the documentation and/or other materials provided with the
0016:         *   distribution.
0017:         *
0018:         * Neither the name of Sun Microsystems, Inc. or the names of
0019:         * contributors may be used to endorse or promote products derived
0020:         * from this software without specific prior written permission.
0021:         *
0022:         * This software is provided "AS IS," without a warranty of any
0023:         * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
0024:         * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
0025:         * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
0026:         * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
0027:         * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
0028:         * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
0029:         * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
0030:         * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
0031:         * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
0032:         * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
0033:         * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
0034:         * POSSIBILITY OF SUCH DAMAGES.
0035:         *
0036:         * You acknowledge that this software is not designed, licensed or
0037:         * intended for use in the design, construction, operation or
0038:         * maintenance of any nuclear facility.
0039:         *
0040:         * $Revision: 1.4 $
0041:         * $Date: 2007/02/09 17:20:13 $
0042:         * $State: Exp $
0043:         */
