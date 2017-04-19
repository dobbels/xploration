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
import jade.lang.acl.MessageTemplate;

import java.util.Date;

public class AgCompany4 extends Agent {
	 
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;
	// The name of the service the Registration Desk agent is using to announce itself in the Yellow Pages
	public final static String REGISTRATION_DESK_NAME = "registrationDesk";
	public final static String REQUEST_SUCCEEDED = "Request: Success";
	public final static String REQUEST_FAULED = "Request: Failure";

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
		addBehaviour(new SimpleBehaviour(this)
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
								System.out.println(getLocalName()+": TRY TO REGISTER");
								
								//TODO process answers and add printing messages with possible outcomes (too late, already registered) ..
								ACLMessage ans = receive(MessageTemplate.MatchPerformative(ACLMessage.AGREE));
								
//								doWait(5000);
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
                try {
                	getContentManager().fillContent(msg, new Action(server, action));
                	msg.addReceiver(ag);
                	send(msg);
                	}
                catch (Exception ex) { ex.printStackTrace(); }
			}
				send(msg);
			}

			public boolean done ()
			{
				return registrationSuccess;
			}

		});


		// Adds a behavior to process the answer to an estimation request
		// The painter with the best estimation will be notified about its acceptation
		// while the rest will receive a reject message

		addBehaviour(new SimpleBehaviour(this)
		{
			private static final long serialVersionUID =1L;			
			boolean end = false;
			boolean thereisestimation = false;
			int best;
			ACLMessage replybest;

			public void action()
			{
				// Waits for the arrival of an answer
				ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
				if (msg != null)
				{
					String estim = msg.getContent();
					if(estim.startsWith(ESTIMATION))
					{
						// If an estimation has arrived, is analysed
						System.out.println(myAgent.getLocalName()+": ESTIMATION RECEIVED"); 
						estim = estim.substring(11);
						int p = Integer.parseInt(estim);

						// If it is the best estimation, it becomes the best
						if (!thereisestimation)
						{
							replybest = msg.createReply();
							best = p;
							thereisestimation = true;							
						}
						// If there was already an estimation, it checks if this new is better
						// If it is better, this becomes the best and it is notified the rejection of the previous one
						// If it is worse, it is notified directly its rejection		
						else
						{
							if (best > p)
							{
								replybest.setPerformative(ACLMessage.REJECT_PROPOSAL);
								myAgent.send(replybest);	
								System.out.println(myAgent.getLocalName()+": SENT ESTIMATION REJECTION ("+best+")");

								replybest = msg.createReply();
								best = p;
							}
							else
							{
								ACLMessage reply = msg.createReply();
								reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
								myAgent.send(reply);
								System.out.println(myAgent.getLocalName()+": SENT ESTIMATION REJECTION ("+p+")");
							}
						}
					}
					// If there was no estimation in the message, answers saying that it was not understood
					else
					{
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
						myAgent.send(reply);
						System.out.println(myAgent.getLocalName()+": ESTIMATION NOT UNDERSTOOD!!!");
						end = false;
					}
				}
				else
				{
					// If 60 seconds have already passed searching for estimations, the best one is accepted
					if ((new Date()).getTime() - registerTime.getTime() >= 60000)
					{
						if (thereisestimation)
						{		
							replybest.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
							myAgent.send(replybest);	
							System.out.println(myAgent.getLocalName()+": SENT ESTIMATION ACCEPTATION ("+best+")");
						}
						else
						{
							System.out.println(myAgent.getLocalName()+": NO PAINTER WAS FOUND");						
						}
						end = true;
					}

					// If no message has yet arrived, the behavior blocks itself
					else
					{
						block();
						end = false;
					}
				}

			}

			public boolean done ()
			{
				return end;
			}

		});

	}
}
