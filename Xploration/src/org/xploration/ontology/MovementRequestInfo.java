package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: MovementRequestInfo
* @author ontology bean generator
* @version 2017/05/22, 22:38:38
*/
public class MovementRequestInfo implements AgentAction {

   /**
* Protege name: cell
   */
   private Cell cell;
   public void setCell(Cell value) { 
    this.cell=value;
   }
   public Cell getCell() {
     return this.cell;
   }

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
