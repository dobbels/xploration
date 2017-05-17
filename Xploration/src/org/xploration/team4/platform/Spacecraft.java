package org.xploration.team4.platform;

import java.util.*;
import org.xploration.team4.common.Constants;

import org.xploration.ontology.Cell;
import org.xploration.ontology.XplorationOntology;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.*;
import jade.core.behaviours.OneShotBehaviour;
import jade.wrapper.AgentController;
/*
 * Capsules creation
Therefore the names of all the capsule classes will be in the same format CapsuleX.java, where X is the team number. CapsuleX.java is located at the top level of the platform folder. 
At the creation of the Capsule, the Spacecraft will provide the necessary information as arguments in this particular order:

Landing of Capsule X-coordinate 
Landing of Capsule Y-coordinate 
Dimension of world X
Dimension of world Y
 */
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/*
 * ------------------------------ Test case Id: 1
 * 
 *     Goal of the test: Check if the spacecraft successfully creates the capsule
 *     Input: Capsule release coordinates
 *     Expected output: Print to console from Capsule
 * 
 * ------------------------------ Test case Id: 2
 * 
 *     Goal of the test: check if the spacecraft successfully releases the capsule.
 *     Input: Rover release coordinates
 *     Expected output: Print the releasing coordinates to console
 */
public class Spacecraft extends Agent {
		// private static final long serialVersionUID = 1L;
		
		private static final Ontology ontology = XplorationOntology.getInstance();
		private Codec codec = new SLCodec();
		
		private int mapDimensionX = 10;
		private int mapDimensionY = 10;
		private int missionLength = 10; // mission length in seconds
		
		
		public void setup() {
			System.out.println(getLocalName() + ": HAS ENTERED");
			// Starting time for registration duration

			getContentManager().registerLanguage(codec);
			getContentManager().registerOntology(ontology);
			
			addBehaviour(createCapsule());
		}

		private OneShotBehaviour createCapsule() {
			return new OneShotBehaviour(this) {

				private static final long serialVersionUID = -3422396939743596002L;

				public void action() {
					// Create 4 capsules with random initial coordinates
					ArrayList<Cell> assignedCells = new ArrayList<>();
					ArrayList<AgentController> agents = new ArrayList<>();
					Cell currentCell;
					boolean alreadyAssigned = false;
					
					//Get the JADE runtime interface (singleton)
					jade.core.Runtime runtime = jade.core.Runtime.instance();
					//Create a Profile, where the launch arguments are stored
					Profile profile = new ProfileImpl();
					profile.setParameter(Profile.CONTAINER_NAME, "CapsuleContainer");
					//create a non-main agent container
					ContainerController container = runtime.createAgentContainer(profile);
					
					while (assignedCells.size() < 4) {
						currentCell = Constants.generateCoordinate(mapDimensionX, mapDimensionY);
						for (Cell cell : assignedCells) {
							if (cell.getX() == currentCell.getX() && cell.getY() == currentCell.getY()){
								alreadyAssigned = true;
							}
						}
						
						if (!alreadyAssigned){
							assignedCells.add(currentCell);
							try {
								int teamNb = agents.size() + 1;
								String teamName = "Capsule" + teamNb;
								String className = "org.xploration.team" + teamNb + ".company."+ teamName;
//								String className = "org.xploration.team" + 4 + ".company."+ teamName;
								Object[] args = new Object[5];
								args[0] = currentCell.getX();
								args[1] = currentCell.getY();
								args[2] = mapDimensionX;
								args[3] = mapDimensionY;
								args[4] = missionLength;
						        agents.add(container.createNewAgent(teamName,className, args));
						        alreadyAssigned = false;
							} catch (StaleProxyException e) {
							    e.printStackTrace();
							}
						}
					}
					
					// Start up agents
					for (int i = 3 ; i >= 0 ; i--){
						try {
							agents.get(i).start(); // Our team first muhahaha
						} catch (StaleProxyException e) {
						    e.printStackTrace();
						}
						
					}
					
				}
			};
		}
	}
