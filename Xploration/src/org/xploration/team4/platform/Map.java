package org.xploration.team4.platform;

import org.xploration.ontology.Cell;
import java.util.*;

public class Map {
		
	public int m,n;
	private Cell [][] multi;
	
	//Creating all the cell objects within the map
	public Map(int width, int height){
		multi = new Cell [height][width];
		for(int i = 0; i < height;i ++ ){
			for(int j = 0; j<width ; j++){
				Cell temp =new Cell();
                temp.setX(i+1);
                temp.setY(j+1);
                temp.setMineral("A");//TODO read from file
				multi[i][j] = temp;
			}
		}
		//Map 3X3 sizes with A,B,C,D minerals
//		multi[0][0].setMineral(""); multi[0][1].setMineral(""); multi[0][2].setMineral(""); multi[0][3].setMineral("");
//		multi[1][0].setMineral(""); multi[1][1].setMineral("A"); multi[1][2].setMineral(""); multi[1][3].setMineral("C");
//		multi[2][0].setMineral(""); multi[2][1].setMineral(""); multi[2][2].setMineral("B"); multi[2][3].setMineral(""); 
//		multi[3][0].setMineral(""); multi[3][1].setMineral("D"); multi[3][2].setMineral(""); multi[3][3].setMineral("B");
	}
	

    public Cell getCell(int x, int y) throws Exception {
        if(x%2 == y%2)
            return multi[x-1][y-1];
        else throw new Exception("Not a cell");
    }

    public void putMineral(int x, int y, String m) throws Exception {
        if(x%2 == y%2)
            multi[x-1][y-1].setMineral(m);
        else throw new Exception("Not a cell");
    }

    public int getWidth() {
        return multi[0].length;
    }


    public int getHeight() {
        return multi.length;
    }
}


