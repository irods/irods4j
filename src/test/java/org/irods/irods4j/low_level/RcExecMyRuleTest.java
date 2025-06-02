package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.ExecCmdOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ExecMyRuleInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParamArray_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParam_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RHostAddr_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.STR_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcExecMyRuleTest {
	
	private static final Logger log = LogManager.getLogger();

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
	void testRcExecMyRule() throws IOException {
		XmlUtil.enablePrettyPrinting();
		
		var text = "Hello, irods4j! 1 + 1 =";
		var input = new ExecMyRuleInp_PI();
		// The rule text to execute.
		input.myRule = String.format("@external rule { writeLine('stdout', '%s 2'); }", text);
		input.RHostAddr_PI = new RHostAddr_PI();
		// The output variable to store the output in. This is specific to stdout.
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 1;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Target a specific rule engine plugin (REP).
		input.KeyValPair_PI.keyWord.add("instance_name");
		input.KeyValPair_PI.svalue.add("irods_rule_engine_plugin-irods_rule_language-instance");
		input.outParamDesc = "ruleExecOut";
		// Input variables.
		input.MsParamArray_PI = new MsParamArray_PI();
		input.MsParamArray_PI.paramLen = 1;
		input.MsParamArray_PI.MsParam_PI = new ArrayList<>();
		var mp = new MsParam_PI();
		mp.label = "*x";
		mp.type = "STR_PI";
		mp.inOutStruct = new STR_PI();
		((STR_PI) mp.inOutStruct).myStr = "2";
		input.MsParamArray_PI.MsParam_PI.add(mp);
		
		var output = new Reference<MsParamArray_PI>();
		
		var ec = IRODSApi.rcExecMyRule(comm, input, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
		
		var ruleExecOut = (ExecCmdOut_PI) output.value.MsParam_PI.get(0).inOutStruct;
		assertEquals(ruleExecOut.status, 0);
		log.debug("stdout buffer = {}", ruleExecOut.BinBytesBuf_PI.get(0).buf);
		log.debug("stdout length (whitespace trimmed) = {}", ruleExecOut.BinBytesBuf_PI.get(0).buf.trim().length());
		assertTrue((text + " 2").equals(ruleExecOut.BinBytesBuf_PI.get(0).buf.trim()));
	}

}
