package org.xploration.team4.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.xploration.ontology.Cell;

public class MapReader {
	
	private final static String MAP_FILE = "C:\\Users\\asus\\git\\xploration\\Xploration\\src\\org\\xploration\\MAP_FILE.txt";
	
	//worldMap construction from a read txt file
	public static Map readMap() {
		
		File file = new File("./src/org/xploration/team4/platform/MAP_FILE.txt");
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
					dims[1] = dims[1].substring(0, dims[1].length());
					dimX = Integer.parseInt(dims[0]);
					dimY = Integer.parseInt(dims[1]);
					dimensionsRead = true;
					map = new Map(dimX, dimY);
				} 
				else {
					if (!(text.length() == dimY))
						System.out.println("Inconsistent map dimensions !!");
					for(int y = 0; y <text.length(); y++){
						if (!(map.getCell(x, y) == null)) {
							if (text.substring(y, y+1).equals(" "))
								System.out.println("Parsing error");
							map.putMineral(x, y, text.substring(y, y+1));
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

	public static void main(String[] args) {
		Map map = readMap();
		map.printWorldMap();
//		{
//	        File curDir = new File("./src/org");
//	        getAllFiles(curDir);
//	    }
		
	}
	private static void getAllFiles(File curDir) {

        File[] filesList = curDir.listFiles();
        for(File f : filesList){
            if(f.isDirectory())
                System.out.println(f.getName());
            if(f.isFile()){
                System.out.println(f.getName());
            }
        }

    }

}
