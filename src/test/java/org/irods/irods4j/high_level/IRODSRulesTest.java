package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers.User;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.policy.IRODSRules;
import org.irods.irods4j.high_level.policy.IRODSRules.RuleArguments;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSRulesTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static IRODSConnection conn;

	static User rodsuser = new User("TestIRODSUsers_alice", Optional.of(zone));

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		conn = new IRODSConnection();
		conn.connect(host, port, new QualifiedUsername(username, zone));
		conn.authenticate("native", password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		conn.disconnect();

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testIRODSRulesReturnsAvailableRuleEnginePluginInstances() throws Exception {
		List<String> repInstances = IRODSRules.getAvailableRuleEnginePluginInstances(conn.getRcComm());
		assertNotNull(repInstances);
		assertFalse(repInstances.isEmpty());
		log.debug("Rule engine plugin instances = {}", repInstances);
	}

	@Test
	void testExecuteRuleReturnsStdoutStderrWhenOutputArgumentIsRuleExecOut() throws Exception {
		RuleArguments ruleArgs = new RuleArguments();
		String msg = "hello, irods4j!";
		ruleArgs.ruleText = String.format("writeLine('stdout', '%s')", msg);
		ruleArgs.input = Optional.empty();
		ruleArgs.output = Optional.of(Arrays.asList("ruleExecOut"));
		ruleArgs.ruleEnginePluginInstance = Optional.of("irods_rule_engine_plugin-irods_rule_language-instance");

		Map<String, String> results = IRODSRules.executeRule(conn.getRcComm(), ruleArgs);
		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertEquals(results.get("stdout"), msg);
		assertEquals(results.get("stderr"), "");
	}

	@Test
	void testExecuteRuleReturnsVariablesRequestedByTheUser() throws Exception {
		RuleArguments ruleArgs = new RuleArguments();
		String name = "irods4j";
		String role = "Java client library for iRODS";
		ruleArgs.ruleText = String.format("*name = '%s'; *role = '%s'", name, role);
		ruleArgs.input = Optional.empty();
		ruleArgs.output = Optional.of(Arrays.asList("*name", "*role"));
		ruleArgs.ruleEnginePluginInstance = Optional.of("irods_rule_engine_plugin-irods_rule_language-instance");

		Map<String, String> results = IRODSRules.executeRule(conn.getRcComm(), ruleArgs);
		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertEquals(results.get("*name"), name);
		assertEquals(results.get("*role"), role);
	}

}
