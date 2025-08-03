package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcGenQuery2Test {

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
		IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testRcGenQuery2() throws IOException, IRODSException {
		var input = new Genquery2Input_PI();
		input.query_string = "select COLL_NAME";
//		input.zone = zone;
		input.sql_only = 0;
		input.column_mappings = 0;

		var output = new Reference<String>();

		assertEquals(IRODSApi.rcGenQuery2(comm, input, output), 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertTrue(output.value.contains("[\"/tempZone/home/rods\"]"));
	}

	// TODO Update this test's behavior to match its name.
	@Test
	void testRcGenQuey2HandlesEmbeddedSingleQuotes() throws IOException, IRODSException {
		var input = new Genquery2Input_PI();
		input.query_string = "select COLL_NAME";
		input.zone = zone;
		input.sql_only = 0;
		input.column_mappings = 0;

		var output = new Reference<String>();

		assertEquals(IRODSApi.rcGenQuery2(comm, input, output), 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertTrue(output.value.contains("[\"/tempZone/home/rods\"]"));
	}

}
