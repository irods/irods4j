package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Optional;

import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.protocol.packing_instructions.RErrMsg_PI;
import org.junit.jupiter.api.Test;

class RcConnectTest {

	@Test
	void testConnectAndDisconnect() {
		final String host = "localhost";
		final int port = 1247;
		final String zone = "tempZone";
		final String username = "rods";

		IRODSApi.RcComm comm = assertDoesNotThrow(() -> IRODSApi.rcConnect(host, port, username, zone, Optional.empty(),
				Optional.empty(), Optional.empty(), Optional.empty()));
		assertNotNull(comm);
		assertDoesNotThrow(() -> IRODSApi.rcDisconnect(comm));
	}

	@Test
	void testRcConnectCapturesErrorInfoInRErrMsgObject() {
		assumeTrue(false, "Need to investigate how to trigger this case");

		// TODO Figure out how to trigger an error that gets captured by rcConnect.
		final String host = "localhost";
		final int port = 1247;
//		final var zone = "tempZone";
//		final var username = "bogus";
		final String zone = "x";
		final String username = "x";

		RErrMsg_PI errInfo = new RErrMsg_PI();
		IRODSApi.RcComm comm = assertDoesNotThrow(() -> IRODSApi.rcConnect(host, port, username, zone, Optional.empty(),
				Optional.empty(), Optional.empty(), Optional.of(errInfo)));
		assertNull(comm);
		assertTrue(errInfo.status < 0);
		assertDoesNotThrow(() -> IRODSApi.rcDisconnect(comm));
	}

}
