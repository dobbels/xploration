package org.xploration.team4.company;

import org.xploration.ontology.*;
import org.xploration.team4.common.Constants;
import org.xploration.team4.common.Map;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Capsule4 extends Agent {
		
	private static final long serialVersionUID = 1L;
	private Cell location = new Cell();
	private int worldWidth;
	private int worldHeight;
	private int missionLength;
	//Map worldMap = new Map();
	
	
	//sources: 
	//  http://paginas.fe.up.pt/~eol/SOCRATES/Palzer/ontologysupportJADE.htm
	//  https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Ontologies.htm
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	protected void setup()
	{
		System.out.println(getLocalName()+": HAS ENTERED");
		/*
		worldMap.printWorldMap();
		try {
			System.out.println("Testing world map mineral: "+ worldMap.getMineral(4, 10));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
			
		Object[] args = getArguments();
		//Type needed to be changed into String
		//Integer type causes program to be crashed
		String arg1 = (String) args[0]; // Landing of Capsule X-coordinate 
		String arg2 = (String) args[1]; // Landing of Capsule Y-coordinate 
		String arg3 = (String) args[2]; // World map X dimension
		String arg4 = (String) args[3]; // World map Y dimension
		String arg5 = (String) args[4]; // the mission length

		
		//Type conversions
		location.setX(Integer.parseInt(arg1));
		location.setY(Integer.parseInt(arg2));
		worldWidth = Integer.parseInt(arg3);
		worldHeight = Integer.parseInt(arg4);
		missionLength = Integer.parseInt(arg5);
		
		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
		
        //addBehaviour(deployRover());
        capsuleRegistration(location);
        
		// Add a behavior to handle requests form rover
        //	addBehaviour(mainBehaviour());
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
	//TODO This function causes a name problem an agent name problem
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
                    Object[] args = new Object[]{x, y, worldWidth, worldHeight};
                    a = cnt.createNewAgent(teamName, className, args);
                    a.start();
                } catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            }
        };
	}
		
	private void capsuleRegistration(Cell myCell){	
		addBehaviour (new SimpleBehaviour(this)
		{	
			private static final long serialVersionUID1 = 3L;

			AID agMapSimulator;

			private boolean capsuleRegistration = false;

			public void action(){
				//A defensive check
				if(!capsuleRegistration){
					//Creates description for the AGENT MAP SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(Constants.MAP_SIMULATOR);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agMapSimulator = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": Map Simulator agent is found");


							CapsuleRegistrationInfo capsuleReg = new CapsuleRegistrationInfo();
							capsuleReg.setCell(myCell);
							//TODO: Type should be integer or team
							capsuleReg.setTeam(Constants.myTeam);

							Action cellAction = new Action(agMapSimulator, capsuleReg);

							ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

							msg.setProtocol(XplorationOntology.CAPSULEREGISTRATIONSERVICE);
							msg.setLanguage(codec.getName());
							msg.setOntology(ontology.getName());
							try{
								getContentManager().fillContent(msg, cellAction);
								msg.addReceiver(agMapSimulator);
								send(msg);			
								System.out.println(getLocalName() + ": INFORM is sent");
								capsuleRegistration = true;
							}
							catch(Exception e){
								System.out.println(getLocalName() + " INFORM Exception");
							}					
						}
						else{
							System.out.println(getLocalName() + ": No map simulator found in yellow pages yet.");
							doWait(5000);
						}
					}
					catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}
			}
			//To stop behaviour
			public boolean done() {
				return capsuleRegistration;
			}
		});
	}
}

