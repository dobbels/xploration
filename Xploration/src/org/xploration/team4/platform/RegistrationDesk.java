
package org.xploration.team4.platform;

import java.util.*;

import org.xploration.ontology.RegistrationRequest;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;

import jade.content.*; 
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.*; 
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.*; 

public class RegistrationDesk extends Agent {

	// private static final long serialVersionUID = 1L;
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	// ArrayList to store Registered Agent Teams
	private List<Team> registerationList = new ArrayList<Team>();
	// Registration Duration as 1 minute
	private final int registrationPeriod = 6000;

	boolean checkRegisteredBefore(List<Team> registerationList, Team requestorTeam) {
		boolean check = false;

		for (int i = 0; i < registerationList.size(); i++) {
			if (registerationList.get(i) == requestorTeam) {
				check = true;
			}
		}
		return check;
	}

	Date registerTime;

	public void setup() {
		System.out.println(getLocalName() + ": has entered into the system");
		// Starting time for registration duration
		registerTime = new Date();

		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

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
	}

	private Behaviour addRegistrationListener() {
		return new CyclicBehaviour(this) {

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = blockingReceive(MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
								MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()),
								MessageTemplate.MatchPerformative(ACLMessage.REQUEST))));              
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
								
								//Creating reply message objcet
								ACLMessage reply = msg.createReply();
								reply.setLanguage(codec.getName());
								reply.setOntology(ontology.getName());
								
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
										ACLMessage finalMsg = new ACLMessage(ACLMessage.FAILURE);
										finalMsg.addReceiver(fromAgent);                                
										finalMsg.setLanguage(codec.getName());
										finalMsg.setOntology(ontology.getName());
										myAgent.send(finalMsg);
										System.out.println(
												myAgent.getLocalName() + ": failure to registration. Agent has already registered before.");
									} 
									else {
										// if the team hasn't registered yet
										// Add the team to the ArrayList 
										registerationList.add(requestorTeam);
										//INFORM message is sent
										ACLMessage finalMsg = new ACLMessage(ACLMessage.INFORM);
										finalMsg.addReceiver(fromAgent);
										finalMsg.setLanguage(codec.getName());
										finalMsg.setOntology(ontology.getName());
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
						AID fromAgent = msg.getSender();
						ACLMessage reply = msg.createReply();
						reply.setLanguage(codec.getName());
						reply.setOntology(ontology.getName());
						reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");
					}
				}
			}

		};
	}
}
