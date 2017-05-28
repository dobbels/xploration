package org.xploration.team4.company;

import java.util.ArrayList;
import java.util.Iterator;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
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
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;


public class AgRover4 extends Agent {

	private int worldDimY;
	private int worldDimX;
	private int missionLength;
	private int communicationRange;
	
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;

	private Cell location = new Cell();
	private ArrayList<Cell> analyzedCells = new ArrayList<>();

	private Map localWorldMap; 

	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();

	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		Object[] args = getArguments();
		int arg1; 
		int arg2; 
		int arg3;
		int arg4;
		int arg5;
		int arg6;
		
		if (args[0] instanceof String) { // To be able to pass arguments in command line
			arg1 = Integer.parseInt((String) args[0]); // Landing of Capsule X-coordinate
			arg2 = Integer.parseInt((String) args[1]); // Landing of Capsule Y-coordinate 
			arg3 = Integer.parseInt((String) args[2]); // World map X dimension
			arg4 = Integer.parseInt((String) args[3]); // World map Y dimension
			arg5 = Integer.parseInt((String) args[4]); // the mission length
			arg6 = Integer.parseInt((String) args[5]); // communication range
		}
		else {
			arg1 = (int) args[0]; // Landing of Capsule X-coordinate 
			arg2 = (int) args[1]; // Landing of Capsule Y-coordinate 
			arg3 = (int) args[2]; // World map X dimension
			arg4 = (int) args[3]; // World map Y dimension
			arg5 = (int) args[4]; // the mission length
			arg6 = (int) args[5]; // communication range
		}
				
		//Type conversions
		location.setX(arg1);
		location.setY(arg2);
		worldDimX = arg3;
		worldDimY = arg4;
		missionLength = arg5;
		communicationRange = arg6;
		
		localWorldMap = new Map(worldDimX, worldDimY);

		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		System.out.println(getLocalName()+": communicationRange: "+ arg6);
		
		//roverRegistration for Map Simulator
	    roverRegistration(location);	    
//		claimCell();
//		claimCell();
		startMainBehaviour();
	} 
	
	private void startMainBehaviour() {
		
	}

	private void analyzeCell(Cell myCell){

		addBehaviour (new SimpleBehaviour(this) //TODO can also be OneShotBehaviour?
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
					sd.setType(XplorationOntology.TERRAINSIMULATOR);
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
									System.out.println(getLocalName() + ": Initial AGREE was received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, XplorationOntology.CELLANALYSIS);

									switch (finalMsg.getPerformative()) {
									case ACLMessage.INFORM:
										System.out.println(getLocalName()+": INFORM is received!");

										ContentElement ce;
										try {
											ce = getContentManager().extractContent(finalMsg);

											// We expect an action inside the message
											if (ce instanceof Action) {
												Action agAction = (Action) ce;
												Concept conc = agAction.getAction();

												if (conc instanceof CellAnalysis) {
													Cell cell = ((CellAnalysis) conc).getCell();
													//TODO set mineral in our representation of map. Is this the way to go?
													analyzedCells.add(cell);
													localWorldMap.setCell(cell);
													System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
															+cell.getX() + ","+ cell.getY()+  ", " + cell.getMineral() + ")");
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										claimingCell = true;	

										// map broadcast
										broadcastCurrentMap(analyzedCells);

										//Test if we get failure to this message. TODO delete
										Cell notLocation = localWorldMap.calculateNextPosition(location.getX(), location.getY(), "up");
										notLocation.setX(notLocation.getX());
										notLocation.setY(notLocation.getY());
										analyzeCell(notLocation);

										break;
									case ACLMessage.FAILURE:
										System.out.println(getLocalName()+": FAILURE was received!");
										claimingCell = true;
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

					sd.setType(XplorationOntology.ROVERREGISTRATIONSERVICE);
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
							Team team = new Team();
							team.setTeamId(Constants.TEAM_ID);
							roverReg.setTeam(team);

							ACLMessage msg = MessageHandler.constructMessage(agMapSimulator, ACLMessage.INFORM, roverReg, XplorationOntology.ROVERREGISTRATIONINFO);
							send(msg);	
							System.out.println(getLocalName() + ": INFORM is sent");
							roverRegistration = true;

							listenForMaps();

							//UNDER COMMENTS Because it causes some problems
//							requestMovement(); // TODO analyze cell happens in this behaviour for testing
							//analyzeCell(location);
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

	// This behaviour broadcasts a map to every rover in range. The behaviour will be started after every movement/analyzing of cell/5 seconds (to be decided).  
	private void broadcastCurrentMap(ArrayList<Cell> cells){

		addBehaviour (new OneShotBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			AID agCommunication;

			public void action(){

				//Creates description for the AGENT TERRAIN SIMULATOR to be searched
				DFAgentDescription dfd = new DFAgentDescription();     
				ServiceDescription sd = new ServiceDescription();
				sd.setType(XplorationOntology.MAPBROADCASTSERVICE);
				dfd.addServices(sd);

				try {
					// It finds agents of the required type
					DFAgentDescription[] result = new DFAgentDescription[20];
					result = DFService.search(myAgent, dfd);

					// Gets the first occurrence, if there was success
					if (result.length > 0)
					{
						//System.out.println(result[0].getName());
						agCommunication = (AID) result[0].getName();	

						//TODO change from here on 

						System.out.println(getLocalName()+ ": map broadcast service is found");

						if (!analyzedCells.isEmpty()) {
							MapBroadcastInfo mbi = new MapBroadcastInfo();
							org.xploration.ontology.Map map = new org.xploration.ontology.Map();

							for (Cell c : analyzedCells) {
								map.addCellList(c);
							}
							mbi.setMap(map);

							ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, mbi, XplorationOntology.MAPBROADCASTINFO);
							send(msg);			                	

							System.out.println(getLocalName() + ": map broadcast INFORM is sent");
							//doWait(1000);
						}
						else {
							System.out.println(getLocalName() + ": No analyzed cells to broadcast yet.");
						}
					}else{
						System.out.println(getLocalName() + ": No map broadcast service found yet.");
						doWait(5000);
					}

				}catch(Exception e){
					System.out.println(getLocalName() + "Exception is detected!");
					e.printStackTrace();
				}				
			}
		});

	}

	private void listenForMaps(){

		addBehaviour (tbf.wrap(new CyclicBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {				
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO);

				if (msg != null) {
					System.out.println(getLocalName() + ": received map broadcast");

					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);

						if (ce instanceof Action) {
							Concept conc = ((Action) ce).getAction();

							if (conc instanceof MapBroadcastInfo) {
								MapBroadcastInfo mbi = (MapBroadcastInfo) conc;
								org.xploration.ontology.Map map = mbi.getMap();
								Iterator it = map.getAllCellList();
								Cell c;
								while (it.hasNext()) {
									c = (Cell) it.next();
									localWorldMap.setCell(c);
								}
								System.out.println(getLocalName() + ": new local world map");
//								localWorldMap.printWorldMap();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}			
		}));

	}

	private void requestMovement() { //TODO here we probably add an argument 'cell' and take the decision of where to go outside?!
		addBehaviour(new SimpleBehaviour(this) {
			private static final long serialVersionUID = 1L;
			AID agMovementSim;
			private boolean movementRequested = false;
			@Override
			public void action() {
				// TODO Auto-generated method stub
				if (!movementRequested) {
					//Creates description for the AGENT TERRAIN SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.MOVEMENTREQUESTSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0) {
							//System.out.println(result[0].getName());
							agMovementSim = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": movement simulator agent is found");

							MovementRequestInfo mri = new MovementRequestInfo();
							Cell destination = localWorldMap.calculateNextPosition(location.getX(), location.getY(), "up");
							//TODO uncomment when you want to test sprint 3.6
							//							destination = localWorldMap.calculateNextPosition(destination.getX(), destination.getY(), "up");
							destination.setX(destination.getX());
							destination.setY(destination.getY());
							Team team = new Team();
							team.setTeamId(TEAM_ID);
							mri.setCell(destination);
							mri.setTeam(team);

							ACLMessage msg = MessageHandler.constructMessage(agMovementSim, ACLMessage.REQUEST, mri, XplorationOntology.MOVEMENTREQUESTINFO);
							send(msg);			                	

							System.out.println(getLocalName() + ": REQUEST is sent");

							ACLMessage ans = MessageHandler.blockingReceive(myAgent, XplorationOntology.MOVEMENTREQUESTINFO);
							if (ans != null) {
								if (ans.getPerformative() == ACLMessage.REFUSE) {
									System.out.println(getLocalName() + ": REFUSED due to Invalid Cell");
									movementRequested = true;
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD) {
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									movementRequested = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE) {
									System.out.println(getLocalName() + ": Initial AGREE was received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, XplorationOntology.MOVEMENTREQUESTINFO);
									if (finalMsg.getPerformative() == ACLMessage.INFORM) {
										System.out.println(getLocalName() + ": INFORM was received, movement accepted");
										location = destination;
										movementRequested = true;
										// Analyze this cell 
										analyzeCell(location);
									}
									else if (finalMsg.getPerformative() == ACLMessage.FAILURE) {
										System.out.println(getLocalName() + ": FAILURE was received, collision");
										movementRequested = true;
									}
								}
							}
						}
					}
					catch (Exception e) {
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}


				}
			}

			@Override
			public boolean done() {
				// TODO Auto-generated method stub
				return movementRequested;
			}

		});
	}
	//Cell Claim Protocol from Rover to Platform Simulator
	private void claimCell(){
		addBehaviour (new SimpleBehaviour (this){
			
			//Receiver Agent ID
			AID agCommunication;
			private boolean claimCell = false;

			public void action(){

				if(!claimCell){
					//Searching for an agent with RADIOCLAIMSERVICE Description
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.RADIOCLAIMSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agCommunication = (AID) result[0].getName();											
							System.out.println(getLocalName()+ ": Radio Claim Service is found");

							ClaimCellInfo cci = new ClaimCellInfo();
							
							//CREATING EXAMPLE INPUT, ONLY FOR TESTING TODO delete
							Team myTeam = new Team();
							myTeam.setTeamId(Constants.TEAM_ID);
							cci.setTeam(myTeam);
							org.xploration.ontology.Map cciMap = new org.xploration.ontology.Map();
							Cell myCell = new Cell();
							myCell.setMineral("A");
							myCell.setX(1);
							myCell.setY(3);
							cciMap.addCellList(myCell);
							cci.setMap(cciMap);
													
							try{
								ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, cci, XplorationOntology.CLAIMCELLINFO);
								send(msg);	
								System.out.println(getLocalName() + ": INFORM is sent");
								claimCell = true;
							}
							catch(Exception e){
								e.printStackTrace();
								System.out.println(getLocalName() + ": INFORM couldn't sent");
							}
						}
						else{
							System.out.println(getLocalName()+ ": No agent found yet");
							doWait(5000);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
	
			public boolean done() {
				return claimCell;
			}			
		});
	}
}



