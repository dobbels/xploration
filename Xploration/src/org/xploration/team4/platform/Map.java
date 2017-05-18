package org.xploration.team4.platform;

import org.xploration.ontology.Cell;
import org.xploration.team4.common.Constants;

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
		for(int i = 0; i<getHeight(); i++){
			
			for(int j = 0; j<getWidth(); j++){
				if (i%2 == j%2) {
					c = new Cell();
					c.setX(i);
					c.setY(j);
					worldMap.get(i).set(j, c);
				}
			}
		}
	}
	
	public void printWorldMap(){
		//Doesn't display the last mineral in the even lines. Because length is 10 not 9
		//But it stores the text file correctly
		for(int i = 0; i<getHeight(); i++){			
			for(int j = 0; j<getWidth(); j++){
				System.out.printf("%s ", worldMap.get(i).get(j).getMineral());
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
        return worldMap.get(y-1).get(x-1);
    }
    
    public String getMineral(int x, int y) throws Exception {
        if(y%2 == x%2)
            return worldMap.get(y-1).get(x-1).getMineral();
        else throw new Exception("Not a cell");
    }
    
    public void putMineral(int x, int y, String m) {
        if(y%2 == x%2)
            worldMap.get(y-1).get(x-1).setMineral(m);
        else System.out.println("Attempt to put a mineral in a non-existing cell");
    }

    public int getWidth() {
        return worldMap.get(1).size();
    }
    
    public int getHeight() {
        return worldMap.size();
    }
}


