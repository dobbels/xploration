package org.xploration.team4.platform;

import org.xploration.ontology.XplorationOntology;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.Agent;

public class AgMovementSimulator extends Agent{
	
	private static final Ontology ontology = XplorationOntology.getInstance();
	private Codec codec = new SLCodec();
	
	
	public void setup() {
		
	}

}
