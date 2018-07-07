package edu.wustl.taoju.fittingtool.tools;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

public class FileUtils 
{
	
	public static BufferedImage getImage(int index, String directory, String basename, String ext, int scale) {
		String path = directory + "/" + basename + (scale+index)+"."+ext;
		return getImage(path);
	}

	/** Given the name/path of an image, returns image from server. 
	 * 
	 * @param imagePath
	 * @return image
	 */
	public static BufferedImage getImage(String imagePath) {		
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		byte[] bytes = FileUtils.getFile(imagePath);

		try { image = ImageIO.read(new ByteArrayInputStream(bytes)); } 
		catch (IOException e) { Log.p("Could not load image: " + e); }

		return image;
	}
	
	public static byte[] getFile (String fname )
	{
		// Get file from local machine	
		try {
			DataInputStream in = new DataInputStream( new FileInputStream( fname ) );

			// First, get the fileinfo
			File fl = new File( fname ) ;
			int length = (int)( fl.length() );

			// Read the file content from request
			byte[] fcontent = new byte[length];
			int nread = 0 ;
			while ( nread < length )
			{
				nread += in.read( fcontent, nread, length - nread );
			}

			in.close();
			return fcontent;			
		} catch (IOException e) 
		{
			return null ;
		}

	}
	
	public static void copyfile(File f1, File f2){
		try{
			InputStream in = new FileInputStream(f1);
			OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			System.out.println("File copied.");
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage() + " in the specified directory.");
			System.exit(0);
		}
		catch(IOException e){
			System.out.println(e.getMessage());  
		}
	}

}

