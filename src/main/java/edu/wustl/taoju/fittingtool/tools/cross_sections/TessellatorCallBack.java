package app.tools.cross_sections;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellatorCallback;

import app.tools.Log;

/** TessellatorCallBack.java
 * <br>
 * This class is necessary for rendering concave polygons in JOGL.
 * Note, I did not write any of the code on this page - it is freely available
 * from Java-Tips.org, and copyright information is at the bottom of the page.
 * 
 * @source Kiet Le's port of The Red Book, using JOGL
 * @url http://ak.kiet.le.googlepages.com/theredbookinjava.html
 *
 */
public class TessellatorCallBack implements GLUtessellatorCallback {
	private GL gl;
	private GLU glu;

	public TessellatorCallBack(GL gl, GLU glu)
	{
		this.gl = gl;
		this.glu = glu;
	}

	public void begin(int type)
	{
		gl.glBegin(type);
	}

	public void end()
	{
		gl.glEnd();
	}

	public void vertex(Object vertexData)
	{
		double[] pointer;
		if (vertexData instanceof double[])
		{
			pointer = (double[]) vertexData;
			if (pointer.length == 6) gl.glColor3dv(pointer, 3);
			gl.glVertex3dv(pointer, 0);
		}

	}

	public void vertexData(Object vertexData, Object polygonData)
	{
	}

	/*
	 * combineCallback is used to create a new vertex when edges intersect.
	 * coordinate location is trivial to calculate, but weight[4] may be used to
	 * average color, normal, or texture coordinate data. In this program, color
	 * is weighted.
	 */
	public void combine(double[] coords, Object[] data, //
			float[] weight, Object[] outData)
	{	
		double[] vertex = new double[6];
		int i;

		vertex[0] = coords[0];
		vertex[1] = coords[1];
		vertex[2] = coords[2];
		for (i = 3; i < 6/* 7OutOfBounds from C! */; i++)
			vertex[i] = weight[0] //
			                   * ((double[]) data[0])[i] + weight[1]
			                                                      * ((double[]) data[1])[i] + weight[2]
			                                                                                         * ((double[]) data[2])[i] + weight[3]
			                                                                                                                            * ((double[]) data[3])[i];
		outData[0] = vertex;	
	}

	public void combineData(double[] coords, Object[] data, //
			float[] weight, Object[] outData, Object polygonData)
	{
	}

	public void error(int errnum)
	{
		String estring;

		estring = glu.gluErrorString(errnum);
		System.err.println("Tessellation Error: " + estring); 
		Log.p("TessellatorCallBack.java requesting application restart.");
		throw new RuntimeException("Couldn't tessellate cross section.");
	}

	public void beginData(int type, Object polygonData)
	{
	}

	public void endData(Object polygonData)
	{
	}

	public void edgeFlag(boolean boundaryEdge)
	{
	}

	public void edgeFlagData(boolean boundaryEdge, Object polygonData)
	{
	}

	public void errorData(int errnum, Object polygonData)
	{
	}
}
/*
 *	For the software in this file
 * (c) Copyright 1993, Silicon Graphics, Inc.
 * ALL RIGHTS RESERVED 
 * Permission to use, copy, modify, and distribute this software for 
 * any purpose and without fee is hereby granted, provided that the above
 * copyright notice appear in all copies and that both the copyright notice
 * and this permission notice appear in supporting documentation, and that 
 * the name of Silicon Graphics, Inc. not be used in advertising
 * or publicity pertaining to distribution of the software without specific,
 * written prior permission. 
 *
 * THE MATERIAL EMBODIED ON THIS SOFTWARE IS PROVIDED TO YOU "AS-IS"
 * AND WITHOUT WARRANTY OF ANY KIND, EXPRESS, IMPLIED OR OTHERWISE,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL SILICON
 * GRAPHICS, INC.  BE LIABLE TO YOU OR ANYONE ELSE FOR ANY DIRECT,
 * SPECIAL, INCIDENTAL, INDIRECT OR CONSEQUENTIAL DAMAGES OF ANY
 * KIND, OR ANY DAMAGES WHATSOEVER, INCLUDING WITHOUT LIMITATION,
 * LOSS OF PROFIT, LOSS OF USE, SAVINGS OR REVENUE, OR THE CLAIMS OF
 * THIRD PARTIES, WHETHER OR NOT SILICON GRAPHICS, INC.  HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH LOSS, HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, ARISING OUT OF OR IN CONNECTION WITH THE
 * POSSESSION, USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 * US Government Users Restricted Rights 
 * Use, duplication, or disclosure by the Government is subject to
 * restrictions set forth in FAR 52.227.19(c)(2) or subparagraph
 * (c)(1)(ii) of the Rights in Technical Data and Computer Software
 * clause at DFARS 252.227-7013 and/or in similar or successor
 * clauses in the FAR or the DOD or NASA FAR Supplement.
 * Unpublished-- rights reserved under the copyright laws of the
 * United States.  Contractor/manufacturer is Silicon Graphics,
 * Inc., 2011 N.  Shoreline Blvd., Mountain View, CA 94039-7311.
 *
 * OpenGL(TM) is a trademark of Silicon Graphics, Inc.
 */
