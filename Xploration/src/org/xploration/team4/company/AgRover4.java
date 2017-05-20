package org.xploration.team4.company;

import java.util.ArrayList;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;
import org.xploration.ontology.Team;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


public class AgRover4 extends Agent {

	//TODO It should not send the second or third request
	private int worldWidth;
	private int worldHeight;
	private int missionLength;
	
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;
	
	private Cell location = new Cell();
	private ArrayList<Cell> analyzedCells = new ArrayList<>();

	private Cell cell1 = new Cell();
	private Cell cell2 = new Cell();


	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();

	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		Object[] args = getArguments();
		//Type needed to be changed into String
		//Integer type causes program to be crashed
		int arg1 = (int) args[0]; // Landing of Capsule X-coordinate 
		int arg2 = (int) args[1]; // Landing of Capsule Y-coordinate 
		int arg3 = (int) args[2]; // World map X dimension
		int arg4 = (int) args[3]; // World map Y dimension
		int arg5 = (int) args[4]; // the mission length
		
		//Type conversions
		location.setX(arg1);
		location.setY(arg2);
		worldWidth = arg3;
		worldHeight = arg4;
		missionLength = arg5;
		
		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		
		//TODO delete these two cells, they are meant for testing
		cell1.setX(4);
		cell1.setY(6);
		cell1.setMineral("A");
	
		cell2.setX(1);
		cell2.setY(3);
		cell2.setMineral("C");
		
		analyzedCells.add(cell1);
		analyzedCells.add(cell2);
		
		//Cell Analysis for Terrain Simulator 
		cellAnalysis(location);
		//roverRegistration for Map Simulator
//		roverRegistration(location);
		// map broadcast //TODO eventually before/after every movement
//		broadcastCurrentMap(analyzedCells); //TODO
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
					sd.setType(Constants.TERRAIN_SIMULATOR);
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
							System.out.println(getLocalName()+ ": terrain simulator agent is found");


							CellAnalysis cellAnalysis = new CellAnalysis();
							cellAnalysis.setCell(myCell);
							
							ACLMessage msg = MessageHandler.constructMessage(agTerrain, ACLMessage.REQUEST, cellAnalysis, XplorationOntology.CELLANALYSIS);
							send(msg);			                	
	
							System.out.println(getLocalName() + ": REQUEST is sent");
							//doWait(1000);

							//Returned answer from Terrain Simulation
							ACLMessage ans = MessageHandler.blockingReceive(myAgent, XplorationOntology.CELLANALYSIS);
							if(ans!= null){	  
								if(ans.getPerformative()==ACLMessage.REFUSE)
								{
									System.out.println(getLocalName() + ": REFUSED due to Invalid Cell");
									claimingCell = true;
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									claimingCell = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE)
								{
									System.out.println(getLocalName() + ": Initial AGREE is received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, ACLMessage.INFORM, XplorationOntology.CELLANALYSIS);
									
									System.out.println(getLocalName()+": INFORM is received!");
									System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
											+myCell.getX() + ","+ myCell.getY()+  ", " + myCell.getMineral() + ")");
									claimingCell = true;											
							
								}						  						  						  
							}else{
								//If no message arrives
								block();
							}

						}else{
							System.out.println(getLocalName() + ": No terrain simulator found in yellow pages yet.");
							doWait(5000);
						}

					}catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
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
	
	private void roverRegistration(Cell myCell){	
		addBehaviour (new SimpleBehaviour(this)
		{	
			private static final long serialVersionUID1 = 2L;

			AID agMapSimulator;

			private boolean roverRegistration = false;
			
			public void action(){
				//A defensive check
				if(!roverRegistration){
					//Creates description for the AGENT MAP SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					
					sd.setType(Constants.MAP_SIMULATOR);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agMapSimulator = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": Map Simulator agent is found");

							
							RoverRegistrationInfo roverReg = new RoverRegistrationInfo();
							roverReg.setCell(myCell);
							//TODO Type should be integer or team
							roverReg.setTeam(Constants.myTeam);

							ACLMessage msg = MessageHandler.constructMessage(agMapSimulator, ACLMessage.REQUEST, roverReg, XplorationOntology.ROVERREGISTRATIONINFO);
							send(msg);	
							System.out.println(getLocalName() + ": INFORM is sent");
							roverRegistration = true;
						}
						else{
							System.out.println(getLocalName() + ": No map simulator found in yellow pages yet.");
							doWait(5000);
						}
					}
					catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}
				
			}
			//To stop behaviour
			public boolean done() {
				return roverRegistration;
			}
		});
	}
	
	// This behaviour broadcasts a map to every rover in range. The behaviour will be started after every movement.  
	private void broadcastCurrentMap(ArrayList<Cell> cells){

//		addBehaviour (new OneShotBehaviour(this)
//		{						  			
//			private static final long serialVersionUID = 1L;
//
//			AID agCommunication;
//
//			public void action(){
//
//				//Creates description for the AGENT TERRAIN SIMULATOR to be searched
//				DFAgentDescription dfd = new DFAgentDescription();     
//				ServiceDescription sd = new ServiceDescription();
//				sd.setType(Constants.COMMUNICATION_SIMULATOR);
//				dfd.addServices(sd);
//
//				try {
//					// It finds agents of the required type
//					DFAgentDescription[] result = new DFAgentDescription[20];
//					result = DFService.search(myAgent, dfd);
//
//					// Gets the first occurrence, if there was success
//					if (result.length > 0)
//					{
//						//System.out.println(result[0].getName());
//						agCommunication = (AID) result[0].getName();	
//						
//						//TODO change from here on 
//						
//						System.out.println(getLocalName()+ ": terrain simulator agent is found");
//
//
////						CellAnalysis cellAnalysis = new CellAnalysis();
////						cellAnalysis.setCell(myCell);
//
////						Action cellAction = new Action(agTerrain, cellAnalysis);
//
//						ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//
//						msg.setLanguage(codec.getName());
//						msg.setOntology(ontology.getName());
//						try{
//							getContentManager().fillContent(msg, cellAction);
//							msg.addReceiver(agCommunication);
//							send(msg);			                	
//						}
//						catch(Exception e){
//							System.out.println(getLocalName() + " Request Exception");
//						}
//
//						System.out.println(getLocalName() + ": REQUEST is sent");
//						//doWait(1000);
//
//						//Returned answer from Terrain Simulation
//						ACLMessage ans = blockingReceive();
//						if(ans!= null){	  
//							if(ans.getPerformative()==ACLMessage.REFUSE)
//							{
//								System.out.println(getLocalName() + ": REFUSED due to Invalid Cell");
//								claimingCell = true;
//							}
//
//							else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
//							{
//								System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
//								claimingCell = true;
//							}
//							else if(ans.getPerformative()== ACLMessage.AGREE)
//							{
//								System.out.println(getLocalName() + ": Initial AGREE is received");	  
//
//								ACLMessage finalMsg = blockingReceive();
//								if(finalMsg != null){
//									if(finalMsg.getPerformative()==ACLMessage.INFORM)
//									{										
//										System.out.println(getLocalName()+": INFORM is received!");
//										System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
//												+myCell.getX() + ","+ myCell.getY()+  ", " + myCell.getMineral() + ")");
//										claimingCell = true;											
//									}
//									else{
//										System.out.println(getLocalName() + " A problem occured, it should be informed");
//									}
//								}
//								else{//If no message arrives
//									block();
//								}
//							}						  						  						  
//						}else{
//							//If no message arrives
//							block();
//						}
//
//					}else{
//						System.out.println(getLocalName() + ": No terrain simulator found in yellow pages yet.");
//						doWait(5000);
//					}
//
//				}catch(Exception e){
//					System.out.println(getLocalName() + "Exception is detected!");
//					e.printStackTrace();
//				}				
//			}
//		});

	}
	
	private void listenForMaps(){

		addBehaviour (new CyclicBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				// TODO listen for an inform of the mapbroadcast protocol from the communication simulator and store the cells you didn't know about yet
			}
			
		});

	}
}



