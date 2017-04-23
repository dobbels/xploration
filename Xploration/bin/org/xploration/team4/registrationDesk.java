import jade.core.Agent;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.behaviours.*;
import java.util.Date;

//import org.xploration.team4.DFAgentDescription;

public class registrationDesk extends Agent {

	private static final long serialVersionUID1 = 1L;
	public final static String Company = "Company";
	final Date registerTimeStarts; 
	final int registrationPeriod = 60000; 						//Registration Duration as 1 minute

	protected void setup(){

		System.out.println(getLocalName()+": has entered into the system");
		registerTimeStarts = new Date();                        //Current Date

		DFAgentDescription dfd;
		ServiceDescription sd;
		try{
			// Creating its own description
			dfd = new DFAgentDescription();
			sd = new ServiceDescription();
			sd.setName(this.getName());
			sd.setType(Company);
			dfd.addServices(sd);
			// Registering its description in the DF
			DFService.register(this, dfd);
			System.out.println(getLocalName()+": registered in the DF");
			dfd = null;
			sd = null;
			doWait(10000); 				//Waits for 10 seconds
		}
		catch (FIPAException e)
		{
			e.printStackTrace();
		}

		public void action()
		{			
			ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			if (msg != null)
			{
				// If a request successfully arrives...
				System.out.println(getLocalName()+ " received request from "+(msg.getSender()).getLocalName());

				// If initially agrees to the request
				if ((new Date()).getTime() - registerTimeStarts.getTime() <= registrationPeriod)
				{
					System.out.println(getLocalName()+": Initial Agreement to "+(msg.getSender()).getLocalName());

					DFAgentDescription[] res = new DFAgentDescription[20];
					res = DFService.search(myAgent, dfd);             //I Tried to use a search function

					if (search(myAgent,dfd){
						System.out.println(getLocalName()+": Registration Failure already registered!");
					}							
					else
					{
						System.out.println(getLocalName()+": Registration Inform is done!");
					}

				}

				// If rejects to the request... 
				else if ((new Date()).getTime() - registerTimeStarts.getTime() > registrationPeriod)
				{
					System.out.println(getLocalName()+": Rejection to "+(msg.getSender()).getLocalName());
					System.out.println(getLocalName()+": Too late to register "+(msg.getSender()).getLocalName());
				}

				// If what is received is not understood
				ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.NOT_UNDERSTOOD));
				if (msg != null)
				{
					// If a not understood message arrives...
					System.out.println(getLocalName()+": received NOT_UNDERSTOOD from "+(msg.getSender()).getLocalName());
				}

			}
			else
			{
				// If no message arrives
				block();
			}

		}
	});
}
}






