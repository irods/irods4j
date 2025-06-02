package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.GeneralAdminInp_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcGeneralAdminTest {

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static RcComm comm;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

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
	void testAddAndRemoveResource() throws IOException {
		String rescName = "irods4j_pt_0";

		// Add resource.
		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "add";
		input.arg1 = "resource";
		input.arg2 = rescName;
		input.arg3 = "passthru";
		input.arg4 = "";
		input.arg5 = "";
		input.arg6 = "";
		assertEquals(IRODSApi.rcGeneralAdmin(comm, input), 0);

		// Remove resource.
		input.arg0 = "rm";
		input.arg1 = "resource";
		input.arg2 = rescName;
		input.arg3 = ""; // Dryrun flag (cannot be null)
		assertEquals(IRODSApi.rcGeneralAdmin(comm, input), 0);
	}

	@Test
	void testAddRemoveChildResource() throws Exception {
		String rescName1 = "irods4j_pt_1";
		String rescName2 = "irods4j_pt_2";

		// Add resources.
		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		for (String rescName : new String[] { rescName1, rescName2 }) {
			input.arg0 = "add";
			input.arg1 = "resource";
			input.arg2 = rescName;
			input.arg3 = "passthru";
			input.arg4 = "";
			input.arg5 = "";
			input.arg6 = "";
			assertEquals(IRODSApi.rcGeneralAdmin(comm, input), 0);
		}

		// Create a new connection so the resources can be seen/used.
		RcComm comm1 = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm1);
		IRODSApi.rcAuthenticateClient(comm1, "native", password);

		// Add child resource to parent resouce.
		input.arg0 = "add";
		input.arg1 = "childtoresc";
		input.arg2 = rescName1;
		input.arg3 = rescName2;
		input.arg4 = ""; // Context string
		assertEquals(IRODSApi.rcGeneralAdmin(comm1, input), 0);

		// Refresh the connection so the resources can be seen/used.
		IRODSApi.rcDisconnect(comm1);
		comm1 = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm1);
		IRODSApi.rcAuthenticateClient(comm1, "native", password);

		// Remove child resource from parent resource.
		input.arg0 = "rm";
		input.arg1 = "childfromresc";
		input.arg2 = rescName1;
		input.arg3 = rescName2;
		assertEquals(IRODSApi.rcGeneralAdmin(comm1, input), 0);

		// Make sure to disconnect the intermediate connection.
		IRODSApi.rcDisconnect(comm1);

		// Remove resource.
		for (String rescName : new String[] { rescName1, rescName2 }) {
			input.arg0 = "rm";
			input.arg1 = "resource";
			input.arg2 = rescName;
			input.arg3 = "";
			assertEquals(IRODSApi.rcGeneralAdmin(comm, input), 0);
		}
	}

}
