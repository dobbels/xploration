package org.xploration.ontology;

import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: MovementRequestService
* @author ontology bean generator
* @version 2017/05/10, 22:02:17
*/
public class MovementRequestService extends Service{ 

   /**
* Protege name: movementRequestInfo
   */
   private MovementRequestInfo movementRequestInfo;
   public void setMovementRequestInfo(MovementRequestInfo value) { 
    this.movementRequestInfo=value;
   }
   public MovementRequestInfo getMovementRequestInfo() {
     return this.movementRequestInfo;
   }

}
