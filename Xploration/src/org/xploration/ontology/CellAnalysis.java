package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: CellAnalysis
* @author ontology bean generator
* @version 2017/05/22, 22:38:38
*/
public class CellAnalysis implements AgentAction {

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

}
