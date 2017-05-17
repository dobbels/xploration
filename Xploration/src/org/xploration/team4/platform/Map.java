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
	//File Location
	private final static String MAP_FILE = "C:\\Users\\asus\\git\\xploration\\Xploration\\src\\org\\xploration\\MAP_FILE.txt";
			
	//worldMap construction from a read txt file
	public Map(){
		
		File file = new File(MAP_FILE);
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(file));
			String text = null;		    		    
			//text has the lines
			while ((text = reader.readLine()) != null)
			{	
				int row = 0;
				ArrayList<Cell> myRow = new ArrayList<Cell>();		    	
				for(int i = 0; i <text.length(); i++){
					//Filling the row
					Cell myCell = new Cell();
					myCell.setMineral(text.substring(i, i+1));
					myCell.setX(row);
					myCell.setY(i);
					myRow.add(myCell);
				}				
				//Filling the 2D arrayList
				worldMap.add(myRow);
				row ++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 

		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {}
		}
	}
	public void printWorldMap(){
		//Doesn't display the last mineral in the even lines. Because length is 10 not 9
		//But it stores the text file correctly
		for(int i = 0; i<worldMap.size(); i++){			
			for(int j = 0; j<worldMap.get(0).size(); j++){
				System.out.printf("%s ", worldMap.get(i).get(j).getMineral());
			}
			System.out.println();
		}		
	}

    public Cell getCell(int x, int y) throws Exception {
        if(x%2 == y%2)
            return worldMap.get(x-1).get(y-1);
        else throw new Exception("Not a cell");
    }
    
    public String getMineral(int x, int y) throws Exception {
        if(x%2 == y%2)
            return worldMap.get(x-1).get(y-1).getMineral();
        else throw new Exception("Not a cell");
    }
    
    public void putMineral(int x, int y, String m) throws Exception {
        if(x%2 == y%2)
            worldMap.get(x-1).get(y-1).setMineral(m);
        else throw new Exception("Not a cell");
    }

    public int getWidth() {
        return worldMap.size();
    }
    
    public int getHeight() {
        return worldMap.get(1).size();
    }
}


