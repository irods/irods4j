package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcAtomicApplyAclOperationsTest {

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
	void testRcAtomicApplyAclOperations() throws IOException {
		var logicalPath = Paths.get("/", zone, "home", username).toString();

		// Set the rodsadmin's permission to "write" on their home collection.
		// Remember, they can always restore their permissions because they are
		// a rodsadmin.
		var op = new HashMap<String, Object>();
		op.put("entity_name", username);
//		op.put("acl", "modify_object"); // TODO Bump to 4.3.3 since this isn't supported by 4.3.2.
		op.put("acl", "read");

		var ops = new ArrayList<Object>();
		ops.add(op);

		var inputStruct = new HashMap<String, Object>();
		inputStruct.put("logical_path", logicalPath);
		inputStruct.put("operations", ops);

		var operations = JsonUtil.toJsonString(inputStruct);
		System.out.println(operations);
		var output = new Reference<String>();
		var ec = IRODSApi.rcAtomicApplyAclOperations(comm, operations, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);

		// Restore the rodsadmin's permissions.
		op.put("acl", "own");
		ops.clear();
		ops.add(op);
		inputStruct.put("admin_mode", true);

		operations = JsonUtil.toJsonString(inputStruct);
		ec = IRODSApi.rcAtomicApplyAclOperations(comm, operations, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
	}

}
