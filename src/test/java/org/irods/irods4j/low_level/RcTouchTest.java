package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RodsObjStat_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcTouchTest {

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
	void testRcTouch() throws IOException {
		String logicalPath = '/' + String.join("/", zone, "home", username);
		int mtime = 1700000000;

		// Set the mtime of the rodsadmin's home collection to a specific value.
		HashMap<String, Object> options = new HashMap<String, Object>();
		options.put("seconds_since_epoch", mtime);

		HashMap<String, Object> touchInput = new HashMap<String, Object>();
		touchInput.put("logical_path", logicalPath);
		touchInput.put("options", options);

		String input = JsonUtil.toJsonString(touchInput);
		System.out.println(input);
		int ec = IRODSApi.rcTouch(comm, input);
		assertEquals(ec, 0);

		// Show the collection's mtime has been changed to the target mtime.
		DataObjInp_PI statInput = new DataObjInp_PI();
		statInput.objPath = logicalPath;
		statInput.KeyValPair_PI = new KeyValPair_PI();

		Reference<RodsObjStat_PI> output = new Reference<RodsObjStat_PI>();

		ec = IRODSApi.rcObjStat(comm, statInput, output);
		assertFalse(ec < 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertEquals(Integer.parseInt(output.value.modifyTime), mtime);
	}

}
