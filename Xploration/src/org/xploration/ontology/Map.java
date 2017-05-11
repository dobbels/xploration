package org.xploration.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Map
* @author ontology bean generator
* @version 2017/05/10, 22:02:17
*/
public class Map implements Concept {

   /**
* Protege name: cellList
   */
   private List cellList = new ArrayList();
   public void addCellList(Cell elem) { 
     List oldList = this.cellList;
     cellList.add(elem);
   }
   public boolean removeCellList(Cell elem) {
     List oldList = this.cellList;
     boolean result = cellList.remove(elem);
     return result;
   }
   public void clearAllCellList() {
     List oldList = this.cellList;
     cellList.clear();
   }
   public Iterator getAllCellList() {return cellList.iterator(); }
   public List getCellList() {return cellList; }
   public void setCellList(List l) {cellList = l; }

}
