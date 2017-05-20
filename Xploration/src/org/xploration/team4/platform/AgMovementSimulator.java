package org.xploration.team4.platform;

import java.util.HashMap;

import org.xploration.ontology.Cell;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;
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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgMovementSimulator extends Agent{
	
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	public int dimX = 5;
	public int dimY = 10;
	public int initialX = 1;
	public int initialY = 3;
	
	private HashMap<Integer, AID> teamAID = new HashMap<Integer, AID>();
	private HashMap<Integer, Cell> roversPosition = new HashMap<Integer, Cell>();
	
	
	public void setup() {
		
		System.out.println(getLocalName() + ": has entered into the system");
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
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
		
		addBehaviour(MovementListener());
		
	}
	
	private Behaviour MovementListener() {
		return new CyclicBehaviour(this) {
			
			@Override
			public void action() {
				// TODO Auto-generated method stub
				
				ACLMessage msg = MessageHandler.blockingReceive(myAgent, ACLMessage.REQUEST, XplorationOntology.MOVEMENTREQUESTINFO); 
				 
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
								if (Constants.isExistingCoordinate(dimX, dimY, destination.getX(), destination.getY()) 
										&& Constants.isNextPosition(dimX, dimY, initialX, initialY, destination.getX(), destination.getY())) {
									
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
				
			}
		};
	}

}
