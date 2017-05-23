package org.xploration.team4.platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
	
	public static int getProperty(String property) {
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("./src/org/xploration/team4/platform/xploration.properties");

			// load a properties file
			prop.load(input);
			
			return Integer.parseInt(prop.getProperty(property));
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return -1;
	}
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {

		Properties prop = new Properties();
		InputStream input = null;
	
		try {
	
			input = new FileInputStream("./src/org/xploration/team4/platform/xploration.properties");
	
			// load a properties file
			prop.load(input);
	
			// get the property value and print it out
			System.out.println(prop.getProperty("MISSION_LENGTH"));
			System.out.println(prop.getProperty("RADIO_RANGE"));
			System.out.println(prop.getProperty("REGISTRATION_WINDOW"));
			System.out.println(prop.getProperty("ANALYSIS_TIME"));
			System.out.println(prop.getProperty("MOVEMENT_TIME"));
	
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}