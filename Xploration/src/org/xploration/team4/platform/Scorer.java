package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.runners.model.Annotatable;
import org.xploration.ontology.CapsuleRegistrationInfo;
import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.RegistrationRequest;
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

public class Scorer extends Agent {
	
	// TODO send score to spacecraft in the end ?
	
	private static final long serialVersionUID = 1L;
	
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	Map worldMap = MapReader.readMap(); //TODO send this from platform simulator through a proper ACLMessage ?
	
	private int worldDimensionY = worldMap.getWidth(); 
	private int worldDimensionX = worldMap.getHeight();
	
	private ArrayList<Cell> claimedCells = new ArrayList<>();
	private HashMap<Integer, Integer> nbCorrectClaims = new HashMap<>();
	private HashMap<Integer, Integer> nbIncorrectClaims = new HashMap<>();
	private HashMap<Integer, Integer> nbLateClaims = new HashMap<>();
	//TODO get AIDs from simulator at registration to check if it's really them (not possible because senderAID is from Spacecraft?)
	
	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");
		
		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		try
		{
			DFAgentDescription dfd = new DFAgentDescription();
			
			ServiceDescription sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType("scorer");
			dfd.addServices(sd);
			DFService.register(this, dfd);
		} catch (FIPAException e){
			e.printStackTrace();
		}
		
		receiveClaim();
		
		//TODO add behaviour(s) here
	}
	
	void receiveClaim() {
		addBehaviour(new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				// TODO Auto-generated method stub
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.CLAIMCELLINFO);
				
				if (msg!= null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						
						if (ce instanceof Action) {
							Action agAction = (Action) ce;
							Concept conc = agAction.getAction();
							// If the action is RegistrationRequest
							if (conc instanceof ClaimCellInfo) {
								ClaimCellInfo cci = (ClaimCellInfo) conc;
								
								Team team = cci.getTeam();
								Iterator cellListIterator = cci.getMap().getAllCellList();
								int teamid = team.getTeamId();
								
								Cell c;
								while (cellListIterator.hasNext()) {
									c = (Cell) cellListIterator.next();
									
									// In any case check if the claim was correct
									if (correctClaim(c)) {
										if (!alreadyClaimed(c)) {
											nbCorrectClaims.put(teamid, nbCorrectClaims.put(teamid, nbCorrectClaims.get(teamid)));
											claimedCells.add(c);
										}
										else {
											if (!nbLateClaims.containsKey(teamid))
												nbLateClaims.put(teamid, 1);
											else
												nbLateClaims.put(teamid, nbLateClaims.get(teamid)+1);
										}
									}
									else {
										nbIncorrectClaims.put(teamid, nbIncorrectClaims.put(teamid, nbIncorrectClaims.get(teamid)));
										if (alreadyClaimed(c)) { //TODO necessary?
											if (!nbLateClaims.containsKey(teamid))
												nbLateClaims.put(teamid, 1);
											else
												nbLateClaims.put(teamid, nbLateClaims.get(teamid)+1);
										}
									}
								}
								
							} else {
								throw new NotUnderstoodException(msg);		
							}
						} else {
							throw new NotUnderstoodException(msg);
						}
					} catch (NotUnderstoodException | OntologyException | CodecException e) {
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
		});							
		
	}

	protected boolean correctClaim(Cell c) {
		return (worldMap.getCell(c.getX(), c.getY()).getMineral() == c.getMineral());
	}

	protected boolean alreadyClaimed(Cell c) {
		int x = c.getX();
		int y = c.getY();
		
		for (Cell cell : claimedCells) {
			if (cell.getX() == x && cell.getY() == y)
				return true;
		}
		
		return false;
	}

}
