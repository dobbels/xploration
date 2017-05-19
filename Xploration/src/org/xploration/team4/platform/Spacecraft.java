package org.xploration.team4.platform;

import java.lang.reflect.Constructor;
import java.util.*;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.MessageHandler;
import org.xploration.ontology.Cell;
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
		// private static final long serialVersionUID = 1L;
		
		private static final Ontology ontology = XplorationOntology.getInstance();
		private Codec codec = new SLCodec();
		
		private int mapDimensionX = 10;
		private int mapDimensionY = 10;
		private int missionLength = 10; // mission length in seconds
		
		/******Registration Desk Fields*******/
		// ArrayList to store Registered Agent Teams
		private List<Team> registerationList = new ArrayList<Team>(); //TODO use this list to deploy only the registered companies
		// Registration Duration as 1 minute
		private final int registrationPeriod = 6000;
		Date registerTime;

		boolean checkRegisteredBefore(List<Team> registerationList, Team requestorTeam) {
			boolean check = false;

			for (int i = 0; i < registerationList.size(); i++) {
				if (registerationList.get(i) == requestorTeam) {
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
				// Creating registrationDesk description
				DFAgentDescription dfd = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setName(this.getName());
				sd.setType(Constants.REGISTRATION_DESK_NAME);
				dfd.addServices(sd);
				// Registers its description in the DF
				DFService.register(this, dfd);
				System.out.println(getLocalName() + ": registered in the DF");
			} catch (FIPAException e) {
				e.printStackTrace();
			}
			// Behaviour is addded to the setup() method
			addBehaviour(addRegistrationListener());
			/***END RD SETUP***/
			
//			addBehaviour(createCapsule()); //TODO uncomment ! just for testing purposes! 
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
		
		private Behaviour addRegistrationListener() {
			return new CyclicBehaviour(this) {

				private static final long serialVersionUID = 1L;

				public void action() {
					System.out.println(getLocalName() + ": registration listening behaviour started.");
					//Using codec content language, ontology and request interaction protocol
					ACLMessage msg = MessageHandler.blockingReceive(myAgent, ACLMessage.REQUEST, XplorationOntology.REGISTRATIONREQUEST);
					
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
									System.out.println(myAgent.getLocalName() + ": registration request for team: " + requestorTeam);  
									
									//Creating reply message object. 
									// Receiver, language, ontology and protocol are automatically set. 
									ACLMessage reply = msg.createReply();
									
									//if company agent is still in the registration duration								
									if ((new Date()).getTime() - registerTime.getTime() <= registrationPeriod) {                                           //Not get it Change 
										reply.setPerformative(ACLMessage.AGREE);
										// The ContentManager transforms the java
										// objects into strings
										myAgent.send(reply);
										//Initial Agreement condition is sent
										System.out.println(myAgent.getLocalName() + ": Initial agreement is sent");
										
										// if the team has already registered before
										// Checks the ArrayList of teams
										if (checkRegisteredBefore(registerationList, requestorTeam)) { 
											//FAILURE message is sent										
											ACLMessage finalMsg = MessageHandler.constructMessage(fromAgent, ACLMessage.FAILURE, XplorationOntology.REGISTRATIONREQUEST);
											myAgent.send(finalMsg);
											System.out.println(
													myAgent.getLocalName() + ": failure to registration. Agent has already registered before.");
										} 
										else {
											// if the team hasn't registered yet
											// Add the team to the ArrayList 
											registerationList.add(requestorTeam);
											//INFORM message is sent
											ACLMessage finalMsg = MessageHandler.constructMessage(fromAgent, ACLMessage.INFORM, XplorationOntology.REGISTRATIONREQUEST);
											myAgent.send(finalMsg);
											System.out.println(myAgent.getLocalName() + ": confirmation to registration. Success!");
										}
									}
									else {
										//REFUSE message is sent
										reply.setPerformative(ACLMessage.REFUSE);
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
							ACLMessage reply = msg.createReply();
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							myAgent.send(reply);
							System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");
						}
					}
				}

			};
		}

	}
