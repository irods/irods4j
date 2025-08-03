package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.policy.IRODSRules;
import org.irods.irods4j.high_level.policy.IRODSRules.RuleArguments;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RErrorStackTest {

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static IRODSConnection conn;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		conn = new IRODSConnection();
		conn.connect(host, port, new QualifiedUsername(username, zone));
		conn.authenticate(new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		conn.disconnect();

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testRcCommCapturesRErrorInfo() throws Exception {
		RuleArguments ruleArgs = new RuleArguments();
		ruleArgs.ruleText = "writeLine('stdout', 'missing syntax";
		ruleArgs.input = Optional.empty();
		ruleArgs.output = Optional.empty();
		ruleArgs.ruleEnginePluginInstance = Optional.of("irods_rule_engine_plugin-irods_rule_language-instance");

		assertThrows(IRODSException.class, () -> IRODSRules.executeRule(conn.getRcComm(), ruleArgs));
		assertEquals(conn.getRcComm().rError.count, 1);
		assertTrue(conn.getRcComm().rError.RErrMsg_PI.get(0).msg.contains("parseRuleSet: error parsing rule."));
	}

}
