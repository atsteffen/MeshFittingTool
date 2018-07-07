package app.tools.topology;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import app.tools.math.Vector;
import app.tools.math.Vertex;

public class Landmark {
	
	private List<Integer> materials;
	private Vertex location;
	private Color displayColor;
	
	public Landmark(Vertex v) {
		location = v;
		materials = new ArrayList<Integer>();
	}
	
	public void addMaterial(Integer i){
		materials.add(i);
	}
	
	public void move(Vector v){
		location.plus(v);
	}
	
	public void setLocation(Vertex v){
		this.location = v;
	}
	
	public Vertex getLocation(){
		return this.location;
	}
	
	public List<Integer> getMaterials(){
		return this.materials;
	}

	public Color getDisplayColor() {
		return displayColor;
	}

	public void setDisplayColor(Color displayColor) {
		this.displayColor = displayColor;
	}

}
