package org.xploration.team4.company;

import org.xploration.team4.company.Constants;
import org.xploration.team4.platform.Map;
import org.xploration.ontology.*;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Capsule4 extends Agent {
	

	
	private static final long serialVersionUID = 1L;
	private Cell location = new Cell();
	private int worldWidth;
	private int worldHeight;
	private Map map;

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
		
		location.setX(arg1);
		location.setY(arg2);
		worldWidth = arg3;
		worldHeight = arg4;
		
		try {
			map = new Map(worldWidth, worldHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(getLocalName()+" starting location: "+ Integer.toString(arg1) +  ", " + Integer.toString(arg2));
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
		
        addBehaviour(deployRover());
        
		// Add a behavior to handle requests form rover
//		addBehaviour(mainBehaviour());
	}
	
	private Behaviour mainBehaviour() {
		return new CyclicBehaviour(this) 
		{
			
			private static final long serialVersionUID = 1L;
			
			AID ag;
			
			public void action()
			{   
				System.out.println(getLocalName() + ": Just doing nothing");
				doWait(5000);
			}
			
			// TODO Add sendMessage to Constants files (+ agent argument)

		};
	}
	
	private Behaviour deployRover() {
		return new OneShotBehaviour() {
            
			private static final long serialVersionUID = 1L;

			@Override
            public void action() {
                int x = location.getX();
                int y = location.getY();

                AgentContainer cnt = getContainerController();
                AgentController a;

                try {
                	String teamName = "Rover4";
					String className = this.getClass().getPackage().getName()+".AgRover4";
                    Object[] args = new Object[]{x, y, map.getWidth(), map.getHeight()};
                    a = cnt.createNewAgent(teamName, className, args);
                    a.start();
                } catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            }
        };
	}
}
