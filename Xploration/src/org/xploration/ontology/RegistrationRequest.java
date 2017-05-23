package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: RegistrationRequest
* @author ontology bean generator
* @version 2017/05/22, 22:38:38
*/
public class RegistrationRequest implements AgentAction {

   /**
* Protege name: team
   */
   private Team team;
   public void setTeam(Team value) { 
    this.team=value;
   }
   public Team getTeam() {
     return this.team;
   }

}
