package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.xploration.ontology.Cell;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;
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
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
	private HashMap<Integer, Integer> teamScores = new HashMap<>();
	//TODO get AIDs from simulator at registration to check if it's really them (not possible because senderAID is from Spacecraft?)
	
	private long printingRate = 10000;
	
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
		
		listenForClaims();
		
		printScore();
	}
	
	void printScore() {
		// print Team X: score, # of correct cells claimed, # incorrect cells claimed, # late claims regularly
		addBehaviour(new TickerBehaviour(this, printingRate) {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void onStart() {
				System.out.println(getLocalName() + ": Start Printing Scores");
			}

			@Override
			protected void onTick() { // print all teams with the numbers of attempted claims 
				try {
					ArrayList<Integer> teams = new ArrayList<>();
					teams.addAll(nbCorrectClaims.keySet());
					Collections.sort(teams);
					ArrayList<Object[]> table = new ArrayList<>();
					
					table.add(new Object[] {" ", "# correct claims", "# incorrect claims", "# late claims", "## score ##"});
					for (int teamid : teams) {	
						Integer arg2 = 0, arg3 = 0, arg4 = 0;
//						if (nbIncorrectClaims.containsKey(teamid)) 
							arg2 = nbIncorrectClaims.get(teamid);
						
//						if (nbLateClaims.containsKey(teamid))
							arg3 = nbLateClaims.get(teamid);
						arg4 = teamScores.get(teamid);
						table.add(new Object[] {"Team" + teamid, 
								nbCorrectClaims.get(teamid).toString(), 
								arg2.toString(),
								arg3.toString(),
								arg4.toString()});
					}
					
					for (Object[] row : table) {
						System.out.format("%-10s%-20s%-20s%-20s%-20s\n", row);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//TODO sort from highest to lowest? 
		});
		
	}

	void listenForClaims() {
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
								
								if (!nbCorrectClaims.containsKey(teamid))
									nbCorrectClaims.put(teamid, 0);
								if (!nbIncorrectClaims.containsKey(teamid))
									nbIncorrectClaims.put(teamid, 0);
								if (!nbLateClaims.containsKey(teamid))
									nbLateClaims.put(teamid, 0);
								if (!teamScores.containsKey(teamid))
									teamScores.put(teamid, 0);
								
								Cell c;
								while (cellListIterator.hasNext()) {
									c = (Cell) cellListIterator.next();
									
									// In any case check if the claim was correct
									if (correctClaim(c)) {
										if (!alreadyClaimed(c)) {
											nbCorrectClaims.put(teamid, nbCorrectClaims.get(teamid)+1);
											claimedCells.add(c);
											teamScores.put(teamid, teamScores.get(teamid)+1);
										}
										else {
											nbLateClaims.put(teamid, nbLateClaims.get(teamid)+1);
										}
									}
									else {
										nbIncorrectClaims.put(teamid, nbIncorrectClaims.get(teamid)+1);
										teamScores.put(teamid, teamScores.get(teamid)-5);
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
		return (worldMap.getCell(c.getX(), c.getY()).getMineral().equals(c.getMineral()));
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
