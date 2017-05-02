package org.xploration.team4.platform;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.*;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
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

public class AgTerrainSimulator4 extends Agent {

	private static final long serialVersionUID = 1L;
	public static final String terrainSimulatorName = "agTerrain";
	//For this sprint it remains always true, only INFORM case
	boolean validPosition = true;

	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	protected void setup(){
				
		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		try{
		//Registration Description of Terrain Simulator
		DFAgentDescription dfd = new DFAgentDescription(); 
		ServiceDescription sd  = new ServiceDescription();
		sd.setType(terrainSimulatorName);
		sd.setName(getLocalName());
		dfd.addServices(sd);	
		DFService.register(this, dfd );  
		}catch (FIPAException e){ 
			e.printStackTrace();
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
		}
		
		//Makes the agent KILLED!
		//Map 5X5 sizes with A,B,C,D minerals
		/*
		String [][] multi = new String [5][5];	
		multi[1][1] = "A"; multi[1][3] = "B"; multi[1][5] = "C";
		multi[2][2] = "B"; multi[2][4] = "C"; 
		multi[3][1] = "D"; multi[3][3] = "B"; multi[3][5] = "D";
		multi[4][2] = "C"; multi[4][4] = "A";
		multi[5][1] = "D"; multi[5][3] = "C"; multi[5][5] = "A";	
		*/
		//Adding Behaviour to Setup() Method
		addBehaviour(cellRequestListener());
	}

	private Behaviour cellRequestListener() {
		return new CyclicBehaviour(this) {

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = blockingReceive(MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
						MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()),
								MessageTemplate.MatchPerformative(ACLMessage.REQUEST)))); 

				if (msg != null )
				{
					// If a Cell Claiming request arrives
					// it answers with the REFUSE, AGREE or NOT_UNDERSTOOD
					
					// storing the message sender agent
					AID fromAgent = msg.getSender();
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
								System.out.println(getLocalName()+": CellAnalysis REQUEST is received from " + 
										(msg.getSender()).getLocalName());

								CellAnalysis ca = (CellAnalysis) conc;
								//claimedCell has the (x,y) coordinates to check validity
								Cell claimedCell = ca.getCell();

								//Creating reply message
								ACLMessage reply = msg.createReply();
								reply.setLanguage(codec.getName());
								reply.setOntology(ontology.getName());

								//Invalid Cell Condition
								//Checking world boundaries 5x5
								//UPDATE THIS CHECK
								if(claimedCell.getX()>5 || claimedCell.getY()>5 || 
										(!(claimedCell.getMineral()=="A" || claimedCell.getMineral()=="B"||
										claimedCell.getMineral()== "C"|| claimedCell.getMineral()== "D")))
								{
									reply.setContent("REFUSE");
									reply.setPerformative(ACLMessage.REFUSE);
									myAgent.send(reply);
									System.out.println(myAgent.getLocalName()+": REFUSE due to invalid cell");
								}

								//Valid Cell Condition
								//Checking world boundaries
								//UPDATE THIS CHECK
								else if(claimedCell.getX()<= 5 && claimedCell.getY()<=5 && 
										(claimedCell.getMineral()=="A") || claimedCell.getMineral()== "B" ||
										claimedCell.getMineral()== "C" ||	claimedCell.getMineral()=="D"){
									reply.setContent("initial AGREE");
									reply.setPerformative(ACLMessage.AGREE);
									myAgent.send(reply);
									System.out.println(myAgent.getLocalName()+":Initial AGREEMENT is sent");
									
									//Only INFORM case
									if(validPosition){
										ACLMessage finalMsg = new ACLMessage(ACLMessage.INFORM);
										finalMsg.addReceiver(fromAgent);
										finalMsg.setLanguage(codec.getName());
										finalMsg.setOntology(ontology.getName());
										send(finalMsg);
										System.out.println(myAgent.getLocalName() + ": confirmation to claiming cell!");
									}								
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
			}
		};
	}
}

