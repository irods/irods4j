package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;

import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.GeneralAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SwitchUserInp_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcSwitchUserTest {

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static RcComm comm;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm);
		IRODSApi.rcAuthenticateClient(comm, "native", password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testRcSwitchUser() throws Exception {
		var otherUser = "otherRods";

		// Add a new rodsuser to the zone.
		var addUserInput = new GeneralAdminInp_PI();
		addUserInput.arg0 = "add";
		addUserInput.arg1 = "user";
		addUserInput.arg2 = otherUser;
		addUserInput.arg3 = "rodsuser";
		addUserInput.arg4 = zone;

		var ec = IRODSApi.rcGeneralAdmin(comm, addUserInput);
		assertEquals(ec, 0);

		// Become the new rodsuser.
		var switchUserInput = new SwitchUserInp_PI();
		switchUserInput.username = otherUser;
		switchUserInput.zone = zone;
		switchUserInput.KeyValPair_PI = new KeyValPair_PI();
		switchUserInput.KeyValPair_PI.ssLen = 1;
		switchUserInput.KeyValPair_PI.keyWord = new ArrayList<>();
		switchUserInput.KeyValPair_PI.svalue = new ArrayList<>();
		switchUserInput.KeyValPair_PI.keyWord.add(SwitchUserInp_PI.KW_SWITCH_PROXY_USER);
		switchUserInput.KeyValPair_PI.svalue.add("");

		ec = IRODSApi.rcSwitchUser(comm, switchUserInput);
		assertEquals(ec, 0);
		// The client user information in the RcComm should reflect the new rodsuser.
		assertTrue(otherUser.equals(comm.clientUsername));
		assertTrue(zone.equals(comm.clientUserZone));
		// The proxy user information in the RcComm should reflect the new rodsuser.
		assertTrue(otherUser.equals(comm.proxyUsername));
		assertTrue(zone.equals(comm.proxyUserZone));

		// Connect as the local rodsadmin and remove the recently created user.
		// This is required because we became a rodsuser, which means we lost
		// rodsadmin level privileges.
		var adminComm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty());
		assertNotNull(adminComm);
		IRODSApi.rcAuthenticateClient(adminComm, "native", password);

		// Remove the rodsuser.
		var removeUserInput = new GeneralAdminInp_PI();
		removeUserInput.arg0 = "rm";
		removeUserInput.arg1 = "user";
		removeUserInput.arg2 = otherUser;
		removeUserInput.arg3 = zone;

		ec = IRODSApi.rcGeneralAdmin(adminComm, removeUserInput);
		assertEquals(ec, 0);

		IRODSApi.rcDisconnect(adminComm);
	}

}
