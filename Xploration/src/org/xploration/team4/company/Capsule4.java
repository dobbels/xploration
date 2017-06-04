package org.xploration.team4.company;

import java.util.Iterator;

import org.xploration.ontology.*;
import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;

import jade.content.Concept;
import jade.content.ContentElement;
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
	private int mapDimX;
	private int mapDimY;
	private int missionLength;	
	private int communicationRange;
	
	private Map localWorldMap;
	
	public final static int TEAM_ID = 4;
	
	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
	
	//sources: 
	//  http://paginas.fe.up.pt/~eol/SOCRATES/Palzer/ontologysupportJADE.htm
	//  https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Ontologies.htm
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	protected void setup()
	{
		System.out.println(getLocalName()+": HAS ENTERED");
		
		Object[] args = getArguments();
		int arg1; 
		int arg2; 
		int arg3;
		int arg4;
		int arg5;
		int arg6;
		
		if (args[0] instanceof String) { // To be able to pass arguments in command line
			arg1 = Integer.parseInt((String) args[0]); // Landing of Capsule X-coordinate
			arg2 = Integer.parseInt((String) args[1]); // Landing of Capsule Y-coordinate 
			arg3 = Integer.parseInt((String) args[2]); // World map X dimension
			arg4 = Integer.parseInt((String) args[3]); // World map Y dimension
			arg5 = Integer.parseInt((String) args[4]); // the mission length
			arg6 = Integer.parseInt((String) args[5]); // communication range
		}
		else {
			arg1 = (int) args[0]; // Landing of Capsule X-coordinate 
			arg2 = (int) args[1]; // Landing of Capsule Y-coordinate 
			arg3 = (int) args[2]; // World map X dimension
			arg4 = (int) args[3]; // World map Y dimension
			arg5 = (int) args[4]; // the mission length
			arg6 = (int) args[5]; // communication range
		}
				
		//Type conversions
		location.setX(arg1);
		location.setY(arg2);
		mapDimX = arg3;
		mapDimY = arg4;
		missionLength = arg5;
		communicationRange = arg6;
		
		localWorldMap = new Map(mapDimX, mapDimY);
		
		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        		
        capsuleRegistration(location);
        
        killAgentAtMissionEnd();
	}
	
	private WakerBehaviour killAgentAtMissionEnd() {
		return new WakerBehaviour(this, missionLength*1000) {
			
			 
			private static final long serialVersionUID = 1442964318675336227L;

			protected void onWake() {
				System.out.println(getLocalName() + ": committing suicide");
                myAgent.doDelete();
	        } 
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
					Object[] args = new Object[]{x, y, mapDimX, mapDimY, missionLength, communicationRange};
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
			private static final long serialVersionUID = -7873999374941843621L;

			AID agMapSimulator;

			private boolean capsuleRegistration = false;

			public void action(){
				//A defensive check
				if(!capsuleRegistration){
					//Creates description for the AGENT MAP SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.CAPSULEREGISTRATIONSERVICE);
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
							Team team = new Team();
							team.setTeamId(TEAM_ID);
							capsuleReg.setTeam(team);
							
							ACLMessage msg = MessageHandler.constructMessage(agMapSimulator, ACLMessage.INFORM, capsuleReg, XplorationOntology.CAPSULEREGISTRATIONINFO);
							send(msg);			
							System.out.println(getLocalName() + ": INFORM is sent");
							capsuleRegistration = true;
							
							// Now the rover can be deployed
							addBehaviour(deployRover());
							
							listenForMaps();

							listenRoverClaimCell();
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
			
//			public int onEnd() {
//				addBehaviour(deployRover());
//				return super.onEnd();
//			}
		});
	}
	
	private void listenForMaps(){

		addBehaviour (tbf.wrap(new CyclicBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {				
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO);
				
				if (msg != null) {
					System.out.println(getLocalName() + ": received map broadcast");
					
					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);
						
						if (ce instanceof Action) {
							Concept conc = ((Action) ce).getAction();
							
							if (conc instanceof MapBroadcastInfo) {
								MapBroadcastInfo mbi = (MapBroadcastInfo) conc;
								org.xploration.ontology.Map map = mbi.getMap();
								@SuppressWarnings("rawtypes")
								Iterator it = map.getAllCellList();
								Cell c;
								while (it.hasNext()) {
									c = (Cell) it.next();
									localWorldMap.setCell(c);
								}
//								System.out.println(getLocalName() + ": new local world map");
//								localWorldMap.printWorldMap();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}			
		}));
	}
	//Listens for a claim cell information
	private void listenRoverClaimCell(){
		addBehaviour(tbf.wrap(new CyclicBehaviour(this){

			private static final long serialVersionUID = -6727271021594639998L;

			public void action(){
				
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.CLAIMCELLINFO);
				
				if(msg != null){	
					ContentElement ce;
					
					try{
						ce = getContentManager().extractContent(msg);
						
						if(ce instanceof Action){
							Concept conc = ((Action) ce).getAction();
							if(conc instanceof ClaimCellInfo){			
								
								try{
										ClaimCellInfo cellInfo = (ClaimCellInfo) conc;
										Team claimedTeam = cellInfo.getTeam();
										
										System.out.println(getLocalName()+ ": claim INFORM is received from team " + claimedTeam.getTeamId());
										try{
											//Passes the information to the spacecraft
											cellClaimToSpacecraft(cellInfo);
										}catch(Exception e){
											System.out.println(getLocalName()+ ": Cell claim to capsule Exception");
										}										
								}
								catch(Exception e){
									e.printStackTrace();
									System.out.println(getLocalName()+ ": ERROR about extracting the message");
								}
							}
							else{
								System.out.println(getLocalName()+ ": ERROR about unpacking ClaimCellInfo");
							}
						}
						else{
							System.out.println(getLocalName()+ ": ERROR about unpacking ClaimCellInfo");
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
				else{
					//Empty message is ignored
					block();
				}			
			}							
	   }));
	}
	   	   
	//Passes information to the spacecraft
	private void cellClaimToSpacecraft(ClaimCellInfo cellInfo){
		addBehaviour (new SimpleBehaviour (this){ 

			private static final long serialVersionUID = 5145559126297018013L;
			AID agCommunication;
			private boolean claimCellToSpacecraft = false;

			public void action(){

				if(!claimCellToSpacecraft){
					//Searching for an agent with SPACECRAFTCLAIMSERVICE
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.SPACECRAFTCLAIMSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agCommunication = (AID) result[0].getName();											
							System.out.println(getLocalName()+ ": Spacecraft Claim Service is found");

							try{
								ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, cellInfo, XplorationOntology.CLAIMCELLINFO);
								send(msg);	
								System.out.println(getLocalName() + ": INFORM is sent");
								claimCellToSpacecraft = true;
							}
							catch(Exception e){
								e.printStackTrace();
								System.out.println(getLocalName() + ": INFORM couldn't sent");
							}
						}
						else{
							System.out.println(getLocalName()+ ": No agent found yet!");
							doWait(5000);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}

			@Override
			public boolean done() {
				return claimCellToSpacecraft;
			}		
		});
	}
}


