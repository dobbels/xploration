package org.xploration.team4.company;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.XplorationOntology;
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
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

//TODO search for all the services at the startup of this agent (in a behaviour) !! Not every time you do an action of course! 
//TODO solve OntologyException
public class AgRover4 extends Agent {

	private int worldDimY;
	private int worldDimX;
	private int missionLength;
	private int communicationRange;
	
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;

	enum State {
		MOVING, ANALYZING, OTHER
	}

	private State state = State.OTHER;
	private Cell location = new Cell();
	private Cell capsuleLocation = new Cell();
	private ArrayList<Cell> analyzedCells = new ArrayList<>();
	private ArrayList<Cell> claimedCells = new ArrayList<>();
	private boolean alreadyClaiming = false;
	
	private ArrayList<Cell> nextMovements = new ArrayList<Cell>();
	private ArrayList<String> directions = new ArrayList<String>();

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
		location.setX(arg1); capsuleLocation.setX(arg1);
		location.setY(arg2); capsuleLocation.setY(arg2);
		worldDimX = arg3;
		worldDimY = arg4;
		missionLength = arg5;
		communicationRange = arg6;
		
		localWorldMap = new Map(worldDimX, worldDimY);

		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		System.out.println(getLocalName()+": communicationRange: "+ arg6);
		
		directions.add("rightUp");
		directions.add("rightDown");
		directions.add("down");
		directions.add("leftDown");
		directions.add("leftUp");
		directions.add("up");
		
		//roverRegistration for Map Simulator
	    roverRegistration(location);	    
	} 
	
	private void startMainBehaviour() {
		addBehaviour (new CyclicBehaviour(this) {

			@Override
			public void onStart() {
				addBehaviour(tbf.wrap(killAgentAtMissionEnd()));
				super.onStart();
			}
			
			@Override
			public void action() {
				if (nextMovements.isEmpty()) {
					System.out.println(getLocalName() + ": calculating next cells");
					nextMovements = calculateBorderCells(location, localWorldMap.distance(location, capsuleLocation)+1); 
					System.out.println(nextMovements);
				} //TODO with 6 if's we can probably make a bulletproof spiralalgorithm.
				
				if (!analyzing() && !moving() && currentCellAnalyzed()) {
					System.out.println(getLocalName() + ": requesting movement");
					if (state == State.ANALYZING && state == State.MOVING) {
						System.out.println(getLocalName() + ": ERROR! Moving while analyzing");
					}
					state = State.MOVING;
					requestMovement(nextMovements.get(0));
				} //TODO in future maybe turn around and start turning anti clockwise if there are too many claimed cells before you 
				
				if (!moving() && !analyzing() && !currentCellAlreadyHandled()) {
					System.out.println(getLocalName() + ": analyzing");
					if (state == State.MOVING && state == State.ANALYZING) {
						System.out.println(getLocalName() + ": ERROR! Analyzing while moving");
					}
					state = State.ANALYZING;
					analyzeCurrentCell();
				} //TODO print at start and end of claimCells(), there is a currentModification error
				
				// nextMovements.remove(0); is done in requestMovement after an inform. If a failure is received, the same cell will be tried on the next iteration.
				//TODO if refused or failed, calculate new nextMovements from current location? Or relative to capsule loc? 
				
				// TODO add exploration-algorithm-logic
				// we could move in spiral + radio range level if no other rovers are around
				// if rovers are around move in spiral starting close to where they are so that they don't move to our position
				
				// maybe never go back in range of capsule, except when the end of the mission is approaching. 
				// if the world is small, maybe it's even best to just go around the whole time, or mark your territory first and then 
				// start filling in the inside. To calculate the size of this territory you could make some calculation based on missionlength, analyzingTime and movementTime (oh maybe these last ones you don't know).
				// + claim cells you get in map broadcast. Just in case they broadcast it right after analyzing, not after claiming
			}
		});
	}
	
	/*
	 * Our current location is already analyzed by this rover or claimed by other rovers
	 * (In this function we assume that others only broadcast claimed cells, not when they are just analyzed)
	 */ 
	protected boolean currentCellAlreadyHandled() {
		for (Cell c : localWorldMap.getCellList()) {
			if (c.getX() == location.getX() && c.getY() == location.getY() && c.getMineral() != null)
				return true;
		}
		return false;
	}

	protected boolean analyzing() {
		return state == State.ANALYZING;
	}
	
	protected boolean moving() {
		return state == State.MOVING;
	}
	
	protected boolean currentCellAnalyzed() {
		for (Cell c : analyzedCells) {
			if (c.getX() == location.getX() && c.getY() == location.getY())
				return true;
		}
		for (Cell c : claimedCells) {
			if (c.getX() == location.getX() && c.getY() == location.getY())
				return true;
		}
		return false;
	}

	private ArrayList<Cell> calculateBorderCells(Cell position) {
		ArrayList<Cell> border = new ArrayList<Cell>();
		for (int i = 0; i < directions.size(); i++) {
			Cell next = localWorldMap.calculateNextPosition(position.getX(), position.getY(), directions.get(i));
			border.add(next);
		}
		return border;
	}
	
	private ArrayList<Cell> calculateBorderCells(Cell position, int distance) {
		ArrayList<Cell> borderCells = new ArrayList<Cell>();
		Cell nextPos = localWorldMap.calculateNextPosition(position.getX(), position.getY(), "up");
		borderCells.add(nextPos);
		
		//TODO should work with (borderCells.get(0) equals borderCells.get(borderCells.size()-1)
		while(borderCells.size() <= (distance * 6)) {
			ArrayList<Cell> border = new ArrayList<Cell>();
			for (int i = 0; i < directions.size(); i++) {
				Cell next = localWorldMap.calculateNextPosition(nextPos.getX(), nextPos.getY(), directions.get(i));
				border.add(next);
			}
			Cell c;
			ArrayList<Cell> atRightDistance = new ArrayList<Cell>();
			for (int j = 0; j < border.size(); j++) {
				c = border.get(j);
				if (localWorldMap.distance(capsuleLocation, c) == distance)
					atRightDistance.add(c);
			}
			if (atRightDistance.size() != 2) {
				System.out.println("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
				System.out.println(nextPos.getX() + ", " + nextPos.getY());
				for (Cell whut : atRightDistance) {
					System.out.println(whut.getX() + ", " + whut.getY());
				}
			}

			for (Cell last : atRightDistance) {
				if (notContains(borderCells, last)) {
					nextPos = last;
					break;
				}
			}
//			nextPos = atRightDistance.get(0);
			borderCells.add(nextPos);
		}
		borderCells.remove(borderCells.size()-1);
		return borderCells;
	}
	
	private boolean notContains(ArrayList<Cell> borderCells, Cell last) {
		for (Cell c : borderCells) {
			if (c.getX() == last.getX() && c.getY() == last.getY())
				return false;
		}
		return true;
	}

	private WakerBehaviour killAgentAtMissionEnd() { //TODO use in every agent, especially in PlatformSimulator
		return new WakerBehaviour(this, missionLength*1000) {
			
			protected void onWake() {
				System.out.println(getLocalName() + ": committing suicide");
                myAgent.doDelete();
	        } 
		};
	}

	
	
	// ------------------------------------------------------------------------------------------------------------------

	private void analyzeCurrentCell(){ //TODO all behaviours parallel? We have roverstates, so should normally stay consistent? But what do we win with it?

		addBehaviour (new SimpleBehaviour(this) { //TODO can also be OneShotBehaviour?					  			
			private static final long serialVersionUID = 1L;

			AID agTerrain;

			private boolean cellAnalyzed = false;

			public void action(){

				//A defensive check
				if(!cellAnalyzed){
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
							cellAnalysis.setCell(location);

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
									cellAnalyzed = true;
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									cellAnalyzed = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE)
								{
									System.out.println(getLocalName() + ": Initial AGREE was received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, XplorationOntology.CELLANALYSIS);

									switch (finalMsg.getPerformative()) {
									case ACLMessage.INFORM:
										System.out.println(getLocalName()+": analyze INFORM is received!");

										ContentElement ce;
										try {
											ce = getContentManager().extractContent(finalMsg);

											// We expect an action inside the message
											if (ce instanceof Action) {
												Action agAction = (Action) ce;
												Concept conc = agAction.getAction();

												if (conc instanceof CellAnalysis) {
													Cell cell = ((CellAnalysis) conc).getCell();
													analyzedCells.add(cell);
													localWorldMap.setCell(cell);
													System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
															+cell.getX() + ","+ cell.getY()+  ", " + cell.getMineral() + ")");
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										cellAnalyzed = true;	

										break;
									case ACLMessage.FAILURE:
										System.out.println(getLocalName()+": FAILURE was received!");
										cellAnalyzed = true;
									}							
								}						  						  						  
							}else{
								//If no message arrives
								block();
							}
							state = State.OTHER;

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
				return cellAnalyzed;
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
							team.setTeamId(TEAM_ID);
							roverReg.setTeam(team);

							ACLMessage msg = MessageHandler.constructMessage(agMapSimulator, ACLMessage.INFORM, roverReg, XplorationOntology.ROVERREGISTRATIONINFO);
							send(msg);	
							System.out.println(getLocalName() + ": INFORM is sent");
							roverRegistration = true;

							System.out.println(getLocalName() + ": Main behaviour started");
							startMainBehaviour();
							listenForMaps();
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
	private void broadcastCurrentMap(){

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

						if (!claimedCells.isEmpty()) {
							MapBroadcastInfo mbi = new MapBroadcastInfo();
							org.xploration.ontology.Map map = new org.xploration.ontology.Map();

							for (Cell c : claimedCells) {
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
	//TODO you have to give something back here? because if movementrequest fails, it is still removed from list and on next move there is a problem? 
	private void requestMovement(Cell destination) { //TODO here we probably add an argument 'cell' and take the decision of where to go outside?!
		addBehaviour(new SimpleBehaviour(this) {
			private static final long serialVersionUID = 1L;
			AID agMovementSim;
			private boolean movementRequested = false;
			@Override
			public void action() {
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
							agMovementSim = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": movement simulator agent is found");

							MovementRequestInfo mri = new MovementRequestInfo();
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
										if (nextMovements.get(0) != destination)
											System.out.println(getLocalName() + ": ERROR! next destination does not match provisioned one.");
										nextMovements.remove(0);
										movementRequested = true;
										
										// Try to claim here, because there is no use in trying more often than the times you move 
										if (!analyzedCells.isEmpty() && !alreadyClaiming && localWorldMap.inRangeFrom(location, capsuleLocation, communicationRange)) {
											alreadyClaiming = true;
//											System.out.println("should not be empty: " + analyzedCells);
											System.out.println(getLocalName() + ": claimin'");
											claimCells();
										}
										
										broadcastCurrentMap();
										
//										if (!currentCellAlreadyHandled())
//											analyzeCurrentCell();
									}
									else if (finalMsg.getPerformative() == ACLMessage.FAILURE) {
										System.out.println(getLocalName() + ": FAILURE was received, collision");
										movementRequested = true;
									}
								}
							}
						}
						state = State.OTHER;
					}
					catch (Exception e) {
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}
			}

			@Override
			public boolean done() {
				return movementRequested;
			}

		});
	}
	//Cell Claim Protocol from Rover to Platform Simulator
	private void claimCells(){
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
							
							Team myTeam = new Team();
							myTeam.setTeamId(TEAM_ID);
							cci.setTeam(myTeam);
							org.xploration.ontology.Map cciMap = new org.xploration.ontology.Map();
							System.out.println(analyzedCells);
							for (Cell c : analyzedCells) {
								cciMap.addCellList(c);
								claimedCells.add(c);
							}
							analyzedCells.clear();
							
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
			
			@Override
			public int onEnd() {
				alreadyClaiming = false;
				return super.onEnd();
			}
		});
	}
}



