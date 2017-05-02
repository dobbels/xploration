package org.xploration.ontology;

import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

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
