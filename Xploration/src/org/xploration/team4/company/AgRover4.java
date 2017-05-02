package org.xploration.team4.company;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.XplorationOntology;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgRover4 extends Agent {

	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;
	//The name of the Terrain Simulator Agent in the DF yellow pages
	public final static String TERRAIN_SIMULATOR = "TerrainSimulator4";

	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();

	//Cell object to claim by Rover
	public Cell myCell = new Cell();
	//Example Coordinates
	private int xCoord = 3;
	private int yCoord = 3;

	protected void setup(){

		System.out.println(getLocalName() + " HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//Just a trial for (3,3) coordinates
		myCell.setX(xCoord);
		myCell.setY(yCoord);

		//Behaviour is added in the cellAnalysis Method
		cellAnalysis(myCell);
	} 

	private void cellAnalysis(Cell myCell){

		addBehaviour (new SimpleBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			AID agTerrain;

			private boolean claimingCell = false;

			public void action(){

				//A defensive check
				if(!claimingCell){
					//Creates description for the AGENT TERRAIN SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(TERRAIN_SIMULATOR);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);
						
						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agTerrain = (AID) result[0].getName();						  

							//REQUEST is sent	
							//MISSING Parts I need to use Cell(x,y)
							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
							msg.setContent("Request for Cell");
							msg.addReceiver(agTerrain);
							send(msg);
							System.out.println("REQUEST is sent");
							
							
							//Returned answer from Terrain Simulation
							ACLMessage ans = receive();
							if(ans!= null){	  
								if(ans.getPerformative()==ACLMessage.REFUSE)
								{
									System.out.println("REFUSED due to Invalid Cell");
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println("NOT UNDERSTOOD the message");
								}
								else if(ans.getPerformative()== ACLMessage.AGREE)
								{
									System.out.println("Initial AGREE");	  

									ACLMessage ansFinal = receive();
									if(ansFinal.getPerformative()==ACLMessage.INFORM)
									{										
										System.out.println("Claiming Cell is successful");
										System.out.println(myAgent.getLocalName()+ "has received: "+ myCell.getMineral()+ " minerals"
												+" from (" +myCell.getX() + ","+ myCell.getY()+ ") Cell");
										claimingCell = true;
										//Update of the Cell 
										myCell.setMineral(" ");												
									}
									else{
										System.out.println("A problem occured, it should be informed");
									}
								}						  						  						  
							}

						}else{
							System.out.println("Search returns NULL");
							doWait(5000);
						}

					}catch(Exception e){
						System.out.println("Exception is detected!");
					}
				}				
			}

			//To stop behaviour
			public boolean done() {
				//Cell is claimed
				return claimingCell;
			}        	       	

		});

	}	
}	

