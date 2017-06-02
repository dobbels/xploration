package org.xploration.team4.platform;

import java.util.*;

import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;
import org.xploration.ontology.Cell;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.RegistrationRequest;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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

/**
 * 
 * This agent implements the behaviour of the spacecraft and also handles the registration service.
 */
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
	
	private static final long serialVersionUID = 1L;
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	Map map = MapReader.readMap();
	private int mapDimensionX = map.getHeight();
	private int mapDimensionY = map.getWidth();
	private int missionLength = Constants.MISSION_LENGTH; // mission length in seconds
	
	/******Registration Desk Fields*******/
	// ArrayList to store Registered Agent Teams
	private List<Team> registrationList = new ArrayList<Team>();
	// Registration Duration as 1 minute
	private final int registrationPeriod = Constants.REGISTRATION_WINDOW*1000; // in milliseconds
	Date registerTime;
	
	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

	boolean checkRegisteredBefore(List<Team> registerationList, Team requestorTeam) {
		boolean check = false;
		for (int i = 0; i < registerationList.size(); i++) {
			if (registerationList.get(i).getTeamId() == requestorTeam.getTeamId()) {
				check = true;
			}
		}
		return check;
	}
	
	
	public void setup() {
		System.out.println(getLocalName() + ": HAS ENTERED");
		// Starting time for registration duration

		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		/***START RD SETUP***/
		registerTime = new Date();

		try {
			// Creating its description
			DFAgentDescription dfd = new DFAgentDescription();			
			ServiceDescription sd = new ServiceDescription();
			
			sd.setName(this.getName());
			sd.setType(XplorationOntology.REGISTRATIONDESK);
			dfd.addServices(sd);
			
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(XplorationOntology.SPACECRAFTCLAIMSERVICE);
			dfd.addServices(sd);
			
			// Registers its description in the DF
			DFService.register(this, dfd);
														
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		//Behaviour is addded to the setup() method
		Behaviour b = registrationListener();
		addBehaviour(tbf.wrap(b));
		
		//Behaviour for claimcell
		Behaviour r = listenRoverClaimCellFromCapsule();
		addBehaviour(tbf.wrap(r));
		/***END RD SETUP***/
		
		addBehaviour(createCapsulesAfterRegistration());
		
		// After the registration period finishes, the registration desk will actively refuse 
		// companies for a while. After this time, the registration listening behaviour is terminated. 
		addBehaviour(new WakerBehaviour(this, 2*registrationPeriod) {
			private static final long serialVersionUID = 1L;

			protected void handleElapsedTimeout() {
				System.out.println(getLocalName() + ": Registration desk shutdown");
		        removeBehaviour(registrationListener());
		        // TODO remove registrationDesk from the yellow pages?
		      } 
		    });
	}
	
	private WakerBehaviour createCapsulesAfterRegistration() {
		return new WakerBehaviour(this, registrationPeriod) {
			private static final long serialVersionUID = 1L;

			protected void handleElapsedTimeout() {
				String allTeams = "";
				for (Team team : registrationList) {
					allTeams += team.getTeamId();
					allTeams += ", ";
				}
				System.out.println(getLocalName() + ": Registration period is over: for teams " + allTeams + "capsules will be deployed ");
				myAgent.addBehaviour(createCapsules());
	        } 
		};
	}

	private OneShotBehaviour createCapsules() {
		return new OneShotBehaviour(this) {

			private static final long serialVersionUID = -3422396939743596002L;

			public void action() {
				// Create 4 capsules with random initial coordinates
				ArrayList<Cell> assignedCells = new ArrayList<>();
				ArrayList<AgentController> agents = new ArrayList<>();
				Cell currentCell;
				boolean alreadyAssigned;
				
				//Get the JADE runtime interface (singleton)
				jade.core.Runtime runtime = jade.core.Runtime.instance();
				//Create a Profile, where the launch arguments are stored
				Profile profile = new ProfileImpl();
				profile.setParameter(Profile.CONTAINER_NAME, "CapsuleContainer");
				//create a non-main agent container
				ContainerController container = runtime.createAgentContainer(profile);
				
				for (Team team : registrationList) {
					do {
						alreadyAssigned = false;
						currentCell = Constants.generateCoordinate(mapDimensionX, mapDimensionY);
						for (Cell cell : assignedCells) {
							if (cell.getX() == currentCell.getX() && cell.getY() == currentCell.getY()){
								alreadyAssigned = true;
							}
						}
					} while (alreadyAssigned);
					
					assignedCells.add(currentCell);
					try {
						int teamNb = team.getTeamId();
						String teamName = "Capsule" + teamNb;
						String className = "org.xploration.team" + teamNb + ".company."+ teamName;
//							String className = "org.xploration.team" + 4 + ".company."+ teamName;
						//TODO delete next four lines!
//						currentCell.setX(map.getHeight()/2);
//						currentCell.setY(map.getWidth()/2);
						currentCell.setX(5);
						currentCell.setY(5);
						Object[] args = new Object[]{ currentCell.getX(), currentCell.getY(), mapDimensionX, mapDimensionY, missionLength, Constants.COMMUNICATION_RANGE};
				        agents.add(container.createNewAgent(teamName,className, args));
					} catch (StaleProxyException e) {
					    e.printStackTrace();
					}
				}
				
				// Start up agents
				for (AgentController ac : agents){
					try {
						ac.start();
					} catch (StaleProxyException e) {
					    e.printStackTrace();
					}
				}
			}
		};
	}
	
	private Behaviour registrationListener() {
		return new CyclicBehaviour(this) {

			private static final long serialVersionUID = 1L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.REQUEST, XplorationOntology.REGISTRATIONREQUEST);
				
				if (msg != null) {
					// If an REGISTRATION request arrives
					// it answers with the REFUSE, AGREE or NOT_UNDERSTOOD

					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
						// We expect an action inside the message
						if (ce instanceof Action) {
							Action agAction = (Action) ce;
							Concept conc = agAction.getAction();
							// If the action is RegistrationRequest
							if (conc instanceof RegistrationRequest) {
								AID fromAgent = msg.getSender();
								System.out.println(myAgent.getLocalName() + ": received registration request from "
										+ (msg.getSender()).getLocalName());
								//Getting Requestor Team 
								Team requestorTeam = ((RegistrationRequest) conc).getTeam();
								System.out.println(myAgent.getLocalName() + ": registration request for team: " + requestorTeam.getTeamId());  
								
								//if company agent is still in the registration duration								
								if ((new Date()).getTime() - registerTime.getTime() <= registrationPeriod) {                                           //Not get it Change
									ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.AGREE);
									myAgent.send(reply);
									//Initial Agreement condition is sent
									System.out.println(myAgent.getLocalName() + ": Initial agreement is sent");
									
									// if the team has already registered before
									// Checks the ArrayList of teams
									if (checkRegisteredBefore(registrationList, requestorTeam)) { 
										//FAILURE message is sent										
										ACLMessage finalMsg = MessageHandler.constructMessage(fromAgent, ACLMessage.FAILURE, XplorationOntology.REGISTRATIONREQUEST);
										myAgent.send(finalMsg);
										System.out.println(
												myAgent.getLocalName() + ": failure to registration. Agent has already registered before.");
									} 
									else {
										// if the team hasn't registered yet
										// Add the team to the ArrayList 
										registrationList.add(requestorTeam);
										System.out.println(getLocalName() + ": " + registrationList.toString());
										//INFORM message is sent
										ACLMessage finalMsg = MessageHandler.constructMessage(fromAgent, ACLMessage.INFORM, XplorationOntology.REGISTRATIONREQUEST);
										myAgent.send(finalMsg);
										System.out.println(myAgent.getLocalName() + ": confirmation to registration. Success!");
									}
								}
								else {
									//REFUSE message is sent
									ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.REFUSE);
									myAgent.send(reply);
									System.out.println(myAgent.getLocalName() + ": Too late for Registration, refuse is sent");
								}

							} else {
								throw new NotUnderstoodException(msg);		
							}
						} else {
							throw new NotUnderstoodException(msg);
						}
					} catch ( NotUnderstoodException |CodecException | OntologyException e) {
						//NOT_UNDERSTOOD message is sent
						e.printStackTrace();
						ACLMessage reply = MessageHandler.constructReplyMessage(msg, ACLMessage.NOT_UNDERSTOOD);
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}

		};
	}
		
	private Behaviour listenRoverClaimCellFromCapsule(){
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
										System.out.println(getLocalName()+ ": INFORM is received");
										
										ClaimCellInfo cellInfo = (ClaimCellInfo) conc;
										Team claimedTeam = cellInfo.getTeam();
										org.xploration.ontology.Map claimedMap = cellInfo.getMap(); 
																				
										System.out.println(getLocalName()+ ": claimed team is team" + claimedTeam.getTeamId());
										
										//Forward message
										sendClaimToScorer(cellInfo);
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
	
	//Passes information to the spacecraft
	private void sendClaimToScorer(ClaimCellInfo cellInfo){
		addBehaviour (new CyclicBehaviour (this){ 
			//TODO should be simple behaviour. now it keeps doing nothing forever. The searching in yellow pages should then happen in a while loop 

			AID agScorer;
			private boolean forwarded = false;

			public void action(){

				if(!forwarded){
					//Searching for an agent with SPACECRAFTCLAIMSERVICE
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType("scorer");
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agScorer = (AID) result[0].getName();										

							try{
								ACLMessage msg = MessageHandler.constructMessage(agScorer, ACLMessage.INFORM, cellInfo, XplorationOntology.CLAIMCELLINFO);
								send(msg);	
								System.out.println(getLocalName() + ": claim-INFORM is sent");
								forwarded = true;
							}
							catch(Exception e){
								e.printStackTrace();
							}
						}
						else{
							System.out.println(getLocalName()+ ": No scorer found yet!");
							doWait(5000);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}		
		});
	}


}
