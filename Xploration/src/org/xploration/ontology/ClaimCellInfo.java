package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: ClaimCellInfo
* @author ontology bean generator
* @version 2017/05/22, 22:38:38
*/
public class ClaimCellInfo implements AgentAction {

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

   /**
* Protege name: map
   */
   private Map map;
   public void setMap(Map value) { 
    this.map=value;
   }
   public Map getMap() {
     return this.map;
   }

}
