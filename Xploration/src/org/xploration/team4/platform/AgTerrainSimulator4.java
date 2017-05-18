package org.xploration.team4.platform;


import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.Map;

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


//TODO I'don't know why but this agent DOES NOT ENTER the system
//On the internet, it says I need to enter class path but I didn't get it completely
public class AgTerrainSimulator4 extends Agent {

	private static final long serialVersionUID = 1L;

	//For this sprint it remains always true, only INFORM case
	boolean validPosition = true;
		
	Map worldMap = (new MapReader()).readMap();
				
	private int worldWidth = worldMap.getWidth() ; 
	private int worldHeight = worldMap.getHeight();

	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	
	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");
		
		System.out.println("TEST");
		worldMap.printWorldMap();

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

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

		//Adding Behaviour to Setup() Method
		addBehaviour(cellRequestListener());
	}

	private Behaviour cellRequestListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = 11924124L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = blockingReceive(MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
												MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()),
												MessageTemplate.MatchPerformative(ACLMessage.REQUEST)))); 
				
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

								//Creating reply message
								ACLMessage reply = msg.createReply();
								reply.setLanguage(codec.getName());
								reply.setOntology(ontology.getName());
															
								//Exact coordinates for the map
								int m = claimedCell.getX();
								int n = claimedCell.getY();
								
								try {
									//Invalid Cell Condition
									//Checking world boundaries	
									//Checking whether there exists  a mineral or not for that cell											
									if(claimedCell.getX()>worldWidth || claimedCell.getY()>worldHeight || 
											!(worldMap.getMineral(m,n).equals("A") ||
											worldMap.getMineral(m,n).equals("B") || worldMap.getMineral(m,n).equals("C")|| 
											worldMap.getMineral(m,n).equals("D")))
									
									{
										reply.setContent("REFUSE");
										reply.setPerformative(ACLMessage.REFUSE);
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": REFUSE due to invalid cell");
										doWait(3000);
									}
	
									//Valid Cell Condition
									
									else if(claimedCell.getX()<=worldWidth && claimedCell.getY()<=worldHeight && (worldMap.getMineral(m,n).equals("A")
											|| worldMap.getMineral(m,n).equals("B") || worldMap.getMineral(m,n).equals("C")||
											worldMap.getMineral(m,n).equals("D")))
									{								
										reply.setContent("initial AGREE");
										reply.setPerformative(ACLMessage.AGREE);
										myAgent.send(reply);
										System.out.println(myAgent.getLocalName()+": Initial AGREEMENT is sent");
	
										//Only INFORM case
										if(validPosition){
											CellAnalysis cellAnalysis = new CellAnalysis();
											cellAnalysis.setCell(claimedCell);
											
											Action cellAction = new Action(fromAgent, cellAnalysis);

											ACLMessage inform = msg.createReply();
											inform.setPerformative(ACLMessage.INFORM);
											getContentManager().fillContent(inform, cellAction);
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
						ACLMessage reply = msg.createReply();
						reply.setLanguage(codec.getName());
						reply.setOntology(ontology.getName());
						reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName() + ": NOT_UNDERSTOOD is sent");				
					}
				}
				else{
					//if no message arrives
					block();
				}
			}
		};
	}
}

