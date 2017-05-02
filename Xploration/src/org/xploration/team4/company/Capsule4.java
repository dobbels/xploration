package org.xploration.team4.company;

import org.xploration.ontology.RegistrationRequest;
import org.xploration.ontology.Team;
import org.xploration.ontology.XplorationOntology;

import jade.content.AgentAction;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;

public class Capsule4 extends Agent {
	

	
	private static final long serialVersionUID = 1L;

	//sources: 
	//  http://paginas.fe.up.pt/~eol/SOCRATES/Palzer/ontologysupportJADE.htm
	//  https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Ontologies.htm
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	protected void setup()
	{
		System.out.println(getLocalName()+": HAS ENTERED");
		
		Object[] args = getArguments();
		int arg1 = (int)args[0]; // Landing of Capsule X-coordinate 
		int arg2 = (int)args[1]; // Landing of Capsule Y-coordinate 
		int arg3 = (int)args[2]; // Dimension of world X
		int arg4 = (int)args[3]; // Dimension of world Y
		
		System.out.println("With arguments: " + Integer.toString(arg1) +  ", " + Integer.toString(arg2) + ", " + Integer.toString(arg3) + ", " + Integer.toString(arg4));
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
		
		// Add a behavior to handle requests form rover
		addBehaviour(new CyclicBehaviour(this) 
		{
			
			private static final long serialVersionUID = 1L;
			
			AID ag;
			
			public void action()
			{   
				System.out.println(getLocalName() + ": Just doing nothing");
				doWait(5000);
			}
			
			// TODO Add sendMessage to Constants files (+ agent argument)

		});
	}
}
