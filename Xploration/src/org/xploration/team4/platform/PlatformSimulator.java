package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.xploration.ontology.CapsuleRegistrationInfo;
import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;

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
import jade.core.behaviours.ThreadedBehaviourFactory;
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
	
	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory(); //TODO use for listening threads. Why not..
	
	/***COMM_SIM***/
	private int communciationRange = 3;
	
	/***MAP_SIM***/
	// registered rovers and capsules are implicit. Use .hasKey() to know if registered. 
	private HashMap<Integer, AID> capsuleAID = new HashMap<>();
	private HashMap<Integer, Cell> capsulePositions = new HashMap<>();
	
	/***MOVEMENT_SIM + MAP_SIM***/
	private HashMap<Integer, AID> roverAID = new HashMap<Integer, AID>();
	private HashMap<Integer, Cell> roversPosition = new HashMap<Integer, Cell>();
	
	private int movingTime = 2000;
	
	//TODO what to do with this?
	public int initialX = 1;
	public int initialY = 3;
	
	/***TERRAIN_SIM***/
	private int analyzingTime = 2000; // in milliseconds
	private HashMap<AID, Integer> AIDToTeamId = new HashMap<AID, Integer>();
	private HashMap<Integer, State>	roverState = new HashMap<>();
		
	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");
		
		System.out.println("This is the worldmap:");
		worldMap.printWorldMap();

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
			
			DFService.register(this, dfd);
			
		} 
		catch (FIPAException e)
		{
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
			e.printStackTrace();
		}
		
		/*
		 * Out of Movement Simulator
		 */
		//fill hashmap for testing purposes
//		Cell cell1 = new Cell();
//		cell1.setX(1);
//		cell1.setY(1);
//		Cell cell2 = new Cell();
//		cell2.setX(1);
//		cell2.setY(3);
//		Cell cell3 = new Cell();
//		cell3.setX(5);
//		cell3.setY(5);
//		Cell cell4 = new Cell();
//		cell4.setX(1);
//		cell4.setY(7);
//		roversPosition.put(1, cell1);
//		roversPosition.put(2, cell2);
//		roversPosition.put(3, cell3);
//		roversPosition.put(4, cell4);
		
		addBehaviours();
	}
	
	protected void addBehaviours() {
		/***COMM_SIM***/
		addBehaviour(mapBroadcastListener());
		
		/***MAP_SIM***/
		addBehaviour(roverRegistrationListener());
		addBehaviour(capsuleRegistrationListener());	
		
		/***MOVEMENT_SIM***/
		addBehaviour(MovementListener());
		
		/***TERRAIN_SIM***/
		addBehaviour(cellAnalysisRequestListener());
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
						
						AID fromAgent = msg.getSender();
						Cell location = roversPosition.get(AIDToTeamId.get(fromAgent));
						
//						System.out.println(location.getX() + " " + location.getY());
						ArrayList<AID> inRange = getAllInRange(location);
						// Not send map back to sender
						inRange.remove(fromAgent);
						System.out.println("Number of rovers/capsules in range: " + inRange.size());
						if (!inRange.isEmpty()) {
							ACLMessage forward = MessageHandler.constructReceiverlessMessage(ACLMessage.INFORM, (Action) ce, XplorationOntology.MAPBROADCASTINFO);
							for (AID aid : inRange) {
								forward.addReceiver(aid);
							}
							send(forward);
							System.out.println(getLocalName() + ": MAPBROADCAST is forwarded");
						} 
						else {
							System.out.println(getLocalName() + ": No others in range of rover " + AIDToTeamId.get(msg.getSender()));
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
	
	private Behaviour MovementListener() {
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
								Team team = ((MovementRequestInfo) conc).getTeam();
								Cell destination = ((MovementRequestInfo) conc).getCell();
								//TODO communicate internally the dimensions of map
								if (Constants.isExistingCoordinate(worldMap.getWidth(), worldMap.getHeight(), destination.getX(), destination.getY()) 
										&& worldMap.isNextPosition(roversPosition.get(team.getTeamId()).getX(), roversPosition.get(team.getTeamId()).getY(), destination.getX(), destination.getY())) {
									
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					//if no message arrives
					block(); // The behaviour of an agent is woken up again whenever the agent receives an ACLMessage.
				}
			}
		};
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
										if(!isValidPosition(AIDToTeamId.get(fromAgent), cellToAnalyze)){
											doWait(2*analyzingTime); // because this rover is cheating
											
											ACLMessage inform = MessageHandler.constructReplyMessage(msg, ACLMessage.FAILURE);
											send(inform);

											System.out.println(myAgent.getLocalName() + ": FAILURE is sent to team "+ AIDToTeamId.get(fromAgent));
										}
										else {
											CellAnalysis cellAnalysis = new CellAnalysis();
											cellAnalysis.setCell(worldMap.getCell(m, n));
											
											doWait(analyzingTime); //TODO does this work?
											
											ACLMessage inform = MessageHandler.constructReplyMessage(msg, ACLMessage.INFORM, cellAnalysis);
											send(inform);

											System.out.println(myAgent.getLocalName() + ": INFORM is sent with mineral " + worldMap.getCell(m, n).getMineral());
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
				return (actualLocation.getX() == location.getX() && 
						actualLocation.getY() == location.getY() &&
						roverState.get(team) != State.MOVING);
				//TODO also check if rover is not analyzing while moving, or analysing cell other than the one currently located at
			}
		};
	}
}
