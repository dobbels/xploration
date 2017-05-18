package org.xploration.team4.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.xploration.ontology.Cell;
import org.xploration.team4.common.Map;

public class MapReader {
	
	private final static String MAP_FILE = "./src/org/xploration/team4/platform/MAP_FILE.txt";
	
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
					if (!(text.length() == dimY))
						System.out.println("Inconsistent map dimensions !!");
					for(int y = 1; y <=text.length(); y++){
						if (!(map.getCell(x, y) == null)) {
							if (text.substring(y-1, y).equals(" "))
								System.out.println("Parsing error");
							map.putMineral(x, y, text.substring(y-1, y));
						}
					}
					x++;
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

}
