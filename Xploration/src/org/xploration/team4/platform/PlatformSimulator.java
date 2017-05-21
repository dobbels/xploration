package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.xploration.ontology.CapsuleRegistrationInfo;
import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.MovementRequestService;
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
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PlatformSimulator extends Agent {
	
	// TODO think of the case (in the beginning or in registration failure) when not everyone is registered. Don't count on those things.  
	
	// TODO every listening behaviour you put in a thread (http://jade.tilab.com/doc/api/jade/core/behaviours/ThreadedBehaviourFactory.html),
	//			you can just do blockingReceive() ! If there are any problems with messages that don't arrive, then this might be the solution.
	
	// TODO (to be able to use multiple repeating behaviours in the same agent we can use a simple behaviour with done() { return false; }. Then the behaviour is always put in the end of the queue and other behaviour have their turn.) 

	private static final long serialVersionUID = 1L;
	//TODO eventually maybe the difference between variables of 'different' simulators is not important anymore.
	/***COMMON***/
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	Map worldMap = MapReader.readMap();
	
	private int worldDimensionY = worldMap.getWidth(); 
	private int worldDimensionX = worldMap.getHeight();
	
	/***COMM_SIM***/
	
	/***MAP_SIM***/
	private ArrayList<Team> registeredRovers = new ArrayList<>();
	private ArrayList<Team> registeredCapsules = new ArrayList<>();
	
	/***MOVEMENT_SIM + MAP_SIM***/
	private HashMap<Integer, AID> teamAID = new HashMap<Integer, AID>();
	private HashMap<Integer, Cell> roversPosition = new HashMap<Integer, Cell>();
	
	//TODO what to do with this?
	public int initialX = 1;
	public int initialY = 3;
	
	/***TERRAIN_SIM***/
	//For this sprint it remains always true, only INFORM case //TODO change this
	boolean validPosition = true;
	
		
	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");
		
		System.out.println("This is the worldmap:");
		worldMap.printWorldMap();

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		setupCommunciationSim();
		setupMapSim();
		setupMovementSim();
		setupTerrainSim();
		
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
	
	protected void setupCommunciationSim() {
		try{
			//Registration Description of Terrain Simulator
			DFAgentDescription dfd = new DFAgentDescription(); 
			ServiceDescription sd  = new ServiceDescription();
			sd.setType(Constants.COMMUNICATION_SIMULATOR);
			sd.setName(getLocalName());
			dfd.addServices(sd);	
			DFService.register(this, dfd );  
		}catch (FIPAException e){ 
			e.printStackTrace();
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
		}
	}

	protected void setupMapSim() {
		try{
			//Registration Description of Map Simulator
			DFAgentDescription dfd = new DFAgentDescription(); 
			ServiceDescription sd  = new ServiceDescription();
			//
			sd.setType(Constants.MAP_SIMULATOR);
			sd.setName(getLocalName());
			dfd.addServices(sd);	
			DFService.register(this, dfd);  
		}catch (FIPAException e){ 
			e.printStackTrace();
			System.out.println("MAP SIMULATOR REGISTRATION EXCEPTION is detected!"); 
		}
	}

	protected void setupMovementSim() {
		try {
			// Creating registrationDesk description
			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(Constants.MOVEMENT_SIMULATOR);
			dfd.addServices(sd);
			// Registers its description in the DF
			DFService.register(this, dfd);
			
			//fill hashmap for testing purposes
			Cell cell1 = new Cell();
			cell1.setX(1);
			cell1.setY(1);
			Cell cell2 = new Cell();
			cell2.setX(1);
			cell2.setY(3);
			Cell cell3 = new Cell();
			cell3.setX(5);
			cell3.setY(5);
			Cell cell4 = new Cell();
			cell4.setX(1);
			cell4.setY(7);
			roversPosition.put(1, cell1);
			roversPosition.put(2, cell2);
			roversPosition.put(3, cell3);
			roversPosition.put(4, cell4);
			
			System.out.println(getLocalName() + ": registered in the DF");
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	protected void setupTerrainSim() {
		try{
			//Registration Description of Terrain Simulator
			DFAgentDescription dfd = new DFAgentDescription(); 
			ServiceDescription sd  = new ServiceDescription();
			sd.setType(Constants.TERRAIN_SIMULATOR);
			sd.setName(getLocalName());
			dfd.addServices(sd);	
			DFService.register(this, dfd );  
		}catch (FIPAException e){ 
			e.printStackTrace();
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
		}
	}
	
	private Behaviour mapBroadcastListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = -4555719000913759629L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO); 
				
				if (msg != null) {
					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
	
						//print content of message for debugging
						printContent(ce);
						
						//forward map to every rover in range	
						ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
						//TODO is protocol and performative part of content?
						forward.setProtocol(XplorationOntology.MAPBROADCASTSERVICE);
						forward.setLanguage(codec.getName());
						forward.setOntology(ontology.getName());
						try{
							getContentManager().fillContent(forward, ce);
							//TODO send to every rover in range
							// get location of rover who sent the message (msg.getSender, getLocation from movementsimulator ...)
							// get every rover that is in range (given in config file)
							// send to every of these rovers by doing different addReceiver() 
							//send(forward);			                	
						} catch(Exception e){
							e.printStackTrace();
						}
	
						System.out.println(getLocalName() + ": MAPBROADCAST is forwarded");
	
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
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
					System.out.println(getLocalName() + "  x: " + c.getX() + " y: "+ "  min: " + c.getMineral());
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
									int teamId = team.getTeamId();
									teamAID.put(teamId, fromAgent);
									roversPosition.put(teamId, roverLocation);
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
									int teamId = team.getTeamId();
									System.out.println(getLocalName()+ ": Capsule Location is " + capsuleLocation.getX() + "," + capsuleLocation.getY());
								}
							}
						}catch(Exception e){
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
				// TODO Auto-generated method stub
				
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
								Cell claimedCell = ca.getCell();
//								System.out.println(claimedCell.getX() + "  " + claimedCell.getY());
															
								//Exact coordinates for the map
								int m = claimedCell.getX();
								int n = claimedCell.getY();
								
								try {
									//Invalid Cell Condition
									//Checking world boundaries	
									//Checking whether there exists  a mineral or not for that cell											
									if(claimedCell.getX()>worldDimensionY || claimedCell.getY()>worldDimensionX || 
											!(worldMap.getMineral(m,n).equals("A") ||
											worldMap.getMineral(m,n).equals("B") || worldMap.getMineral(m,n).equals("C")|| 
											worldMap.getMineral(m,n).equals("D")))
									
									{
										ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.REFUSE);
//										reply.setContent("REFUSE");
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": REFUSE due to invalid cell");
										doWait(3000);
									}
	
									//Valid Cell Condition
									
									else if(claimedCell.getX()<=worldDimensionY && claimedCell.getY()<=worldDimensionX && (worldMap.getMineral(m,n).equals("A")
											|| worldMap.getMineral(m,n).equals("B") || worldMap.getMineral(m,n).equals("C")||
											worldMap.getMineral(m,n).equals("D")))
									{								
										ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.AGREE);
//										reply.setContent("initial AGREE");
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": Initial AGREEMENT is sent");
	
										//Only INFORM case
										if(validPosition){
											CellAnalysis cellAnalysis = new CellAnalysis();
											cellAnalysis.setCell(claimedCell);
											
											ACLMessage inform = MessageHandler.constructReplyMessage(msg, ACLMessage.INFORM, cellAnalysis);
											send(inform);

											System.out.println(myAgent.getLocalName() + ": INFORM is sent with mineral "+worldMap.getCell(m, n).getMineral());
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
		};
	}
}
