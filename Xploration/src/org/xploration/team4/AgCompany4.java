package org.xploration.team4;

import org.xploration.ontology.*;

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

public class AgCompany4 extends Agent {
	 
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;
	// The name of the service the Registration Desk agent is using to announce itself in the Yellow Pages
	public final static String REGISTRATION_DESK_NAME = "registrationDesk";
//	public final static String REQUEST_SUCCEEDED = "Request: Success";
//	public final static String REQUEST_FAULED = "Request: Failure";

	//sources: 
	//  http://paginas.fe.up.pt/~eol/SOCRATES/Palzer/ontologysupportJADE.htm
	//  https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Ontologies.htm
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	protected void setup()
	{
		System.out.println(getLocalName()+": HAS ENTERED"); //TODO what is LocalName? Better AgCompany4 ?
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
		
		// Add a behavior to register to the registration desk
		// IF registration is successful, it is saved in the boolean
		// ELSE an error message is printed
		addBehaviour(new SimpleBehaviour(this) //TODO should be cyclic to keep trying until a registration desk is found and registration is succesful? (for a certain amount of time maybe)
		{
			
			private static final long serialVersionUID = 1L;
			private boolean registrationSuccess = false;
//			private boolean timedOut = false;
			AID ag;

			public void action()
			{   
				// Only register if not registered yet  
				if (!registrationSuccess) 
				{
					// Creates the description for the type of agent to be searched in the yellow pages
					DFAgentDescription dfd = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(REGISTRATION_DESK_NAME);
					dfd.addServices(sd);
	
					try
					{
						
							// It finds agents of the required type
							DFAgentDescription[] res = new DFAgentDescription[20];
							res = DFService.search(myAgent, dfd);
		
							// Gets the first occurrence (because we assume there to be one)
							if (res.length > 0)
							{
								ag = (AID)res[0].getName();
		
								// Try to register to the desk
								org.xploration.ontology.Team team = new Team();
								team.setTeamId(TEAM_ID);
								org.xploration.ontology.RegistrationRequest regReq = new RegistrationRequest();
								regReq.setTeam(team);
								AgentAction regReqAction = new Action(ag, regReq);
								sendMessage(ACLMessage.REQUEST, regReqAction);
								System.out.println(getLocalName()+": SEND REGISTRATION REQUEST");
								
								//TODO process answers and add printing messages with possible outcomes (too late, already registered) ..
								//TODO how do we know if this is a message from the registration desk? should we add a specifier in the message or something? 
								ACLMessage ans = receive();
								if (ans.getPerformative() == ACLMessage.REFUSE)
								{
									System.out.println(getLocalName()+" WAS REFUSED: TOO LATE TO REGISTER");
								}
								else if (ans.getPerformative() == ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName()+"'S MESSAGE WAS NOT UNDERSTOOD");
								}
								else if (ans.getPerformative() == ACLMessage.AGREE) {
									System.out.println(getLocalName()+": INITIAL AGREEMENT ON REGISTRATION");
									ACLMessage ans2 = receive();
									if (ans2.getPerformative() == ACLMessage.FAILURE)
									{
										System.out.println(getLocalName()+" REGISTRATION FAILED: ALREADY REGISTERED");
									}
									else if (ans2.getPerformative() == ACLMessage.INFORM)
									{
										System.out.println(getLocalName()+": REGISTRATION SUCCESFUL");
										registrationSuccess = true;
									}
								}
								
								doWait(5000);
							}
							else
							{
								// If no registration desk has been found, it waits 5 seconds
								doWait(5000);
							}
						
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
			}
			
			void sendMessage(int performative, AgentAction action) {
				ACLMessage msg = new ACLMessage(performative);
				
				msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());
                try 
                {
                	getContentManager().fillContent(msg, new Action(ag, action));
                	msg.addReceiver(ag);
                	send(msg);
                }
                catch (Exception ex) 
                { 
                	ex.printStackTrace(); 
                }
			}

			public boolean done ()
			{
				return registrationSuccess;
			}

		});
	}
}
