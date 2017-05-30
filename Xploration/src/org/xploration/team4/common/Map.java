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
    
    public Cell calculateNextPosition(int ix, int iy, String direction) {
    	int newx = 0, newy = 0;
    	Cell c = new Cell();
		//move up
    	if (direction.equals("up")) {
    		newx = ix - 2;
    		newy = iy;
    	}
		//move down
    	if (direction.equals("down")) {
			newx = ix + 2;
			newy = iy;
    	}
		//move left up
    	if (direction.equals("leftUp")) {
			newx = ix - 1;
			newy = iy - 1;
    	}
		//move left down
    	if (direction.equals("leftDown")) {
			newx = ix + 1;
			newy = iy - 1;
    	}
		//move right up
    	if (direction.equals("rightUp")) {
			newx = ix - 1;
			newy = iy + 1;
    	}
		//move right down
    	if (direction.equals("rightDown")) {
			newx = ix + 1;
			newy = iy + 1;
    	}
		
		if (newx <= 0 || newx > getWidth()) {
			newx = Math.abs(Math.abs(newx) - getWidth());
		}
		
		if (newy <= 0 || newy > getHeight()) {
			newy = Math.abs(Math.abs(newy) - getHeight());
		}
		
		c.setX(newx);
		c.setY(newy);
		
		return c;
	}
    
    public boolean compareCelltoDestination(Cell dest, int dx, int dy) {
    	return dest.getX() == dx && dest.getY() == dy;
    }
    
    public boolean isNextPosition(int ix, int iy, int dx, int dy) {
    	Cell c = calculateNextPosition(ix, iy, "up");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	c = calculateNextPosition(ix, iy, "down");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	c = calculateNextPosition(ix, iy, "leftUp");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	c = calculateNextPosition(ix, iy, "leftDown");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	c = calculateNextPosition(ix, iy, "rightUp");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	c = calculateNextPosition(ix, iy, "rightDown");
    	if (compareCelltoDestination(c, dx, dy)) return true;
    	
    	return false;
    }
    
    public boolean inRangeFrom(Cell rover, Cell other, int comRange) {
    	// In these calculations it is assumed that the map is spherical, so
		// from the left side, you can go directly to the rigth side and so on. 
    	int x = rover.getX();
    	int y = rover.getY();
    	
    	int x_other = other.getX();
    	int y_other = other.getY();
    	
    	int distance = distance(x,y,x_other,y_other);
    	
    	return (0 <= distance && distance <= comRange);
	}
	
	public int distance(int x, int y, int x_other, int y_other) {
        int rightDiff = (getWidth() + y_other - y) % getWidth();
        int leftDiff = (getWidth() + y - y_other) % getWidth();
        int upDiff = (getHeight() + x - x_other) % getHeight();
        int downDiff = (getHeight() + x_other - x) % getHeight();

        int distY = Math.min(rightDiff, leftDiff);
        int distX = Math.min(upDiff, downDiff);

        return distY + Math.max(0, (distX - distY) / 2);
    }
}


