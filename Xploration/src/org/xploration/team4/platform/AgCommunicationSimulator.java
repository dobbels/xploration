package org.xploration.team4.platform;

import java.awt.AlphaComposite;
import java.util.Iterator;

import org.xploration.ontology.Cell;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.MovementRequestService;
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
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgCommunicationSimulator extends Agent {
	
	private static final long serialVersionUID = 1L;
	private Codec codec = new SLCodec();
    private Ontology ontology = XplorationOntology.getInstance();
    
    protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		try{
			//Registration Description of Terrain Simulator
			DFAgentDescription dfd = new DFAgentDescription(); 
			ServiceDescription sd  = new ServiceDescription();
			sd.setType(Constants.COMMUNICATION_SIMULATOR);
			sd.setName(getLocalName());
			dfd.addServices(sd);	
			DFService.register(this, dfd );  
		}catch (FIPAException e){ 
			e.printStackTrace();
			System.out.println("REGISTRATION EXCEPTION is detected!"); 
		}

		addBehaviour(mapBroadcastListener());
	}

	private Behaviour mapBroadcastListener() {
		return new CyclicBehaviour(this) {
			
			private static final long serialVersionUID = -4555719000913759629L;

			public void action() {
				//Using codec content language, ontology and request interaction protocol
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO); 
				
				if (msg != null) {
					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
	
						//print content of message for debugging
						printContent(ce);
						
						//forward map to every rover in range	
						ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
						//TODO is protocol and performative part of content?
						forward.setProtocol(XplorationOntology.MAPBROADCASTSERVICE);
						forward.setLanguage(codec.getName());
						forward.setOntology(ontology.getName());
						try{
							getContentManager().fillContent(forward, ce);
							//TODO send to every rover in range
							// get location of rover who sent the message (msg.getSender, getLocation from movementsimulator ...)
							// get every rover that is in range (given in config file)
							// send to every of these rovers by doing different addReceiver() 
							//send(forward);			                	
						} catch(Exception e){
							e.printStackTrace();
						}
	
						System.out.println(getLocalName() + ": MAPBROADCAST is forwarded");
	
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}

			private void printContent(ContentElement ce) {
				Action agAction = (Action) ce;
				Concept conc = agAction.getAction();
				
				System.out.println(getLocalName()+": BroadCasted Cells: ");

				MapBroadcastInfo mbi  = (MapBroadcastInfo) conc;
				
				org.xploration.ontology.Map map = mbi.getMap();
				
				Iterator it = map.getAllCellList();
				Cell c;
				while (it.hasNext()) {
					c = (Cell) it.next();
					System.out.println(getLocalName() + "  x: " + c.getX() + " y: "+ "  min: " + c.getMineral());
				}

			}
		};
	}

}
