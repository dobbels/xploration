package org.xploration.team4.company;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.xploration.ontology.Cell;
import org.xploration.ontology.CellAnalysis;
import org.xploration.ontology.ClaimCellInfo;
import org.xploration.ontology.MapBroadcastInfo;
import org.xploration.ontology.MovementRequestInfo;
import org.xploration.ontology.RoverRegistrationInfo;
import org.xploration.ontology.XplorationOntology;
import org.xploration.team4.common.Map;
import org.xploration.team4.common.MessageHandler;
import org.xploration.ontology.Team;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.WakerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

//TODO search for all the services at the startup of this agent (in a behaviour) !! Not every time you do an action of course! 
//TODO solve OntologyException
public class AgRover4 extends Agent {

	private int worldDimY;
	private int worldDimX;
	private int missionLength;
	private int communicationRange;
	
	private static final long serialVersionUID = 1L;
	public final static int TEAM_ID = 4;

	enum State {
		MOVING, ANALYZING, OTHER
	}

	private State state = State.OTHER;
	private Cell location = new Cell();
	private Cell capsuleLocation = new Cell();
	private ArrayList<Cell> analyzedCells = new ArrayList<>();
	private ArrayList<Cell> claimedCells = new ArrayList<>();
	private boolean alreadyClaiming = false;
	
	private ArrayList<Cell> nextMovements = new ArrayList<Cell>();
	private ArrayList<String> directions = new ArrayList<String>();
	private boolean firstBehaviourUseless = false;
	
	private boolean movingInRangeToClaim = false;

	private Map localWorldMap; 

	ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

	private Codec codec = new SLCodec();
	private jade.content.onto.Ontology ontology = XplorationOntology.getInstance();

	protected void setup(){

		System.out.println(getLocalName() + ": HAS ENTERED");

		//Register Language and Ontology
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		Object[] args = getArguments();
		int arg1; 
		int arg2; 
		int arg3;
		int arg4;
		int arg5;
		int arg6;
		
		if (args[0] instanceof String) { // To be able to pass arguments in command line
			arg1 = Integer.parseInt((String) args[0]); // Landing of Capsule X-coordinate
			arg2 = Integer.parseInt((String) args[1]); // Landing of Capsule Y-coordinate 
			arg3 = Integer.parseInt((String) args[2]); // World map X dimension
			arg4 = Integer.parseInt((String) args[3]); // World map Y dimension
			arg5 = Integer.parseInt((String) args[4]); // the mission length
			arg6 = Integer.parseInt((String) args[5]); // communication range
		}
		else {
			arg1 = (int) args[0]; // Landing of Capsule X-coordinate 
			arg2 = (int) args[1]; // Landing of Capsule Y-coordinate 
			arg3 = (int) args[2]; // World map X dimension
			arg4 = (int) args[3]; // World map Y dimension
			arg5 = (int) args[4]; // the mission length
			arg6 = (int) args[5]; // communication range
		}
				
		//Type conversions
		location.setX(arg1); capsuleLocation.setX(arg1);
		location.setY(arg2); capsuleLocation.setY(arg2);
		worldDimX = arg3;
		worldDimY = arg4;
		missionLength = arg5;
		communicationRange = arg6;
		
		localWorldMap = new Map(worldDimX, worldDimY);

		System.out.println(getLocalName()+": starting location: "+ arg1 +  "," + arg2);
		System.out.println(getLocalName()+": missionLength: "+ arg5);
		System.out.println(getLocalName()+": communicationRange: "+ arg6);
		
		directions.add("rightUp");
		directions.add("rightDown");
		directions.add("down");
		directions.add("leftDown");
		directions.add("leftUp");
		directions.add("up");
		
		//roverRegistration for Map Simulator
	    roverRegistration(location);	    
	} 
	
	private void startMainBehaviour() {
		addBehaviour (new CyclicBehaviour(this) {

			@Override
			public void onStart() {
				addBehaviour(tbf.wrap(killAgentAtMissionEnd()));
				super.onStart();
			}
			
			@Override
			public void action() {
				if (nextMovements.isEmpty()) {
					if (!firstBehaviourUseless && 
							(localWorldMap.distance(location, capsuleLocation)+1) <= Math.min(localWorldMap.getHeight()/4, localWorldMap.getWidth()/2)) {
						System.out.println(getLocalName() + ": calculating next cells (for distance " + (localWorldMap.distance(location, capsuleLocation)+1) + ")");
						nextMovements = calculateBorderCells(location, localWorldMap.distance(location, capsuleLocation)+1); 
//						System.out.println(nextMovements);
					}
//					else if (firstBehaviourUseless) {
//						System.out.println(getLocalName() + ": calculating next cannon ball");
//						nextMovements = calculateCanonBallCells(location);
//					}
					else if (localWorldMap.distance(location, capsuleLocation)+1 >= Math.min(localWorldMap.getHeight()/4, localWorldMap.getWidth()/2)) {
						// To make sure first behaviour is not restarted, just because we are in range again at some point
						firstBehaviourUseless = true;
					}
				}
				else {
					if (!analyzing() && !moving() && currentCellAnalyzed()) {
						System.out.println(getLocalName() + ": requesting movement");
						if (state == State.ANALYZING && state == State.MOVING) {
							System.out.println(getLocalName() + ": ERROR! Moving while analyzing");
						}
						state = State.MOVING;
						requestMovement(nextMovements.get(0));
					} 
				}
				
				if (!movingInRangeToClaim && ! moving() //TODO do this differently once the second behaviour has started? Also broadcast analyzed cells when that behaviour started?
						&& analyzedCells.size() > 3*(localWorldMap.distance(location, capsuleLocation) - communicationRange) 
						//TODO better smaller analyze size?! Just a disadvantage then if communication range is 1? because of first tour
						&& !localWorldMap.inRangeFrom(location, capsuleLocation, communicationRange)) {
					// + conditions: not in range and not yet going in range
					ArrayList<Cell> toGoBackInRange = goBackInRange(location);
//					if (nextMovements.get(0) != location) { //TODO make check for if it is last in nextMovements by accident 
					ArrayList<Cell> backToPath = shortestPathBetween(toGoBackInRange.get(toGoBackInRange.size()-1), nextMovements.get(0));
					backToPath.remove(backToPath.size()-1); // otherwise the destination is added twice
					ArrayList<Cell> thereAndBack = new ArrayList<>(toGoBackInRange);
					thereAndBack.addAll(backToPath);
					
//					Collections.reverse(toGoBackInRange);
//					toGoBackInRange.remove(0);
//					thereAndBack.addAll(toGoBackInRange);
//					thereAndBack.add(location);
					//TODO better: from the last cell of (toGoBack): calculate shortest path to next cell that were going to do

					nextMovements.addAll(0, thereAndBack);

//					System.out.println("New next movements");
//					for (int i = 0; i < nextMovements.size(); i++) {
//						System.out.println(nextMovements.get(i).getX() + ", " + nextMovements.get(i).getY());
//					}					
					movingInRangeToClaim = true;
					// TODO merge cells in path
					
					// TODO check in printstatement of alle cellen aaneensluitend zijn
				}
				
				if (!moving() && !analyzing() && !currentCellAlreadyHandled()) {
					System.out.println(getLocalName() + ": analyzing");
					if (state == State.MOVING && state == State.ANALYZING) {
						System.out.println(getLocalName() + ": ERROR! Analyzing while moving");
					}
					state = State.ANALYZING;
					analyzeCurrentCell();
				}
				
				// nextMovements.remove(0); is done in requestMovement after an inform. If a failure is received, the same cell will be tried on the next iteration.
				//TODO if refused or failed, calculate new nextMovements from current location? Or relative to capsule loc? 
				
				// TODO add exploration-algorithm-logic
				// we could move in spiral + radio range level if no other rovers are around
				// if rovers are around move in spiral starting close to where they are so that they don't move to our position
				
				// maybe never go back in range of capsule, except when the end of the mission is approaching. 
				// if the world is small, maybe it's even best to just go around the whole time, or mark your territory first and then 
				// start filling in the inside. To calculate the size of this territory you could make some calculation based on missionlength, analyzingTime and movementTime (oh maybe these last ones you don't know).
				// + claim cells you get in map broadcast. Just in case they broadcast it right after analyzing, not after claiming
			}
		});
	}
	
	/*
	 * Our current location is already analyzed by this rover or claimed by other rovers
	 * (In this function we assume that others only broadcast claimed cells, not when they are just analyzed)
	 */ 
	protected boolean currentCellAlreadyHandled() {
		for (Cell c : localWorldMap.getCellList()) {
			if (c.getX() == location.getX() && c.getY() == location.getY() && c.getMineral() != null)
				return true;
		}
		return false;
	}

	protected boolean analyzing() {
		return state == State.ANALYZING;
	}
	
	protected boolean moving() {
		return state == State.MOVING;
	}
	
	protected boolean currentCellAnalyzed() {
		for (Cell c : analyzedCells) {
			if (c.getX() == location.getX() && c.getY() == location.getY())
				return true;
		}
		for (Cell c : claimedCells) {
			if (c.getX() == location.getX() && c.getY() == location.getY())
				return true;
		}
		return false;
	}
	
	//calculate only border cells with distance 1
	private ArrayList<Cell> calculateSurroundingCells(Cell position) { 
		ArrayList<Cell> border = new ArrayList<Cell>();
		for (int i = 0; i < directions.size(); i++) {
			Cell next = localWorldMap.calculateNextPosition(position.getX(), position.getY(), directions.get(i));
			border.add(next);
		}
		return border;
	}

	private ArrayList<Cell> calculateCanonBallCells(Cell position) { //TODO optimize for use in moment we don't know what to do? + path algo to move back to capsule to claim/before mission ends
		ArrayList<Cell> border = new ArrayList<Cell>();
		for (int i = 0; i < directions.size(); i++) {
			Cell next = localWorldMap.calculateNextPosition(position.getX(), position.getY(), directions.get(i));
			border.add(next);
		}
		return border;
	}
	
	private ArrayList<Cell> calculateBorderCells(Cell position, int distance) {
		ArrayList<Cell> borderCells = new ArrayList<Cell>();
		Cell nextPos = localWorldMap.calculateNextPosition(position.getX(), position.getY(), "up");
		borderCells.add(position); // for followsSpiral-call to work. This entry is deleted at the end of the function
		borderCells.add(nextPos);
		int nbCells;
		boolean nextCellFound = false;
		
		while(borderCells.size() < (distance * 6 + 1)) {
			ArrayList<Cell> border = new ArrayList<Cell>();
			for (int i = 0; i < directions.size(); i++) {
				Cell next = localWorldMap.calculateNextPosition(nextPos.getX(), nextPos.getY(), directions.get(i));
				border.add(next);
			}
			Cell c;
			ArrayList<Cell> atRightDistance = new ArrayList<Cell>();
			for (int j = 0; j < border.size(); j++) {
				c = border.get(j);
				if (localWorldMap.distance(capsuleLocation, c) == distance)
					atRightDistance.add(c);
			}

//			System.out.println("current pos: " + nextPos.getX() + ", " + nextPos.getY());
//			System.out.println("a right distance:");
//			for (Cell whut : atRightDistance) {
//				System.out.println(whut.getX() + ", " + whut.getY());
//			}
 
//			System.out.println("bordercells");
//			for (Cell b : borderCells) {
//				System.out.println(b.getX() + ", " + b.getY());
//			}
			System.out.println();
			nbCells = borderCells.size();
			if (distance <= 1) {
				for (Cell last : atRightDistance) {
					if (notContains(borderCells, last)) {
						nextPos = last;
						break;
					}
				}
			}
			else {
				for (Cell last : atRightDistance) {
					if (notContains(borderCells, last) && preferredDirection(borderCells.get(nbCells-2), borderCells.get(nbCells-1), last)) {
						System.out.println("chosen");
						nextPos = last;
						nextCellFound = true;
						break;
					}
				}
				
				if (!nextCellFound) {
					for (Cell last : atRightDistance) {
						if (notContains(borderCells, last) && otherDirection(borderCells.get(nbCells-2), borderCells.get(nbCells-1), last)) {
							System.out.println("chosen");
							nextPos = last;
							break;
						}
					}
				}
				nextCellFound = false;
			}
//			System.out.println("adding: " + nextPos.getX() + ", " + nextPos.getY());
			borderCells.add(nextPos);
			
			if (borderCells.get(borderCells.size()-2).getX() == borderCells.get(borderCells.size()-1).getX() 
					&& borderCells.get(borderCells.size()-2).getY() == borderCells.get(borderCells.size()-1).getY()) {
				firstBehaviourUseless = true;
				System.out.println("First behaviour finished");
				borderCells.remove(borderCells.size()-1);
				break;
			}
		}
//		System.out.println("Final border cells: ");
//		for (Cell b : borderCells) {
//			System.out.println(b.getX() + " " + b.getY());
//		}
		borderCells.remove(0);
		return borderCells;
	}
	
	// Rover turns, to keep doing the spiral
	private boolean preferredDirection(Cell cell, Cell cell2, Cell last) {
		String dir = localWorldMap.whichDirection(cell, cell2);
		
		int ix = cell.getX();
    	int iy = cell.getY();
    	int dx = cell2.getX();
    	int dy = cell2.getY();
    	System.out.println(ix);
    	System.out.println(iy);
    	System.out.println(dx);
    	System.out.println(dy);
    	System.out.println(" ");
		System.out.println(dir);
		System.out.println();
		System.out.println(last.getX());
		System.out.println(last.getY());
		System.out.println();
		if (localWorldMap.whichDirection(cell2, last) == null) {
			System.out.println("ERROR calculating bordercells");
			return false;
		}
		
		if (dir.equals("up")) {
			return (localWorldMap.whichDirection(cell2, last).equals("rightUp"));
		}
		else if (dir.equals("rightUp")) {
			return (localWorldMap.whichDirection(cell2, last).equals("rightDown"));
		}
		else if (dir.equals("rightDown")) {
			return (localWorldMap.whichDirection(cell2, last).equals("down"));
		}
		else if (dir.equals("down")) {
			return (localWorldMap.whichDirection(cell2, last).equals("leftDown"));
		}
		else if (dir.equals("leftDown")) {
			return (localWorldMap.whichDirection(cell2, last).equals("leftUp"));
		}
		else if (dir.equals("leftUp")) {
			return (localWorldMap.whichDirection(cell2, last).equals("up"));
		}
		
		System.out.println("ERROR, non of the constructs are followed");
		return false;
	}
	
	// Rover keeps heading in the same direction
	private boolean otherDirection(Cell cell, Cell cell2, Cell last) {
		String dir = localWorldMap.whichDirection(cell, cell2);
		
//		int ix = cell.getX();
//    	int iy = cell.getY();
//    	int dx = cell2.getX();
//    	int dy = cell2.getY();
//    	System.out.println(ix);
//    	System.out.println(iy);
//    	System.out.println(dx);
//    	System.out.println(dy);
//    	System.out.println(" ");
//		System.out.println(dir);
//		System.out.println();
//		System.out.println(last.getX());
//		System.out.println(last.getY());
//		System.out.println();
		if (localWorldMap.whichDirection(cell2, last) == null) {
			System.out.println("ERROR calculating bordercells");
			return false;
		}
		
		if (dir.equals("up")) {
			return (localWorldMap.whichDirection(cell2, last).equals("up"));
		}
		else if (dir.equals("rightUp")) {
			return (localWorldMap.whichDirection(cell2, last).equals("rightUp"));
		}
		else if (dir.equals("rightDown")) {
			return (localWorldMap.whichDirection(cell2, last).equals("rightDown"));
		}
		else if (dir.equals("down")) {
			return (localWorldMap.whichDirection(cell2, last).equals("down"));
		}
		else if (dir.equals("leftDown")) {
			return (localWorldMap.whichDirection(cell2, last).equals("leftDown"));
		}
		else if (dir.equals("leftUp")) {
			return (localWorldMap.whichDirection(cell2, last).equals("leftUp"));
		}
		
		System.out.println("ERROR, non of the constructs are followed");
		return false;
	}

	private boolean notContains(ArrayList<Cell> borderCells, Cell last) {
		for (Cell c : borderCells) {
			if (c.getX() == last.getX() && c.getY() == last.getY())
				return false;
		}
		return true;
	}
	
	private ArrayList<Cell> goBackInRange(Cell position) {
		System.out.println("go back in range function called");
		ArrayList<Cell> movements = new ArrayList<Cell>();
		ArrayList<Cell> possible = calculateSurroundingCells(position);
		boolean found = false;
		int distance = localWorldMap.distance(position, capsuleLocation);
		System.out.println("distance to capsule is: " + distance);
		if (distance <= communicationRange) {
			System.out.println("Already in range"); //function should not be called in this case
		}
		int i = 0;
		while (!found) {
			Cell actual = possible.get(i);
			int newDistance = localWorldMap.distance(capsuleLocation, actual);
//			System.out.println("new distance to capsule is: " + newDistance);
//			System.out.println("trying: " + actual.getX() + ", " + actual.getY());
			if (newDistance < distance) {
				if (newDistance <= communicationRange) {
//					System.out.println("adding: " + actual.getX() + ", " + actual.getY());
					movements.add(actual);
					found = true;
				}
				else {
					//getting closer
//					System.out.println("adding: " + actual.getX() + ", " + actual.getY());
					possible.clear();
					possible = calculateSurroundingCells(actual);
					movements.add(actual);
					distance = newDistance;
					i = 0;
				}
			}
			else {
//				System.out.println("too far away");
				++i;
				/*if (i >= 5) {
					System.out.println("this shouldn't happen so recalculate surrounding cells");
					if (movements.size() > 0) {
						possible.clear();
						possible = calculateSurroundingCells(movements.get(movements.size()-1));
						System.out.println(possible.size());
						i = 0;
					}
					else {
						possible.clear();
						possible = calculateSurroundingCells(position);
						i = 0;
					}
				}*/
			}
		}
		
		return movements;
	}
	
	private ArrayList<Cell> shortestPathBetween(Cell position, Cell destination) {
		ArrayList<Cell> movements = new ArrayList<Cell>();
		ArrayList<Cell> possible = calculateSurroundingCells(position);
		boolean found = false;
		int distance = localWorldMap.distance(position, destination);
		
		int i = 0;
		while (!found) {
			Cell actual = possible.get(i);
			int newDistance = localWorldMap.distance(destination, actual);
//			System.out.println("new distance to destination is: " + newDistance);
//			System.out.println("trying: " + actual.getX() + ", " + actual.getY());
			if (newDistance < distance) {
				if (destination.getX() == actual.getX() 
						&& destination.getY() == actual.getY()) {
//					System.out.println("adding: " + actual.getX() + ", " + actual.getY());
					movements.add(actual);
					found = true;
				}
				else {
					//getting closer
//					System.out.println("adding: " + actual.getX() + ", " + actual.getY());
					possible.clear();
					possible = calculateSurroundingCells(actual);
					movements.add(actual);
					distance = newDistance;
					i = 0;
				}
			}
			else {
//				System.out.println("too far away");
				++i;
			}
		}
		
		return movements;
	}
	
	private void resetBehaviour() {
		nextMovements.clear();
		//TODO add cannonball behaviour
	}

	private WakerBehaviour killAgentAtMissionEnd() { //TODO use in every agent, especially in PlatformSimulator
		return new WakerBehaviour(this, missionLength*1000) {
			
			protected void onWake() {
				System.out.println(getLocalName() + ": committing suicide");
                myAgent.doDelete();
	        } 
		};
	}

	
	
	// ------------------------------------------------------------------------------------------------------------------

	private void analyzeCurrentCell(){ //TODO all behaviours parallel? We have roverstates, so should normally stay consistent? But what do we win with it?

		addBehaviour (new SimpleBehaviour(this) { //TODO can also be OneShotBehaviour?					  			
			private static final long serialVersionUID = 1L;

			AID agTerrain;

			private boolean cellAnalyzed = false;

			public void action(){

				//A defensive check
				if(!cellAnalyzed){
					//Creates description for the AGENT TERRAIN SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.TERRAINSIMULATOR);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agTerrain = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": terrain simulator agent is found");


							CellAnalysis cellAnalysis = new CellAnalysis();
							cellAnalysis.setCell(location);

							ACLMessage msg = MessageHandler.constructMessage(agTerrain, ACLMessage.REQUEST, cellAnalysis, XplorationOntology.CELLANALYSIS);
							send(msg);			                	

							System.out.println(getLocalName() + ": REQUEST is sent");
							//doWait(1000);

							//Returned answer from Terrain Simulation
							ACLMessage ans = MessageHandler.blockingReceive(myAgent, XplorationOntology.CELLANALYSIS);
							if(ans!= null){	  
								if(ans.getPerformative()==ACLMessage.REFUSE)
								{
									System.out.println(getLocalName() + ": REFUSED due to Invalid Cell: " + location.getX() + ", " + location.getY());
									cellAnalyzed = true;
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD)
								{
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									cellAnalyzed = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE)
								{
									System.out.println(getLocalName() + ": Initial AGREE was received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, XplorationOntology.CELLANALYSIS);

									switch (finalMsg.getPerformative()) {
									case ACLMessage.INFORM:
										System.out.println(getLocalName()+": analyze INFORM is received!");

										ContentElement ce;
										try {
											ce = getContentManager().extractContent(finalMsg);

											// We expect an action inside the message
											if (ce instanceof Action) {
												Action agAction = (Action) ce;
												Concept conc = agAction.getAction();

												if (conc instanceof CellAnalysis) {
													Cell cell = ((CellAnalysis) conc).getCell();
													analyzedCells.add(cell);
													localWorldMap.setCell(cell);
													System.out.println(myAgent.getLocalName()+ ": investigated Cell ("
															+cell.getX() + ","+ cell.getY()+  ", " + cell.getMineral() + ")");
												}
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
										cellAnalyzed = true;	

										break;
									case ACLMessage.FAILURE:
										System.out.println(getLocalName()+": FAILURE was received!");
										cellAnalyzed = true;
									}							
								}						  						  						  
							}else{
								//If no message arrives
								block();
							}
							state = State.OTHER;

						}else{
							System.out.println(getLocalName() + ": No terrain simulator found in yellow pages yet.");
							doWait(5000);
						}

					}catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}				
			}

			//To stop behaviour
			public boolean done() {
				//Cell is claimed
				return cellAnalyzed;
			}
		});

	}

	private void roverRegistration(Cell myCell){	
		addBehaviour (new SimpleBehaviour(this)
		{	
			private static final long serialVersionUID1 = 2L;

			AID agMapSimulator;

			private boolean roverRegistration = false;

			public void action(){
				//A defensive check
				if(!roverRegistration){
					//Creates description for the AGENT MAP SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();

					sd.setType(XplorationOntology.ROVERREGISTRATIONSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agMapSimulator = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": Map Simulator agent is found");


							RoverRegistrationInfo roverReg = new RoverRegistrationInfo();
							roverReg.setCell(myCell);
							Team team = new Team();
							team.setTeamId(TEAM_ID);
							roverReg.setTeam(team);

							ACLMessage msg = MessageHandler.constructMessage(agMapSimulator, ACLMessage.INFORM, roverReg, XplorationOntology.ROVERREGISTRATIONINFO);
							send(msg);	
							System.out.println(getLocalName() + ": INFORM is sent");
							roverRegistration = true;

							System.out.println(getLocalName() + ": Main behaviour started");
							startMainBehaviour();
							listenForMaps();
						}
						else{
							System.out.println(getLocalName() + ": No map simulator found in yellow pages yet.");
							doWait(5000);
						}
					}
					catch(Exception e){
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}

			}
			//To stop behaviour
			public boolean done() {
				return roverRegistration;
			}
		});
	}

	// This behaviour broadcasts a map to every rover in range. The behaviour will be started after every movement/analyzing of cell/5 seconds (to be decided).  
	private void broadcastCurrentMap(){

		addBehaviour (new OneShotBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			AID agCommunication;

			public void action(){

				//Creates description for the AGENT TERRAIN SIMULATOR to be searched
				DFAgentDescription dfd = new DFAgentDescription();     
				ServiceDescription sd = new ServiceDescription();
				sd.setType(XplorationOntology.MAPBROADCASTSERVICE);
				dfd.addServices(sd);

				try {
					// It finds agents of the required type
					DFAgentDescription[] result = new DFAgentDescription[20];
					result = DFService.search(myAgent, dfd);

					// Gets the first occurrence, if there was success
					if (result.length > 0)
					{
						//System.out.println(result[0].getName());
						agCommunication = (AID) result[0].getName();	

						//TODO change from here on 

						System.out.println(getLocalName()+ ": map broadcast service is found");

						if (!claimedCells.isEmpty()) {
							MapBroadcastInfo mbi = new MapBroadcastInfo();
							org.xploration.ontology.Map map = new org.xploration.ontology.Map();

							for (Cell c : claimedCells) {
								map.addCellList(c);
							}
							mbi.setMap(map);

							ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, mbi, XplorationOntology.MAPBROADCASTINFO);
							send(msg);			                	

							System.out.println(getLocalName() + ": map broadcast INFORM is sent");
							//doWait(1000);
						}
						else {
							System.out.println(getLocalName() + ": No analyzed cells to broadcast yet.");
						}
					}else{
						System.out.println(getLocalName() + ": No map broadcast service found yet.");
						doWait(5000);
					}

				}catch(Exception e){
					System.out.println(getLocalName() + "Exception is detected!");
					e.printStackTrace();
				}				
			}
		});

	}

	private void listenForMaps(){

		addBehaviour (tbf.wrap(new CyclicBehaviour(this)
		{						  			
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {				
				ACLMessage msg = MessageHandler.receive(myAgent, ACLMessage.INFORM, XplorationOntology.MAPBROADCASTINFO);

				if (msg != null) {
					System.out.println(getLocalName() + ": received map broadcast");

					// The ContentManager transforms the message content
					ContentElement ce;
					try {
						ce = getContentManager().extractContent(msg);

						if (ce instanceof Action) {
							Concept conc = ((Action) ce).getAction();

							if (conc instanceof MapBroadcastInfo) {
								MapBroadcastInfo mbi = (MapBroadcastInfo) conc;
								org.xploration.ontology.Map map = mbi.getMap();
								Iterator it = map.getAllCellList();
								Cell c;
								while (it.hasNext()) {
									c = (Cell) it.next();
									localWorldMap.setCell(c);
								}
								System.out.println(getLocalName() + ": new local world map");
//								localWorldMap.printWorldMap();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
					// Behaviour is blocked. Will be woken up again whenever the agent receives an ACLMessage.
					block();
				}
			}			
		}));

	}
	//TODO you have to give something back here? because if movementrequest fails, it is still removed from list and on next move there is a problem? 
	private void requestMovement(Cell destination) { //TODO here we probably add an argument 'cell' and take the decision of where to go outside?!
		addBehaviour(new SimpleBehaviour(this) {
			private static final long serialVersionUID = 1L;
			AID agMovementSim;
			private boolean movementRequested = false;
			@Override
			public void action() {
				if (!movementRequested) {
					//Creates description for the AGENT TERRAIN SIMULATOR to be searched
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.MOVEMENTREQUESTSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0) {
							agMovementSim = (AID) result[0].getName();	
							System.out.println(getLocalName()+ ": movement simulator agent is found");

							MovementRequestInfo mri = new MovementRequestInfo();
							Team team = new Team();
							team.setTeamId(TEAM_ID);
							mri.setCell(destination);
							mri.setTeam(team);

							ACLMessage msg = MessageHandler.constructMessage(agMovementSim, ACLMessage.REQUEST, mri, XplorationOntology.MOVEMENTREQUESTINFO);
							send(msg);
							
							System.out.println(getLocalName() + ": REQUEST is sent");

							ACLMessage ans = MessageHandler.blockingReceive(myAgent, XplorationOntology.MOVEMENTREQUESTINFO);
							if (ans != null) {
								if (ans.getPerformative() == ACLMessage.REFUSE) {
									System.out.println(getLocalName() + ": REFUSED due to Invalid Cell");
									movementRequested = true;
									resetBehaviour();
								}

								else if(ans.getPerformative()== ACLMessage.NOT_UNDERSTOOD) {
									System.out.println(getLocalName() + ": NOT UNDERSTOOD the message");
									movementRequested = true;
								}
								else if(ans.getPerformative()== ACLMessage.AGREE) {
									System.out.println(getLocalName() + ": Initial AGREE was received");	  

									ACLMessage finalMsg = MessageHandler.blockingReceive(myAgent, XplorationOntology.MOVEMENTREQUESTINFO);
									if (finalMsg.getPerformative() == ACLMessage.INFORM) {
										System.out.println(getLocalName() + ": INFORM was received, movement accepted");
										location = destination;
										if (nextMovements.get(0) != destination)
											System.out.println(getLocalName() + ": ERROR! next destination does not match provisioned one.");
										nextMovements.remove(0);
										movementRequested = true;
										
										// Try to claim here, because there is no use in trying more often than the times you move 
										if (!analyzedCells.isEmpty() && !alreadyClaiming && localWorldMap.inRangeFrom(location, capsuleLocation, communicationRange)) {
											alreadyClaiming = true;
//											System.out.println("should not be empty: " + analyzedCells);
											System.out.println(getLocalName() + ": claimin'");
											claimCells();
										}
										
										broadcastCurrentMap();
										
//										if (!currentCellAlreadyHandled())
//											analyzeCurrentCell();
									}
									else if (finalMsg.getPerformative() == ACLMessage.FAILURE) {
										System.out.println(getLocalName() + ": FAILURE was received, collision");
										movementRequested = true;
									}
								}
							}
						}
						state = State.OTHER;
					}
					catch (Exception e) {
						System.out.println(getLocalName() + "Exception is detected!");
						e.printStackTrace();
					}
				}
			}

			@Override
			public boolean done() {
				return movementRequested;
			}

		});
	}
	//Cell Claim Protocol from Rover to Platform Simulator
	private void claimCells(){
		addBehaviour (new SimpleBehaviour (this){
			
			//Receiver Agent ID
			AID agCommunication;
			private boolean claimCell = false;

			public void action(){
				if(!claimCell){
					//Searching for an agent with RADIOCLAIMSERVICE Description
					DFAgentDescription dfd = new DFAgentDescription();     
					ServiceDescription sd = new ServiceDescription();
					sd.setType(XplorationOntology.RADIOCLAIMSERVICE);
					dfd.addServices(sd);

					try {
						// It finds agents of the required type
						DFAgentDescription[] result = new DFAgentDescription[20];
						result = DFService.search(myAgent, dfd);

						// Gets the first occurrence, if there was success
						if (result.length > 0)
						{
							//System.out.println(result[0].getName());
							agCommunication = (AID) result[0].getName();											
							System.out.println(getLocalName()+ ": Radio Claim Service is found");

							ClaimCellInfo cci = new ClaimCellInfo();
							
							Team myTeam = new Team();
							myTeam.setTeamId(TEAM_ID);
							cci.setTeam(myTeam);
							org.xploration.ontology.Map cciMap = new org.xploration.ontology.Map();
							System.out.println(analyzedCells);
							for (Cell c : analyzedCells) {
								cciMap.addCellList(c);
								claimedCells.add(c);
							}
							analyzedCells.clear();
							movingInRangeToClaim = false;
							
							cci.setMap(cciMap);
																				
							try{
								ACLMessage msg = MessageHandler.constructMessage(agCommunication, ACLMessage.INFORM, cci, XplorationOntology.CLAIMCELLINFO);
								send(msg);	
								System.out.println(getLocalName() + ": INFORM is sent");
								claimCell = true;
							}
							catch(Exception e){
								e.printStackTrace();
								System.out.println(getLocalName() + ": INFORM couldn't sent");
							}
						}
						else{
							System.out.println(getLocalName()+ ": No agent found yet");
							doWait(5000);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
	
			public boolean done() {
				return claimCell;
			}			
			
			@Override
			public int onEnd() {
				alreadyClaiming = false;
				return super.onEnd();
			}
		});
	}
}



