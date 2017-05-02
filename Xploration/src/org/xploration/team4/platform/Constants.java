package org.xploration.team4.platform;

import org.xploration.ontology.Cell;

public class Constants {
	// The name of the service the Registration Desk agent is using to announce itself in the Yellow Pages
	public final static String REGISTRATION_DESK_NAME = "registrationDesk";
	
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
			System.out.println("rand X: " + randomX);
			System.out.println("rand Y: " + randomY);
		}
		
		Cell cell = new Cell();
		cell.setX(randomX);
		cell.setY(randomY);
		
		return cell;
	}
}
