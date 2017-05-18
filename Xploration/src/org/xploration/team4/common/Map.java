package org.xploration.team4.common;

import org.xploration.ontology.Cell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Map {
			
	private ArrayList<ArrayList<Cell>> worldMap = new ArrayList<ArrayList<Cell>>();
			
	//worldMap construction from a read txt file
	public Map(int dimX, int dimY){
		Cell c;
		for(int i = 1; i<=dimX; i++){
			ArrayList<Cell> tmp = new ArrayList<Cell>();
			for (int z = 0; z < dimY; z++) {
				tmp.add(null);
			}
			worldMap.add(i-1, tmp);
			for(int j = 1; j<=dimY; j++){
				if (i%2 == j%2) {
					c = new Cell();
					c.setX(i);
					c.setY(j);
					setCell(c);
				}
			}
		}
	}
	
	public void printWorldMap(){
		//Doesn't display the last mineral in the even lines. Because length is 10 not 9
		//But it stores the text file correctly
		System.out.println("Dimensions: " + getHeight() + " " + getWidth());
		for(int i = 0; i<getHeight(); i++){			
			for(int j = 0; j<getWidth(); j++){
				if (i%2 == j%2) {
					System.out.printf("(%d,%d) %s ",i+1, j+1, worldMap.get(i).get(j).getMineral());
				}
					
			}
			System.out.println();
		}		
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return the content of worldmap. If x%2 == y%2 then this is a cell, otherwise null is returned. 
	 */
    public Cell getCell(int x, int y){
        return worldMap.get(x-1).get(y-1);
    }
    
    public void setCell(Cell c) {
    	int x = c.getX();
    	int y = c.getY();
    	worldMap.get(x-1).set(y-1, c);
    }
    
    public String getMineral(int x, int y) {
        if(y%2 == x%2)
            return worldMap.get(x-1).get(y-1).getMineral();
        else {
        	System.out.println("Illegal get mineral");
        	return null;
        }
    }
    
    public void putMineral(int x, int y, String m) {
        if(y%2 == x%2)
            worldMap.get(x-1).get(y-1).setMineral(m);
        else System.out.println("Attempt to put a mineral in a non-existing cell");
    }

    public int getWidth() {
        return worldMap.get(1).size();
    }
    
    public int getHeight() {
        return worldMap.size();
    }
}


