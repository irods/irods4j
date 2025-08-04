package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollInpNew_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollOprStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CollectionOperationsTest {

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
	void testCreatingAndDeletingCollections() throws IOException {
		// Create a new collection.
		CollInpNew_PI input = new CollInpNew_PI();
		input.collName = '/' + String.join("/", zone, "home", username, "irods4j_test_coll");
		input.KeyValPair_PI = new KeyValPair_PI(); // Not necessary as shown through testing.
		input.KeyValPair_PI.ssLen = 0; // Not necessary as shown through testing.

		int ec = IRODSApi.rcCollCreate(comm, input);
		assertEquals(ec, 0);

		// Delete the collection.
		input.KeyValPair_PI.ssLen = 2;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// FORCE_FLAG_KW
		input.KeyValPair_PI.keyWord.add("forceFlag");
		input.KeyValPair_PI.svalue.add("");
		// RECURSIVE_OPR__KW (yes, two underscores - yuck)
		input.KeyValPair_PI.keyWord.add("recursiveOpr");
		input.KeyValPair_PI.svalue.add("");

		Reference<CollOprStat_PI> output = new Reference<CollOprStat_PI>();
		ec = IRODSApi.rcRmColl(comm, input, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
	}

}
