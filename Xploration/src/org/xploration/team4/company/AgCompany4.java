package org.xploration.team4.company;

import org.xploration.ontology.*;
import org.xploration.team4.common.MessageHandler;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AgCompany4 extends Agent {
	 
	private static final long serialVersionUID = 1L;
	
	public final static int TEAM_ID = 4;

	//sources: 
	//  http://paginas.fe.up.pt/~eol/SOCRATES/Palzer/ontologysupportJADE.htm
	//  https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Ontologies.htm
	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();
	protected void setup()
	{
		System.out.println(getLocalName()+": HAS ENTERED");
		
		getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
		
		// Add a behavior to register to the registration desk
		// IF registration is successful, it is saved in the boolean
		// ELSE an error message is printed
		addBehaviour(register());
		
		// To test if registration desk correctly sends FAILURE
//		addBehaviour(register());
	}
	
	private SimpleBehaviour register() {
		return new SimpleBehaviour(this)
		{
			private static final long serialVersionUID = 1L;
			private boolean registrationSuccess = false;
			private boolean registrationFailure = false;
//			private boolean timedOut = false;
			AID ag;

			public void action()
			{   
				// Only register if not registered yet  
				if (!(registrationSuccess || registrationFailure)) //TODO delete this. It is unnecessary as the behaviour is stopped in this case anyway?
				{
					// Creates the description for the type of agent to be searched in the yellow pages
					DFAgentDescription dfd = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.REGISTRATIONDESK);
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
								Team team = new Team();
								team.setTeamId(TEAM_ID);
								RegistrationRequest regReq = new RegistrationRequest();
								regReq.setTeam(team);
								
								send(MessageHandler.constructMessage(ag, ACLMessage.REQUEST, regReq, XplorationOntology.REGISTRATIONREQUEST));
								
								System.out.println(getLocalName()+": SEND REGISTRATION REQUEST");
								
								//TODO how do we know if this is a message from the registration desk? should we add a specifier in the message or something? 
								ACLMessage ans = blockingReceive(MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
												MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()), 
												MessageTemplate.MatchProtocol(XplorationOntology.REGISTRATIONREQUEST))));
								if (ans.getPerformative() == ACLMessage.REFUSE)
								{
									System.out.println(getLocalName()+" WAS REFUSED: TOO LATE TO REGISTER");
									registrationFailure = true;
								}
								else if (ans.getPerformative() == ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName()+"'S MESSAGE WAS NOT UNDERSTOOD");
									registrationFailure = true;
								}
								else if (ans.getPerformative() == ACLMessage.AGREE) {
									System.out.println(getLocalName()+": INITIAL AGREEMENT ON REGISTRATION");
									ACLMessage ans2 = blockingReceive(MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()),
											MessageTemplate.and(MessageTemplate.MatchOntology(ontology.getName()), 
											MessageTemplate.MatchProtocol(XplorationOntology.REGISTRATIONREQUEST))));
									if (ans2.getPerformative() == ACLMessage.FAILURE)
									{
										System.out.println(getLocalName()+" REGISTRATION FAILED: ALREADY REGISTERED");
										registrationFailure = true;
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

			public boolean done ()
			{
				return registrationSuccess || registrationFailure;
			}

            public int onEnd() {
            	System.out.println(getLocalName() + ": committing suicide");
                myAgent.doDelete();
                return super.onEnd();
			}
		};
	}
}
