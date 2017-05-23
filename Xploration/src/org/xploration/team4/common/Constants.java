package org.xploration.team4.common;

import org.xploration.ontology.Cell;
import org.xploration.ontology.Team;
import org.xploration.team4.platform.PropertyReader;

import jade.util.leap.Properties;

import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.Team;

public class Constants {
	
	public final static int TEAM_ID = 4;

	//TODO use in all files, instead of local variables
	public final static int MISSION_LENGTH = PropertyReader.getProperty("MISSION_LENGTH"); // in seconds
	public final static int COMMUNICATION_RANGE = PropertyReader.getProperty("RADIO_RANGE"); // number of cells
	public final static int REGISTRATION_WINDOW = PropertyReader.getProperty("REGISTRATION_WINDOW"); // in seconds
	public final static int ANALYSIS_TIME = PropertyReader.getProperty("ANALYSIS_TIME"); // in seconds
	public final static int MOVEMENT_TIME = PropertyReader.getProperty("MOVEMENT_TIME"); // in seconds
	
	public final static boolean isExistingCoordinate(int dimX, int dimY, int x, int y) {
		return (x > 0 && x <= dimX && y > 0 && y <= dimY &&
				((x%2 == 0 && y%2 == 0) || (x%2 == 1 && y%2 == 1)));
	}
	
	public final static Cell generateCoordinate(int dimX, int dimY) {
		int randomX = 0;
		int randomY = 0;
		
		System.out.println("");
		while (!isExistingCoordinate(dimX, dimY, randomX, randomY)) {
			randomX = 1 + (int)(Math.random() * dimX);
			randomY = 1 + (int)(Math.random() * dimY);
		}
		
		Cell cell = new Cell();
		cell.setX(randomX);
		cell.setY(randomY);
		
		return cell;
	}
	
}
