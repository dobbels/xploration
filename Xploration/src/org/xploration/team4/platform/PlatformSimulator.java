package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.xploration.ontology.CapsuleRegistrationInfo;
import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;

import jade.content.AgentAction;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class PlatformSimulator extends Agent {
	
	// TODO think of the case (in the beginning or in registration failure) when not everyone/no one is registered. Don't count on those things.  
	
	// TODO every listening behaviour you put in a thread (http://jade.tilab.com/doc/api/jade/core/behaviours/ThreadedBehaviourFactory.html),
	//			you can just do blockingReceive() ! If there are any problems with messages that don't arrive, then this might be the solution.
	
	private static final long serialVersionUID = 1L;
	//TODO eventually maybe the difference between variables of 'different' simulators is not important anymore.
	/***COMMON***/
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	Map worldMap = MapReader.readMap();
	
	private int worldDimensionY = worldMap.getWidth(); 
	private int worldDimensionX = worldMap.getHeight();
	
	enum State {
		MOVING, OTHER
	}
	
	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	
	/***COMM_SIM***/
	private int communciationRange = Constants.COMMUNICATION_RANGE;
	
	/***MAP_SIM***/
	// registered rovers and capsules are implicit. Use .hasKey() to know if registered. 
	private HashMap<Integer, AID> capsuleAID = new HashMap<>();
	private HashMap<Integer, Cell> capsulePositions = new HashMap<>();
	
	/***MOVEMENT_SIM + MAP_SIM***/
	private HashMap<Integer, AID> roverAID = new HashMap<Integer, AID>();
	private HashMap<Integer, Cell> roversPosition = new HashMap<Integer, Cell>();
	
	private int movingTime = Constants.MOVEMENT_TIME*1000; // in milliseconds
	
	//TODO what to do with this?
	public int initialX = 1;
	public int initialY = 3;
	
	/***TERRAIN_SIM***/
	private int analyzingTime = Constants.ANALYSIS_TIME*1000; // in milliseconds
	private HashMap<AID, Integer> AIDToTeamId = new HashMap<AID, Integer>();
	private HashMap<Integer, State>	roverState = new HashMap<>();
	
		
	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");
		
		//System.out.println("This is the worldmap:");
		//worldMap.printWorldMap();

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		try
		{
			DFAgentDescription dfd = new DFAgentDescription();
			
			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.TERRAINSIMULATOR);
			dfd.addServices(sd);
			
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.MAPBROADCASTSERVICE);
			dfd.addServices(sd);
			
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.MOVEMENTREQUESTSERVICE);
			dfd.addServices(sd);
			
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.CAPSULEREGISTRATIONSERVICE);
			dfd.addServices(sd);
			
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.ROVERREGISTRATIONSERVICE);
			dfd.addServices(sd);
						
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(org.xploration.ontology.XplorationOntology.RADIOCLAIMSERVICE);
			dfd.addServices(sd);
			
			DFService.register(this, dfd);
			
		} 
		catch (FIPAException e)
		{
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
			e.printStackTrace();
		}
		
		/***COMM_SIM***/
		Behaviour bb = mapBroadcastListener();
		addBehaviour(tbf.wrap(bb));	
		
		/***MAP_SIM***/
		Behaviour rb = roverRegistrationListener();
		Behaviour cb = capsuleRegistrationListener();
		addBehaviour(tbf.wrap(rb));
		addBehaviour(tbf.wrap(cb));	
		
		/***MOVEMENT_SIM***/
		Behaviour mb = movementListener();
		addBehaviour(tbf.wrap(mb));
		
		/***TERRAIN_SIM***/
		Behaviour cab = cellAnalysisRequestListener();
		addBehaviour(tbf.wrap(cab));
		
		/***NETWORK_SIM***/
		Behaviour ccr = cellClaimRoverListener();
		addBehaviour(tbf.wrap(ccr));
	}
		
	/*
	 * All of the behaviours below sleep the whole time. When a message arrives they all wake up and the right behaviour handles the message.
	 */
	private Behaviour mapBroadcastListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = -4555719000913759629L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO);
				
				if (msg != null) {
					System.out.println(getLocalName() + ": received map broadcast");
					
					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
						printContent(ce);
						
						if (ce instanceof Action) {
							Concept conc = ((Action) ce).getAction();
							if (conc instanceof MapBroadcastInfo) {
								
								MapBroadcastInfo mbi = (MapBroadcastInfo) conc;
								
								AID fromAgent = msg.getSender();
								Cell location = roversPosition.get(AIDToTeamId.get(fromAgent));
								
		//						System.out.println(location.getX() + " " + location.getY());
								ArrayList<AID> inRange = getAllInRange(location);
								// Not send map back to sender
								inRange.remove(fromAgent);
								System.out.println("Number of rovers/capsules in range: " + inRange.size());
								if (!inRange.isEmpty()) {
									for (AID aid : inRange) {
										ACLMessage forward = MessageHandler.constructMessage(aid, ACLMessage.INFORM, mbi, XplorationOntology.MAPBROADCASTINFO);
										send(forward);
//										System.out.println(aid);
									}
									System.out.println(getLocalName() + ": MAPBROADCAST is forwarded");
								} 
								else {
									System.out.println(getLocalName() + ": No others in range of rover " + AIDToTeamId.get(msg.getSender()));
								}
						    }
							else {
								System.out.println(getLocalName() + ": Error when unpacking broadcast");
							}
						}
						else {
							System.out.println(getLocalName() + ": Error when unpacking broadcast");
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

			private ArrayList<AID> getAllInRange(Cell location) {
				ArrayList<AID> inRange = new ArrayList<AID>();
				for (Integer teamid : roversPosition.keySet()) {
					Cell other = roversPosition.get(teamid); 
					if (inRangeFrom(location, other)) {
						inRange.add(roverAID.get(teamid));
					}
				}
				for (Integer teamid : capsulePositions.keySet()) {
					Cell other = capsulePositions.get(teamid);
					if (inRangeFrom(location, other))
						inRange.add(capsuleAID.get(teamid));
				}
				return inRange;
			}
			
			private void printContent(ContentElement ce) {
				Action agAction = (Action) ce;
				Concept conc = agAction.getAction();
				
				System.out.println(getLocalName()+": BroadCasted Cells: ");

				MapBroadcastInfo mbi  = (MapBroadcastInfo) conc;
				
				org.xploration.ontology.Map map = mbi.getMap();
				
				Iterator it = map.getAllCellList();
				Cell c;
				while (it.hasNext()) {
					c = (Cell) it.next();
					System.out.println(getLocalName() + "  x: " + c.getX() + " y: "+ c.getY() +"  mineral: " + c.getMineral());
				}

			}
		};
	}
	
	private Behaviour roverRegistrationListener() {
		return new CyclicBehaviour(this) {

			private static final long serialVersionUID = -1383552485084791798L;

			public void action() {
				//RoverRegistrationService Protocol
				ACLMessage msg = MessageHandler.receive(myAgent, XplorationOntology.ROVERREGISTRATIONINFO);

				if (msg != null )
				{
					if(msg.getPerformative()== ACLMessage.INFORM){
						ContentElement ce;
						try {
							ce = getContentManager().extractContent(msg);

							// We expect an action inside the message
							if (ce instanceof Action)
							{
								Action agAction = (Action) ce;
								Concept conc = agAction.getAction();
								// If the action is CellAnalysis
								if(conc instanceof RoverRegistrationInfo)
								{
									//Storing the message sender agent
									AID fromAgent = msg.getSender();

									System.out.println(getLocalName()+": Rover Registration INFORM is received from " + 
											(msg.getSender()).getLocalName());
									
									RoverRegistrationInfo roverLoc = (RoverRegistrationInfo) conc;
									Cell roverLocation = roverLoc.getCell();		
									Team team = roverLoc.getTeam();
									roverAID.put(team.getTeamId(), fromAgent);
									AIDToTeamId.put(fromAgent, team.getTeamId());
									roversPosition.put(team.getTeamId(), roverLocation);
									roverState.put(team.getTeamId(), State.OTHER);
									System.out.println(getLocalName()+ ": Rover Location is " + roverLocation.getX() + "," + roverLocation.getY());
								}
							}
						}catch(Exception e){
							e.printStackTrace();
							System.out.println("Message Exception is detected!");
						}
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}
		};		
	}
		
	private Behaviour capsuleRegistrationListener() {
		return new CyclicBehaviour(this) {

			private static final long serialVersionUID = 5731197496710703895L;

			public void action() {
				//capsuleRegistrationService Protocol
				ACLMessage msg = MessageHandler.receive(myAgent, XplorationOntology.CAPSULEREGISTRATIONINFO);

				if (msg != null )
				{
					if(msg.getPerformative()== ACLMessage.INFORM){
						ContentElement ce;
						try {
							ce = getContentManager().extractContent(msg);

							// We expect an action inside the message
							if (ce instanceof Action)
							{
								Action agAction = (Action) ce;
								Concept conc = agAction.getAction();
								// If the action is CellAnalysis
								if(conc instanceof CapsuleRegistrationInfo)
								{
									//Storing the message sender agent
									AID fromAgent = msg.getSender();
									
									System.out.println(getLocalName()+": Capsule Registration INFORM is received from " + 
											(msg.getSender()).getLocalName());
									
									CapsuleRegistrationInfo capsuleLoc = (CapsuleRegistrationInfo) conc;
									Cell capsuleLocation = capsuleLoc.getCell();	
									Team team = capsuleLoc.getTeam();
									capsuleAID.put(team.getTeamId(), fromAgent);
									capsulePositions.put(team.getTeamId(), capsuleLocation);
									System.out.println(getLocalName()+ ": Capsule Location is " + capsuleLocation.getX() + "," + capsuleLocation.getY());
								}
							}
						}catch(Exception e){
							e.printStackTrace();
							System.out.println("Message Exception is detected!");
						}
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}
		};
	}
	
	private Behaviour movementListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = -8872722866521058972L;

			@Override
			public void action() {
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.REQUEST, XplorationOntology.MOVEMENTREQUESTINFO); 
				 
				if (msg != null) {
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Action) {
							Action agAction = (Action) ce;
							Concept conc = agAction.getAction();
							if (conc instanceof MovementRequestInfo) {
								AID fromAgent = msg.getSender();
								System.out.println(myAgent.getLocalName() + ": received movement request from "
										+ (msg.getSender()).getLocalName());
								Cell destination = ((MovementRequestInfo) conc).getCell();
								int team = AIDToTeamId.get(fromAgent);
								//TODO communicate internally the dimensions of map
								if (Constants.isExistingCoordinate(worldMap.getWidth(), worldMap.getHeight(), destination.getX(), destination.getY()) 
										&& worldMap.isNextPosition(roversPosition.get(team).getX(), roversPosition.get(team).getY(), destination.getX(), destination.getY())) {
									ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.AGREE);
									myAgent.send(reply);
									System.out.println(myAgent.getLocalName()+": Initial AGREEMENT is sent");
									
									//wait for n seconds and send message
									roverState.replace(team, State.MOVING);
									confirmNewLocation(msg, destination, team, movingTime);
									
									System.out.println(myAgent.getLocalName() + ": INFORM is sent with destination cell " + destination.getX() + " " + destination.getY());
								}
								else {
									//invalid postion
									ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.REFUSE);
									myAgent.send(reply);
									System.out.println(myAgent.getLocalName()+": REFUSE due to invalid cell");
								}
							}
							else {
								throw new NotUnderstoodException(msg);
							}
						}
						else {
							throw new NotUnderstoodException(msg);
						}
					} catch (NotUnderstoodException | CodecException | OntologyException e) {
						e.printStackTrace();
						ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.NOT_UNDERSTOOD); 
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");	
					}
				}
				else{
					//if no message arrives
					block(); // The behaviour of an agent is woken up again whenever the agent receives an ACLMessage.
				}
			}
		};
	}
	
	private void confirmNewLocation(ACLMessage msg, Cell destination, int team, long timePeriod){
		addBehaviour (new WakerBehaviour (this, timePeriod){

			private static final long serialVersionUID = 1L;

			protected void handleElapsedTimeout() {
				//send inform message and update rover position
				roverState.replace(team, State.OTHER);
				roversPosition.replace(team, destination);
				ACLMessage inform = MessageHandler.constructReplyMessage(msg, ACLMessage.INFORM);
				send(inform);
			}
		});
	}

	private Behaviour cellAnalysisRequestListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = 11924124L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.REQUEST, XplorationOntology.CELLANALYSIS);  
				
				if (msg != null )
				{
					// If a Cell Claiming request arrives
					// it answers with the REFUSE, AGREE or NOT_UNDERSTOOD

					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);

						// We expect an action inside the message
						if (ce instanceof Action)
						{
							Action agAction = (Action) ce;
							Concept conc = agAction.getAction();
							// If the action is CellAnalysis
							if(conc instanceof CellAnalysis)
							{
								//Storing the message sender agent
								AID fromAgent = msg.getSender();

								System.out.println(getLocalName()+": CellAnalysis REQUEST is received from " + 
										(msg.getSender()).getLocalName());

								CellAnalysis ca = (CellAnalysis) conc;
								Cell cellToAnalyze = ca.getCell();
															
								//Exact coordinates for the map
								int m = cellToAnalyze.getX();
								int n = cellToAnalyze.getY();
								
								try {
									//Invalid Cell Condition
									//Checking world boundaries
									//Check if existing cell within world
									if(cellToAnalyze.getX()>worldDimensionY || cellToAnalyze.getY()>worldDimensionX || !(cellToAnalyze.getX()%2 == cellToAnalyze.getY()%2))
									{
										ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.REFUSE);
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": REFUSE due to invalid cell");
//										doWait(3000);
									}
	
									//Valid Cell Condition
									else
									{								
										ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.AGREE);
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": Initial AGREEMENT is sent");
	
										//Only INFORM case
										int teamID = AIDToTeamId.get(fromAgent);
										if(!(isValidPosition(teamID, cellToAnalyze) && roverState.get(teamID) != State.MOVING)){
											System.out.println(getLocalName()+ ": Either rover " + AIDToTeamId.get(fromAgent) + " is moving while analyzing or it is not in the location asked to analyze.");

											sendFailure(msg, 2*analyzingTime);
											
											System.out.println(myAgent.getLocalName() + ": FAILURE will be sent to team "+ AIDToTeamId.get(fromAgent));
										}
										else {
											CellAnalysis cellAnalysis = new CellAnalysis();
											cellAnalysis.setCell(worldMap.getCell(m, n));
											
											informMineral(msg, cellAnalysis, (long) analyzingTime);
//											System.out.println("issued: " + new Date());
											
											System.out.println(myAgent.getLocalName() + ": INFORM with mineral "+ worldMap.getCell(m, n).getMineral() + " will be sent in " + analyzingTime/1000 + " seconds");
										}	
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}else{
								throw new NotUnderstoodException(msg);
							}
						}else{
							throw new NotUnderstoodException(msg);
						}
					}catch(NotUnderstoodException |CodecException | OntologyException e){
						//NOT_UNDERSTOOD message is sent
						e.printStackTrace();
						ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.NOT_UNDERSTOOD); 
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");				
					}
				}
				else{
					//if no message arrives
					block(); // The behaviour of an agent is woken up again whenever the agent receives an ACLMessage.
				}
			}
			
			private boolean isValidPosition(int team, Cell location) {
				Cell actualLocation = roversPosition.get(team);
				System.out.println("Valid position? " + (actualLocation.getX() == location.getX() && 
						actualLocation.getY() == location.getY()));
				return (actualLocation.getX() == location.getX() && 
						actualLocation.getY() == location.getY());
			}
		};
	}
	
	private void informMineral(ACLMessage msg, CellAnalysis cellAnalysis, long timePeriod){
		addBehaviour (new WakerBehaviour (this, timePeriod){

			private static final long serialVersionUID = 1L;

			protected void handleElapsedTimeout() {
				ACLMessage inform = MessageHandler.constructReplyMessage(msg, ACLMessage.INFORM, cellAnalysis); 
                send(inform);
//                System.out.println("sent: " +new Date());
			}
		});
	}
	
	private void sendFailure(ACLMessage msg, long timePeriod){
		addBehaviour (new WakerBehaviour (this, timePeriod){

			private static final long serialVersionUID = 1L;

			protected void handleElapsedTimeout() {
				ACLMessage failure = MessageHandler.constructReplyMessage(msg, ACLMessage.FAILURE); 
                send(failure);
			}
		});
	}

	
	
	private Behaviour cellClaimRoverListener(){
		return new CyclicBehaviour(this){
			public void action(){
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.CLAIMCELLINFO);
				
				if(msg != null){		
					ContentElement ce;
					
					try{
						ce = getContentManager().extractContent(msg);
						
						if(ce instanceof Action){
							Concept conc = ((Action) ce).getAction();
							if(conc instanceof ClaimCellInfo){			
								
								AID fromAgent = msg.getSender();
								try{
										System.out.println(getLocalName()+ ": INFORM is received about claim cell Info");
										
										ClaimCellInfo cellInfo = (ClaimCellInfo) conc;
										Team claimedTeam = cellInfo.getTeam();
										//TODO COULDN'T EXTRACT A MEANINGFUL MAP INFO
										org.xploration.ontology.Map claimedMap = cellInfo.getMap(); 
										jade.util.leap.List myCellList = claimedMap.getCellList();
										
										//This is not meaningful info, I know But I couldn't access the cell in the list
										System.out.println(getLocalName()+ ": " + myCellList.get(0));										
										System.out.println(getLocalName()+ ": claimed team is: team" + claimedTeam.getTeamId() + " and " + 
										//TODO ADD A MEANINGFUL MAP VALUE FOR DISPLAYING THE MESSSAGE
										"claimed map is: ");
											
										//Information is passing to capsule
										cellClaimToCapsule(cellInfo);																																						
								}
								catch(Exception e){
									e.printStackTrace();
									System.out.println(getLocalName()+ ": ERROR about extracting the message");
								}
							}
							else{
								System.out.println(getLocalName()+ ": ERROR about unpacking ClaimCellInfo");
							}
						}
						else{
							System.out.println(getLocalName()+ ": ERROR about unpacking ClaimCellInfo");
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
				else{
					//Empty message is ignored
					block();
				}			
			}					
	   };
	}
	
	private void cellClaimToCapsule(ClaimCellInfo cellInfo){
		addBehaviour (new CyclicBehaviour (this){ //TODO not cyclic, should be simple

			AID agCommunication;
			Cell capsuleCell;
			Cell roverCell;
			private boolean claimCellToCapsule = false;

			public void action(){

				if(!claimCellToCapsule)
				{		
					Team checkTeam = cellInfo.getTeam();
					//Finds the corresponding AID for that team ID
					agCommunication = capsuleAID.get(checkTeam.getTeamId());
					//Finds the capsule's located cell for that team ID
					capsuleCell = capsulePositions.get(checkTeam.getTeamId());
					//Finds the rover's located cell for that team ID
					roverCell = roversPosition.get(checkTeam.getTeamId());
					
					//If capsule is NOT registered yet, platform simulator will not send any message
					if(agCommunication != null){
						//If the locations of rover or capsule is not stored 
						if(capsuleCell != null && roverCell != null)
						{
							//Checking distance to capsule
							if(inRangeFrom (capsuleCell, roverCell))
							{
								try{
									ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, cellInfo, XplorationOntology.CLAIMCELLINFO);
									send(msg);	
									System.out.println(getLocalName() + ": INFORM is sent");
									claimCellToCapsule = true;
								}
								catch(Exception e){
									e.printStackTrace();
									System.out.println(getLocalName() + ": INFORM couldn't sent");
								}
							}
							else{
								System.out.println(getLocalName()+ ": capsule is not close enough to deliver the message");
								doWait(5000);
							}
						}
						else{
							System.out.println(getLocalName()+ ": either capsule or either location is not stored");
							doWait(5000);
						}
					}
					else{
						System.out.println(getLocalName()+ ": capsule is not registered yet, cell claim info couldn't sent");
						doWait(5000);
					}
				}
			}			
		});
	}	
	//I needed to use these two functions for checking the distance between capsule - rover
	//Thats why I moved them to here and they are under comments in the listener
	//If I could call them somehow in the listener, please let me know later
	private boolean inRangeFrom(Cell rover, Cell other) {
    	// In these calculations it is assumed that the map is spherical, so
		// from the left side, you can go directly to the rigth side and so on. 
    	int x = rover.getX();
    	int y = rover.getY();
    	
    	int x_other = other.getX();
    	int y_other = other.getY();
    	
    	int distance = distance(x,y,x_other,y_other);
    	
    	return (0 <= distance && distance <= Constants.COMMUNICATION_RANGE);
	}
	
	public int distance(int x, int y, int x_other, int y_other) {
        int rightDiff = (worldDimensionY + y_other - y) % worldDimensionY;
        int leftDiff = (worldDimensionY + y - y_other) % worldDimensionY;
        int upDiff = (worldDimensionX + x - x_other) % worldDimensionX;
        int downDiff = (worldDimensionX + x_other - x) % worldDimensionX;

        int distY = Math.min(rightDiff, leftDiff);
        int distX = Math.min(upDiff, downDiff);

        return distY + Math.max(0, (distX - distY) / 2);
    }
}
