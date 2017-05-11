package org.xploration.ontology;

import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: RoverRegistrationService
* @author ontology bean generator
* @version 2017/05/10, 22:02:17
*/
public class RoverRegistrationService extends Service{ 

   /**
* Protege name: roverRegistrationInfo
   */
   private RoverRegistrationInfo roverRegistrationInfo;
   public void setRoverRegistrationInfo(RoverRegistrationInfo value) { 
    this.roverRegistrationInfo=value;
   }
   public RoverRegistrationInfo getRoverRegistrationInfo() {
     return this.roverRegistrationInfo;
   }

}
