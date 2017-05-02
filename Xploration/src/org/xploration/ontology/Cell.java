package org.xploration.ontology;

import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

public class Cell implements Concept {	
	 /**
	* Protege name: x
	   */
	   private int x;
	   public void setX(int value) { 
	    this.x=value;
	   }
	   public int getX() {
	     return this.x;
	   }

	   /**
	* Protege name: y
	   */
	   private int y;
	   public void setY(int value) { 
	    this.y=value;
	   }
	   public int getY() {
	     return this.y;
	   }

	   /**
	* Protege name: mineral
	   */
	   private String mineral;
	   public void setMineral(String value) { 
	    this.mineral=value;
	   }
	   public String getMineral() {
	     return this.mineral;
	   }
	   
	   //Default Constructor that I implemented
	   public Cell(){
		   this.setX(0);
		   this.setY(0);
		   this.setMineral("");
	   }
	   
	   //Constructor of Cell object
	   /*
	   public Cell(int paraX, int paraY, String paraMin){
		   this.x = paraX;
		   this.y = paraY;
		   this.mineral = paraMin;	   
	   }
	   */
}	   
	   
	   
	   
 
	    

