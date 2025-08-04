package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Optional;

import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModAVUMetadataInp_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcModAVUMetadataTest {

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
	void testAddingAndRemovingMetadata() throws IOException {
		var collection = '/' + String.join("/", zone, "home", username);
		var avuName = "irods4j::name";
		var avuValue = "irods4j::value";
		var avuUnit = "irods4j::unit";

		// Add metadata to the user's home collection.
		var input = new ModAVUMetadataInp_PI();
		input.arg0 = "set";
		input.arg1 = "-C";
		input.arg2 = collection;
		input.arg3 = avuName;
		input.arg4 = avuValue;
		input.arg5 = avuUnit;
		input.KeyValPair_PI = new KeyValPair_PI();

		var ec = IRODSApi.rcModAVUMetadata(comm, input);
		assertEquals(ec, 0);

		// Remove the recently added metadata from the user's home collection.
		input.arg0 = "rm";
		ec = IRODSApi.rcModAVUMetadata(comm, input);
		assertEquals(ec, 0);
	}

}
