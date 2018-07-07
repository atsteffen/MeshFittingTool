package app.tools.structure;

import java.awt.*;

/**Regions.java
 * 
 *  Class containing methods associating colors and names with regions of the mesh.
 *
 *  @author Tao Ju
 *  
 *  (with minor changes by Don McCurdy)
 */
public class Regions
{
	private static String[] names = {"Cortex", 
									"Cerebellum",
									"Striatum",
									"Basal Forebrain",
									"Amygdala",
									"Hippocampus",
									"Hypothalamus",
									"Thalamus",
									"Olfactory Bulb",
									"Midbrain",
									"Pons",
									"Medulla",
									"Ventral Striatum",
									"Globus Pallidus",
									"Septum",
									"Fibers",
									 "Ventricles",
									 "Empty Space"};
									 
	
	private static Color[] colors = {new Color( 255, 0, 0 ),
									new Color( 204, 255, 204 ), 
									new Color( 255, 0, 255 ), 
									new Color( 255, 204, 255 ), 
									new Color( 255, 153, 0 ), 
									new Color( 102, 102, 0 ), 
									new Color( 255, 255, 0 ), 
									new Color( 0, 0, 255 ), 
									new Color( 0, 102, 0 ), 
									new Color( 0, 0, 0 ), 
									new Color( 255, 204, 204 ), 
									new Color( 204, 204, 255 ), 
									new Color( 102, 0, 102 ), 
									new Color( 0, 255, 255 ), 
									new Color( 0, 255, 0 ),
									 new Color( 204, 204, 204 ),
									 new Color( 102, 102, 102 ),
									new Color( 255, 255, 255 )} ;
	
	public Regions ( ){} ;
	
	/**
	 * Get number
	 */
	public static int getNumRegions ( )
	{
		return colors.length ;
	}
	
	/**
	 * Get index from color
	 */
	public static int getRegionIndex ( Color color )
	{
		for ( int i = 0 ; i < colors.length ; i ++ )
		{
			if ( color.equals( colors[i] ) )
			{
				return 	i ;
			}
		}
		return -1 ;
	}
	
	/**
	 * Get index from name
	 */
	public static int getRegionIndex ( String name )
	{
		for ( int i = 0 ; i < names.length ; i ++ )
		{
			if ( name.equalsIgnoreCase( names[i] ) )
			{
				return 	i ;
			}
		}
		return -1 ;
	}

	/**
	 * Get color at index
	 */
	public static Color getColor ( int index )
	{
		return colors[index] ;
	}
	
	/**
	 * Get name at index
	 */
	public static String getName ( int index )
	{
		return names[index] ;
	}
	
	/**
	 * Get Region name from color
	 */
	public static Color getColor ( String name )
	{
		for ( int i = 0 ; i < names.length ; i ++ )
		{
			if ( name.equalsIgnoreCase( names[i] ) )
			{
				return 	colors[i] ;
			}
		}
		return null ;
	}

	/**
	 * Get Region name from color
	 */
	public static String getName ( Color color )
	{
		for ( int i = 0 ; i < colors.length ; i ++ )
		{
			if ( color.equals( colors[i] ) )
			{
				return 	names[i] ;
			}
		}
		return null ;
	}
	
	/**
	 * Get Default Index
	 */
	public static int getDefaultIndex ( )
	{
		return colors.length - 1 ;
	}
	
	/**
	 * Test
	 */
	public static void main ( String[] args )
	{
		for ( int i = 0 ; i < colors.length ; i ++ )
		{
			System.out.println( colors[i].getRGB() + "," );	
		}
	}
}