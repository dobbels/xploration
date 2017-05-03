package org.xploration.team4.platform;

import org.xploration.ontology.Cell;
import java.util.*;

public class Map {
		
	public int m,n;
	private Cell [][] multi = new Cell [4][4];
	
	//Creating all the cell objects within the map
	public Map(){		
		for(int i = 0; i < 4;i ++ ){
			for(int j = 0; j<4 ; j++){
				multi[i][j] = new Cell();
			}
		}
		//Map 3X3 sizes with A,B,C,D minerals
		multi[0][0].setMineral(""); multi[0][1].setMineral(""); multi[0][2].setMineral(""); multi[0][3].setMineral("");
		multi[1][0].setMineral(""); multi[1][1].setMineral("A"); multi[1][2].setMineral(""); multi[1][3].setMineral("C");
		multi[2][0].setMineral(""); multi[2][1].setMineral(""); multi[2][2].setMineral("B"); multi[2][3].setMineral(""); 
		multi[3][0].setMineral(""); multi[3][1].setMineral("D"); multi[3][2].setMineral(""); multi[3][3].setMineral("B");
	}
	
	public Cell getCell(int m, int n){
		return multi[m][n];
	}		
}


