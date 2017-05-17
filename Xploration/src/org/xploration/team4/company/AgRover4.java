package org.xploration.team4.company;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.platform.Map;
import org.xploration.team4.common.Constants;
import org.xploration.ontology.Team;

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

	//TODO It should not send the second or third request
	private int worldWidth;
	private int worldHeight;
	private int missionLength;
	
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;
	
	private Cell location = new Cell();

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
		String arg1 = (String) args[0]; // Landing of Capsule X-coordinate 
		String arg2 = (String) args[1]; // Landing of Capsule Y-coordinate 
		String arg3 = (String) args[2]; // WorldMap X dimension
		String arg4 = (String) args[3]; // WorldMap Y dimension
		String arg5 = (String) args[4]; // Mission length
		
		//Type conversions
		location.setX(Integer.parseInt(arg1));
		location.setY(Integer.parseInt(arg2));
		worldWidth = Integer.parseInt(arg3);
		worldHeight = Integer.parseInt(arg4);
		missionLength = Integer.parseInt(arg5);
		
		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);

		//Cell Analysis for Terrain Simulator 
		cellAnalysis(location);
		//roverRegistration for Map Simulator
		roverRegistration(location);
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

							Action cellAction = new Action(agTerrain, cellAnalysis);

							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

							msg.setLanguage(codec.getName());
							msg.setOntology(ontology.getName());
							try{
								getContentManager().fillContent(msg, cellAction);
								msg.addReceiver(agTerrain);
								send(msg);			                	
							}
							catch(Exception e){
								System.out.println(getLocalName() + " Request Exception");
							}
	
							System.out.println(getLocalName() + ": REQUEST is sent");
							//doWait(1000);

							//Returned answer from Terrain Simulation
							ACLMessage ans = blockingReceive();
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

									ACLMessage finalMsg = blockingReceive();
									if(finalMsg != null){
										if(finalMsg.getPerformative()==ACLMessage.INFORM)
										{										
											System.out.println(getLocalName()+": INFORM is received!");
											System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
													+myCell.getX() + ","+ myCell.getY()+  ", " + myCell.getMineral() + ")");
											claimingCell = true;											
										}
										else{
											System.out.println(getLocalName() + " A problem occured, it should be informed");
										}
									}
									else{//If no message arrives
										block();
									}
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

							Action cellAction = new Action(agMapSimulator, roverReg);

							ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
							
							msg.setProtocol(XplorationOntology.ROVERREGISTRATIONSERVICE);
							msg.setLanguage(codec.getName());
							msg.setOntology(ontology.getName());
							msg.addReceiver(agMapSimulator);
							try{
								getContentManager().fillContent(msg, cellAction);						
								send(msg);	
								System.out.println(getLocalName() + ": INFORM is sent");
								roverRegistration = true;
							}
							catch(Exception e){
								System.out.println(getLocalName() + " INFORM Exception");
							}												
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
}



