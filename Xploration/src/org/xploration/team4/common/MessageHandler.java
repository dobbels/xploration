package org.xploration.team4.common;

import org.xploration.ontology.XplorationOntology;

import jade.content.AgentAction;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
	
	// TODO constructReplyMessage, 1 with and 1 without an action. 
	
	public static ACLMessage blockingReceive(Agent agent, int performative, String protocol) {
		ACLMessage ans = agent.blockingReceive(MessageTemplate.and(MessageTemplate.MatchProtocol(protocol), 
								MessageTemplate.and(MessageTemplate.MatchPerformative(performative), 
								MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
								MessageTemplate.MatchOntology(ontology.getName())))));
		return ans;
	}
	
	public static ACLMessage blockingReceive(Agent agent, String protocol) {
		ACLMessage ans = agent.blockingReceive(MessageTemplate.and(MessageTemplate.MatchProtocol(protocol),  
								MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
								MessageTemplate.MatchOntology(ontology.getName()))));
		return ans;
	}
}
