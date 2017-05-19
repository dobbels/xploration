package org.xploration.team4.common;

import org.xploration.ontology.XplorationOntology;

import jade.content.AgentAction;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class MessageHandler {
	
	private static Codec codec = new SLCodec();
	private static jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	private static ContentManager cm = new ContentManager();
	
	public static ACLMessage constructMessage(AID ag, int performative, AgentAction agentAction, String protocol) {
		cm.registerLanguage(codec);
        cm.registerOntology(ontology);
        
        Action action = new Action(ag, agentAction);
        
		ACLMessage msg = new ACLMessage(performative);
		
		msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());
        try 
        {
        	cm.fillContent(msg, action);
        	msg.addReceiver(ag);
        	msg.setProtocol(protocol);
        }
        catch (Exception ex) 
        { 
        	ex.printStackTrace(); 
        }
        
        return msg;
	}
	
	public static ACLMessage constructMessage(AID ag, int performative, String protocol) {
		cm.registerLanguage(codec);
        cm.registerOntology(ontology);
        
		ACLMessage msg = new ACLMessage(performative);
		
		msg.setLanguage(codec.getName());
        msg.setOntology(ontology.getName());
        
    	msg.addReceiver(ag);
    	msg.setProtocol(protocol);        
        return msg;
	}
	
	// also one to construct message and return it?
}
