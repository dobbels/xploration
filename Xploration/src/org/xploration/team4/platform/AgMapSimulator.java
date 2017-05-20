package org.xploration.team4.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xploration.ontology.CapsuleRegistrationInfo;
import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.xploration.team4.common.*;

public class AgMapSimulator extends Agent {

	private static final long serialVersionUID = 1L;

	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	private HashMap<Integer, AID> teamAID = new HashMap<Integer, AID>();
	private HashMap<Integer, Cell> roversPosition = new HashMap<Integer, Cell>();

	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

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
		
		addBehaviour(roverRequestListener());
		addBehaviour(capsuleRequestListener());		
	}
	
	private Behaviour roverRequestListener() {
		return new CyclicBehaviour(this) {

			public void action() {
				
				//RoverRegistrationService Protocol
				ACLMessage msg = MessageHandler.blockingReceive(myAgent, XplorationOntology.ROVERREGISTRATIONINFO);

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
			}
		};		
	}
		
	private Behaviour capsuleRequestListener() {
		return new CyclicBehaviour(this) {

			public void action() {
				
				//capsuleRegistrationService Protocol
				ACLMessage msg = MessageHandler.blockingReceive(myAgent, XplorationOntology.CAPSULEREGISTRATIONSERVICE);

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
			}
		};
	}
}
