package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: CellAnalysis
* @author ontology bean generator
* @version 2017/04/27, 10:39:47
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
