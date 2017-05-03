package org.xploration.team4.company;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.XplorationOntology;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgRover4 extends Agent {
	
	//TODO It should not send the second or third request

	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;

	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();

	//Cell object to claim by Rover
	public Cell myCell = new Cell();

	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//Just a trial for (3,3) coordinates
		myCell.setX(Constants.xCoord);
		myCell.setY(Constants.yCoord);
	    
		//Behaviour is added in the cellAnalysis Method
		cellAnalysis(myCell);
	} 

	private void cellAnalysis(Cell myCell){

		addBehaviour (new SimpleBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			AID agTerrain;

			private boolean claimingCell = false;

			public void action(){

				//A defensive check
				if(!claimingCell){
					//Creates description for the AGENT TERRAIN SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(Constants.TERRAIN_SIMULATOR);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);
						
						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agTerrain = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": terrain simulator agent is found");

							
							CellAnalysis cellAnalysis = new CellAnalysis();
							cellAnalysis.setCell(myCell);
							
							Action cellAction = new Action(agTerrain, cellAnalysis);
												
							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
							
							msg.setLanguage(codec.getName());
			                msg.setOntology(ontology.getName());
			                try{
			                	getContentManager().fillContent(msg, cellAction);
			                	msg.addReceiver(agTerrain);
			                	send(msg);			                	
			                }
			                catch(Exception e){
			                	System.out.println(getLocalName() + " Request Exception");
			                }
							
							/*
							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
							msg.setContent("Request for Cell");
							msg.addReceiver(agTerrain);
							send(msg);
							*/
							System.out.println(getLocalName() + ": REQUEST is sent");
							//doWait(1000);
														
							//Returned answer from Terrain Simulation
							ACLMessage ans = receive();
							if(ans!= null){	  
								if(ans.getPerformative()==ACLMessage.REFUSE)
								{
									System.out.println(getLocalName() + ": REFUSED due to Invalid Cell");
									claimingCell = true;
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									claimingCell = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE)
								{
									System.out.println(getLocalName() + ": Initial AGREE is received");	  

									ACLMessage finalMsg = receive();
									if(finalMsg != null){
										if(finalMsg.getPerformative()==ACLMessage.INFORM)
										{										
											System.out.println(getLocalName()+": INFORM is received!");
											System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
												   +myCell.getX() + ","+ myCell.getY()+ ")");
											claimingCell = true;											
										}
										else{
											System.out.println(getLocalName() + " A problem occured, it should be informed");
										}
									}
									else{//If no message arrives
										block();
									}
								}						  						  						  
							}else{
								//If no message arrives
								block();
							}

						}else{
							System.out.println(getLocalName() + ": No terrain simulator found in yellow pages yet.");
							doWait(5000);
						}

					}catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}				
			}

			//To stop behaviour
			public boolean done() {
				//Cell is claimed
				return claimingCell;
			}        	       	

		});

	}	
}	

