package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TicketAdminInp_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcTicketAdminTest {

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
	void testCreateAndDeleteTicket() throws IOException {
		var ticketName = "irods4j_ticket";
		var collection = Paths.get("/", zone, "home", username).toString();

		// Create a new ticket on the user's home collection.
		var input = new TicketAdminInp_PI();
		input.arg1 = "create";
		input.arg2 = ticketName;
		input.arg3 = "read";
		input.arg4 = collection;
		input.arg5 = ""; // TODO Why is this required to match arg2 in the C++ lib?
		input.arg6 = "";
		input.KeyValPair_PI = new KeyValPair_PI(); // Optional.
		input.KeyValPair_PI.ssLen = 0;

		var ec = IRODSApi.rcTicketAdmin(comm, input);
		assertEquals(ec, 0);

		// Delete the ticket.
		input.arg1 = "delete";
		input.arg3 = "";
		input.arg4 = "";

		ec = IRODSApi.rcTicketAdmin(comm, input);
		assertEquals(ec, 0);
	}

}
