package app.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.media.opengl.GL;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

import app.RegistrationTool;
import app.tools.FileUtils;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.topology.Landmark;

public class ImageStreamer {

	private float minX,minY,minZ;
	private float maxX,maxY,maxZ;
	private Vertex origin;
	
	protected Texture curTexture;
	protected int curIndex;
	protected String id;
	protected Vector axis;

	protected BufferedImage[] imageCache;
	protected BufferedImage waitingImage;

	protected int imageWidth;
	protected int imageHeight;
	
	private int currentsize;
	private int sizex;
	private int sizey;
	private int sizez;
	private int sizestart;
	private float spacingx;
	private float spacingy;
	private float spacingz;
	private String ext;
	private String base;
	private int scale;
	private String directory;

	public static final String X_AXIS = "x", Y_AXIS = "y", Z_AXIS = "z";
	private static final int LOW_TEX = 128;

	public ImageStreamer(int sizestart, int sizex, int sizey, int sizez, String base,
			String ext, int scale, String directory) {
		this.sizestart = sizestart;
		this.sizex = sizex;
		this.sizey = sizey;
		this.sizez = sizez;
		this.base = base;
		this.ext = ext;
		this.scale = scale;
		this.directory = directory;
		currentsize = 0;
		
		minX = RegistrationTool.instance.getCutter().getXMin();
		maxX = RegistrationTool.instance.getCutter().getXMax();
		minY = RegistrationTool.instance.getCutter().getYMin();
		maxY = RegistrationTool.instance.getCutter().getYMax();
		minZ = RegistrationTool.instance.getCutter().getZMin();
		maxZ = RegistrationTool.instance.getCutter().getZMax();
		
		origin = new Vertex((maxX-minX)/2.0f+minX,(maxY-minY)/2.0f+minY,(maxZ-minZ)/2.0f+minZ);
		
		int midx = (int) java.lang.Math.floor(sizex/2.0f);
		spacingx = (java.lang.Math.abs(maxX-minX)/2.0f)/midx;
		int midy = (int) java.lang.Math.floor(sizey/2.0f);
		spacingy = (java.lang.Math.abs(maxY-minY)/2.0f)/midy;
		int midz = (int) java.lang.Math.floor(sizez/2.0f);
		spacingz = (java.lang.Math.abs(maxZ-minZ)/2.0f)/midz;
		
		this.imageCache = new BufferedImage[sizex+sizey+sizez];
	}
	
	public String getId() { return id; }

	public void setId(String id) {
		this.id = id;
		if (getId() == "x") {
			currentsize = sizex;
		}
		if (getId() == "y") {
			currentsize = sizey;
		}
		if (getId() == "z") {
			currentsize = sizez;
		}
	}

	public Vector getAxis() { return axis; }

	public void setAxis(Vector axis) { this.axis = axis; }

	/** Loads thumbnail images from GeneServer.
	 * Full-resolution images are streamed in another thread by FTPTask objects.
	 * @param index
	 * @return image
	 */
	private BufferedImage getImage(int index) {		
		BufferedImage image = FileUtils.getImage(index, directory, base, ext, scale);
		BufferedImage scaledImage = new BufferedImage(LOW_TEX, LOW_TEX, BufferedImage.TYPE_INT_ARGB);

		// Paint scaled version of image to new image
		Graphics2D graphics2D = scaledImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, LOW_TEX, LOW_TEX, null);
		graphics2D.dispose();

		return scaledImage;
	}

	/** Given a BufferedImage, generates a new Texture object ready for binding.
	 * Requires that there be an active GL context on the current thread.
	 * 
	 * @param image
	 * @return texture
	 */
	private Texture getTexture(BufferedImage image) {
		Texture texture =  TextureIO.newTexture(image, false);
		texture.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
		texture.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
		texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		texture.bind();
		return texture;
	}

	/** Returns a texture ready for binding to a GL context, as follows:
	 * 		1) Checks to see if the viewing plane has moved. If so, depending on whether a high-res image is waiting:
	 * 				a) Return the last texture used.
	 * 				b) Use waiting high-res image to make a new texture, and return it.
	 * 		2) If viewing plane HAS moved, create a new FTP task in another thread to start downloading a high-res image.
	 * 		3) Other thread will automatically queue the high-res image once it's downloaded. In the meantime, we need a preview thumbnail.
	 * 		4) In the mean-time, check to see if a thumbnail has been cached. 
	 * 				a) If so, make it into a texture and return it.
	 * 				b) If not, download the low-res thumbnail, cache it, create a texture, and return.
	 * @param i
	 * @return
	 */
	private Texture getTexture(int i) {
		if (i < 0 || i > currentsize) return null;

		if (i == curIndex && imageCache[i] != null) 	{					//If slider hasn't moved, don't download anything.
			if (waitingImage != null) 												//High-res image may be waiting to be rendered.
				setTexture(getTexture(waitingImage));					
			return curTexture;																//Return [pre]loaded texture.
		}

//		if (task != null && !task.isDone()) 										//Start loading high-res image in a new thread.
//			task.cancel(true);															//When starting a new thread, cancel the old one.
//		task = new FTPTask(i, directory);
//		task.execute();



		if (imageCache[i] == null){													//If low-res image isn't cached, download.
			try {
				imageCache[i] = getImage(i+sizestart);	
			} catch (NullPointerException e) { e.printStackTrace(); }
		}

		setTexture(getTexture(imageCache[i]));								//Generate low-res texture and return.
		return curTexture;
	}

	/** Sets the current texture to the one provided, clearing out old resources first. */
	private void setTexture(Texture t) {
		if (curTexture != null) curTexture.dispose();
		curTexture = t;
	}

	public int getIndex(Vertex p) {

		if (getId() == "z" ) {													//Z-Stack. Middle image centered.
			return (int) Math.max(Math.min(sizez-1,(p.getZ()-minZ)/spacingz),0);
		} else if (getId() == "y" ) {											//Y-Stack. Top down.
			int offset = (int) (new Vector(p,origin).dotProduct(axis)/spacingy);
			offset += (int) java.lang.Math.floor(sizey/2.0f);
			return offset + sizestart + sizex;
		} else if (getId() == "x") {											//X-Stack. Left to right.
			int offset = (int) (new Vector(origin,p).dotProduct(axis)/spacingx);
			offset += java.lang.Math.floor(sizex/2.0f);
			return offset + sizestart;
		} else {
			throw new RuntimeException("Metadata error: No axis maps to Z. Unable to determine image index.");
		}
	}

	public void paintImage(GL gl, Vertex center) {
//		if (getId() == "z") 
//			center = center.plus(setData.getOrigin());
		gl.glPushMatrix();

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		//final float invFac =  ( (setData.getMapping(1)==1 && setData.getMapping(2)==2) ? -1f : 1f);
		final float safeDistance =-2f*RegistrationTool.instance.getCutter().getRadius();

		// gl.glClear(GL.GL_COLOR_BUFFER_BIT);  //... am I even double-buffering?
		gl.glColor3f(1.0f, 1.0f,1.0f);

		int index = getIndex(center);
		try { getTexture(index).bind(); } 	//Images aren't available for all cutting-plane locations.
		catch (NullPointerException e ) { gl.glDisable(GL.GL_TEXTURE_2D); } 
		curIndex = index;	

		RegistrationTool.instance.getCutter().rotateView(gl);

		center.setXYZ(new float[] {center.getX()-0.04541f, center.getY() - 0.30283353f,    center.getZ() });

		float minXcoord = 0.0f;
		float maxXcoord = 0.0f;
		float minYcoord = 0.0f;
		float maxYcoord = 0.0f;
		if (getId().equals("z")){
			minXcoord = minX;
			maxXcoord = maxX;
			minYcoord = minY;
			maxYcoord = maxY;
		}
		if (getId().equals("y")){
			minXcoord = minX;
			maxXcoord = maxX;
			minYcoord = minZ;
			maxYcoord = maxZ;
		}
		if (getId().equals("x")){
			minXcoord = minZ;
			maxXcoord = maxZ;
			minYcoord = minY;
			maxYcoord = maxY;
		}

		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2d(1.0, 1.0);
		gl.glVertex3f(	maxXcoord, 		minYcoord, 	 	safeDistance);	
		gl.glTexCoord2d(0.0, 1.0);
		gl.glVertex3f(	minXcoord, 		minYcoord, 	 	safeDistance);
		gl.glTexCoord2d(0.0, 0.0);
		gl.glVertex3f(	minXcoord, 		maxYcoord, 		safeDistance);
		gl.glTexCoord2d(1.0, 0.0);
		gl.glVertex3f(	maxXcoord, 		maxYcoord, 		safeDistance);
		gl.glEnd();

		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glPopMatrix();
	}

	public List<Landmark> extractLandmarks(double threshold, int smoothIters) {
		List<Landmark> landmarks = new ArrayList<Landmark>();
		
		// For now just use the z-aligned images for aligning the boundary
		for (int i=sizestart + sizex + sizey; i<sizex+sizey+sizez+sizestart; i++) {
			
			if (imageCache[i-sizestart] == null){													//If low-res image isn't cached, download.
				try {
					imageCache[i-sizestart] = getImage(i);	
				} catch (NullPointerException e) { e.printStackTrace(); }
			}
			
			landmarks.addAll(getBoundaryPoints(imageCache[i-sizestart],i-sizestart-sizey-sizex,threshold,smoothIters)); 
			
		}
		return landmarks;
	}

	private List<Landmark> getBoundaryPoints(BufferedImage bufferedImage, int index, double threshold, int smoothIters) {
		// Get all the pixels
		int w = bufferedImage.getWidth(null);
		int h = bufferedImage.getHeight(null);
		int[] matrix = new int[w*h];
		
	    for(int i=0; i < w; i ++){
	        for(int j=0; j < h; j++)
	        {
	            //Grab and set the colors one-by-one
	            int temp = bufferedImage.getRGB(i, j);
	            if (temp <= -50000) {
	            	matrix[h*i+j] = 1;
	            }
	            else {
	            	matrix[h*i+j] = 0;
	            }
	        }
	    }
	    for (int i = 0; i < smoothIters; i++) {
	    	openSmooth(matrix,w,h);
	    }
	    largestConnectForeground(matrix,w,h);
	    largestConnectBackground(matrix,w,h);
	    openSmooth(matrix,w,h);
	    List<Integer> boundary = getBoundaryPixels(matrix,w,h);
	    
	    List<Landmark> result = new ArrayList<Landmark>();
	    for (Integer i : boundary){
	    	int xint = i/h;
			int yint = h-i%h;
			float xcoord = (xint/(float)w) * (maxX-minX) + minX;
			float ycoord = (yint/(float)h) * (maxY-minY) + minY;
			float zcoord = (index/(float)sizez) * (maxZ-minZ) + minZ;
						
			Landmark lm = new Landmark(new Vertex(xcoord,ycoord,zcoord));
			lm.addMaterial(-1);
			lm.addMaterial(-2);
			lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
			
			if (java.lang.Math.random() > 0.9) {
				result.add(lm);
			}
	    }
	    
	    return result;
	}

	private void largestConnectForeground(int[] matrix, int width, int height) {
		List<Integer> currentLargest = new ArrayList<Integer>();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (matrix[height*i+j] == 1) {
					List<Integer> temp = flood8Connect(matrix, i*height+j, width, height);
					if (temp.size() > currentLargest.size()) {
						currentLargest = temp;
					}
					if (currentLargest.size() > (width*height/2)){
						break;
					}
				}
			}
		}
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				matrix[height*i+j] = 0;
			}
		}
		for(Integer i : currentLargest){
			matrix[i] = 1;
		}
	}
	private void largestConnectBackground(int[] matrix, int width, int height) {
		List<Integer> currentLargest = new ArrayList<Integer>();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (matrix[height*i+j] == 0) {
					List<Integer> temp = flood8Connect(matrix, i*height+j, width, height);
					if (temp.size() > currentLargest.size()) {
						currentLargest = temp;
					}
					if (currentLargest.size() > (width*height/2)){
						break;
					}
				}
			}
		}
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				matrix[height*i+j] = 0;
			}
		}
		for(Integer i : currentLargest){
			matrix[i] = 1;
		}
	}

	private List<Integer> flood8Connect(int[] matrix, int index, int width, int height) {
		List<Integer> result = new ArrayList<Integer>();
		Stack<Integer> stack = new Stack<Integer>();
		stack.push(index);
		result.add(index);
		matrix[index] = -1;
		while (!stack.isEmpty()) {
			int i = stack.pop();
			int w = i/height;
			int h = i%height;
			if (w != 0) {
				if (matrix[i-height] == 0) {
					result.add(i-height);
					stack.push(i-height);
					matrix[i-height] = -1;
				}
				if (h != 0) {
					if (matrix[i-height-1] == 0) {
						result.add(i-height-1);
						stack.push(i-height-1);
						matrix[i-height-1] = -1;
					}
				}
				if (h != height-1){
					if (matrix[i-height+1] == 0) {
						result.add(i-height+1);
						stack.push(i-height+1);
						matrix[i-height+1] = -1;
					}
				}
			}
			if (w != width-1) {
				if (matrix[i+height] == 0) {
					result.add(i+height);
					stack.push(i+height);
					matrix[i+height] = -1;
				}
				if (h != 0) {
					if (matrix[i+height-1] == 0) {
						result.add(i+height-1);
						stack.push(i+height-1);
						matrix[i+height-1] = -1;
					}
				}
				if (h != height-1){
					if (matrix[i+height+1] == 0) {
						result.add(i+height+1);
						stack.push(i+height+1);
						matrix[i+height+1] = -1;
					}
				}
			}
			if (h != 0) {
				if (matrix[i-1] == 0) {
					result.add(i-1);
					stack.push(i-1);
					matrix[i-1] = -1;
				}
			}
			if (h != height-1){
				if (matrix[i+1] == 0) {
					result.add(i+1);
					stack.push(i+1);
					matrix[i+1] = -1;
				}
			}
		}
		return result;
	}
	
	private void openSmooth(int[] matrix, int width, int height) {
		int[] newmatrix2 = (int[])matrix.clone();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				newmatrix2[i*height+j] = 0;
			}
		}
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (matrix[i*height+j] == 1) {
					if (i != 0) {
						newmatrix2[(i-1)*height+j] = 1;
						if (j != 0) {
							newmatrix2[(i-1)*height+(j-1)] = 1;
						}
						if (j != height-1){
							newmatrix2[(i-1)*height+(j+1)] = 1;
						}
					}
					if (i != width-1) {
						newmatrix2[(i+1)*height+(j)] = 1;
						if (j != 0) {
							newmatrix2[(i+1)*height+(j-1)] = 1;
						}
						if (j != height-1){
							newmatrix2[(i+1)*height+(j+1)] = 1;
						}
					}
					if (j != 0) {
						newmatrix2[(i)*height+(j-1)] = 1;
					}
					if (j != height-1){
						newmatrix2[(i)*height+(j+1)] = 1;
					}
				}
			}
		}
		int[] newmatrix = (int[])newmatrix2.clone();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				newmatrix[i*height+j] = 0;
			}
		}
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (newmatrix2[i*height+j] == 1) {
					if (i != 0) {
						newmatrix[(i-1)*height+j] = 1;
						if (j != 0) {
							newmatrix[(i-1)*height+(j-1)] = 1;
						}
						if (j != height-1){
							newmatrix[(i-1)*height+(j+1)] = 1;
						}
					}
					if (i != width-1) {
						newmatrix[(i+1)*height+(j)] = 1;
						if (j != 0) {
							newmatrix[(i+1)*height+(j-1)] = 1;
						}
						if (j != height-1){
							newmatrix[(i+1)*height+(j+1)] = 1;
						}
					}
					if (j != 0) {
						newmatrix[(i)*height+(j-1)] = 1;
					}
					if (j != height-1){
						newmatrix[(i)*height+(j+1)] = 1;
					}
				}
			}
		}
		matrix = (int[])newmatrix.clone();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (newmatrix[i*height+j] == 0) {
					if (i != 0) {
						matrix[(i-1)*height+j] = 0;
						if (j != 0) {
							matrix[(i-1)*height+(j-1)] = 0;
						}
						if (j != height-1){
							matrix[(i-1)*height+(j+1)] = 0;
						}
					}
					if (i != width-1) {
						matrix[(i+1)*height+(j)] = 0;
						if (j != 0) {
							matrix[(i+1)*height+(j-1)] = 0;
						}
						if (j != height-1){
							matrix[(i+1)*height+(j+1)] = 0;
						}
					}
					if (j != 0) {
						matrix[(i)*height+(j-1)] = 0;
					}
					if (j != height-1){
						matrix[(i)*height+(j+1)] = 0;
					}
				}
			}
		}
	}
	
	private List<Integer> getBoundaryPixels(int[] matrix, int width, int height) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				if (matrix[i*height+j] == 1) {
					if (i != 0) {
						if (matrix[(i-1)*height+j] == 0) {
							result.add(i*height+j);
							continue;
						}
						if (j != 0) {
							if (matrix[(i-1)*height+(j-1)] == 0){
								result.add(i*height+j);
								continue;
							}
						}
						if (j != height-1){
							if (matrix[(i-1)*height+(j+1)] == 0){
								result.add(i*height+j);
								continue;
							}
						}
					}
					if (i != width-1) {
						if (matrix[(i+1)*height+(j)] == 0){
							result.add(i*height+j);
							continue;
						}
						if (j != 0) {
							if (matrix[(i+1)*height+(j-1)] == 0){
								result.add(i*height+j);
								continue;
							}
						}
						if (j != height-1){
							if (matrix[(i+1)*height+(j+1)] == 0){
								result.add(i*height+j);
								continue;
							}
						}
					}
					if (j != 0) {
						if (matrix[(i)*height+(j-1)] == 0){
							result.add(i*height+j);
							continue;
						}
					}
					if (j != height-1){
						if (matrix[(i)*height+(j+1)] == 0){
							result.add(i*height+j);
							continue;
						}
					}
				}
			}
		}
		return result;
	}
}