package org.xploration.tests;

import org.junit.Before;
import org.junit.Test;

/*
 * Possible test cases: 
 * 
 * Start up a certain Registration Desk with registration period X.
 * There should/could be some getRegisteredCompanies method to help the testing
 * 
 * Case 1: Send a request and get an AGREE and an INFORM
 * 
 * Case 2: Send another request from the same team-id (within registration period) and get AGREE and FAILURE
 * 
 * Case 3: Send rubbish and get NOT_UNDERSTOOD
 * 
 * Case 4: Send another request outside of registration period and get REFUSE
 * 
 * TODO Question: do tests manually or only with blackbox agents and registration desk? 
 */
public class RegistrationDeskTest {
	@Before
	public void setUp() {
		//TODO construct a registration desk (and agent) 
	}

	@Test
	public void test_RegistrationSucceedsAndIsRegisteredAtDesk() {
		//TODO
	}
	
	@Test
	public void test_RegistrationFails_WhenRequestedMultipleTimes() {
		//TODO
	}
	
	@Test
	public void test_RegistrationRefused_WhenTooLate() {
		//TODO
	}
	

	@Test
	public void test_NotUnderstood_WhenNotAccordingToProtocol() {
		//TODO
	}// If not caught, could be done with (expected = OntologyException.class)
}
