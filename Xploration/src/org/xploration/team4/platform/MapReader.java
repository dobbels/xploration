package org.xploration.team4.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.xploration.ontology.Cell;
import org.xploration.team4.common.Map;

public class MapReader {
	
	private final static String MAP_FILE = "./src/org/xploration/team4/platform/map2.txt";
	
	//worldMap construction from a read txt file
	public static Map readMap() {
		
		File file = new File(MAP_FILE);
		BufferedReader reader = null;
		int dimX = 0;
		int dimY = 0;
		boolean dimensionsRead = false;
		Map map = null;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			String text = null;	
			//text has the lines
			int x = 1;
			
			
			
			while ((text = reader.readLine()) != null)
			{	
				if (!text.equals("")) { // ignore white lines
					if (!dimensionsRead) {
						String[] dims = text.split(",");
						dims[0] = dims[0].substring(1);
						dims[1] = dims[1].substring(0, dims[1].length()-1);
						dimX = Integer.parseInt(dims[0]);
						dimY = Integer.parseInt(dims[1]);
						map = new Map(dimX, dimY);
						dimensionsRead = true;
					} 
					else {
						if (x%2 == 1) {
							for(int y = 1; y <=text.length(); y++){
								if (!(map.getCell(x, y) == null)) {
									if (text.substring(y-1, y).equals(" "))
										System.out.println("Parsing error");
									map.putMineral(x, y, text.substring(y-1, y));
								}
							}
						}
						else {
							for(int y = 1; y <=text.length(); y++){
								if (!(map.getCell(x, y+1) == null)) {
									if (text.substring(y-1, y).equals(" "))
										System.out.println("Parsing error");
									map.putMineral(x, y+1, text.substring(y-1, y));
								}
							}
						}
						
						x++;
					}
				}
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
		return map;
	}
	
	//To test the parsing
	public static void main(String[] args) {
		
		Map m = readMap();
		
		m.printWorldMap();
		
		ArrayList<Integer> toGoBack = new ArrayList<>();
		int i;
		for (i = 0 ; i <=10 ; i++) {
			toGoBack.add(i);
		}
		
//		ArrayList<Integer> thereAndBack = new ArrayList<>(toGoBack);
//
//		System.out.println(thereAndBack);
//
//		Collections.reverse(toGoBack);
//		toGoBack.remove(0);
//		thereAndBack.addAll(toGoBack);
//		
//		System.out.println(thereAndBack);
//		
//		Cell c1 = new Cell();
//		Cell c2 = new Cell();
//		c1.setX(1);
//		c1.setY(3);
//		c2.setX(1);
//		c2.setY(3);
//		
//		System.out.println(c1);
//		System.out.println(c2);
//		System.out.println(c1.equals(c2));
//		
//		System.out.println();
//		System.out.println(test.toString());
//		test.remove(0);
//		System.out.println(test.toString());
//		
//		System.out.println(test.isEmpty());
//		test.clear();
//		System.out.println(test);
//		System.out.println(test.isEmpty());
	}

}
