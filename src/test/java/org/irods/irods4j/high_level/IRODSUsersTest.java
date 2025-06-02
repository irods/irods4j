package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSUsers.Group;
import org.irods.irods4j.high_level.administration.IRODSUsers.User;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.administration.IRODSZones.ZoneType;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSUsersTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static RcComm comm;

	static User rodsuser = new User("TestIRODSUsers_alice", Optional.of(zone));

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm);
		IRODSApi.rcAuthenticateClient(comm, "native", password);

		// Create a user to test password manipulation.
		IRODSUsers.addUser(comm, rodsuser, UserType.RODSUSER, ZoneType.LOCAL);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSUsers.removeUser(comm, rodsuser);
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testModifyPasswordOfDifferentUser() throws Exception {
		// Generate a random password for the rodsuser.
		var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		var rnd = new Random();
		var pwSb = new StringBuilder();
		var psLength = rnd.nextInt(4, 40);
		for (int i = 0; i < psLength; ++i) {
			pwSb.append(chars.charAt(rnd.nextInt(chars.length())));
		}
		var rodsuserPassword = pwSb.toString();

		// Change the rodsuser's password.
		var prop = new IRODSUsers.UserPasswordProperty();
		prop.value = rodsuserPassword;
		prop.requesterPassword = password;
		assertDoesNotThrow(() -> IRODSUsers.modifyUser(comm, rodsuser, prop));

		// Show that the rodsuser can authenticate using the updated password.
		var comm1 = IRODSApi.rcConnect(host, port, rodsuser.name, rodsuser.zone, Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty());
		assertNotNull(comm1);
		IRODSApi.rcAuthenticateClient(comm1, "native", prop.value);
		IRODSApi.rcDisconnect(comm1);
	}

	@Test
	void testRetrieveAllUsersInASpecificGroup() throws IOException, IRODSException {
		var users = IRODSUsers.users(comm, new Group("public"));
		users.forEach(u -> log.debug("user = {}#{}", u.name, u.zone));
		assertNotNull(users);
		assertFalse(users.isEmpty());
		assertTrue(users.stream().anyMatch(u -> username.equals(u.name) && zone.equals(u.zone)));
	}

	@Test
	void testRetrieveAllGroupsContainingASpecificUser() throws IOException, IRODSException {
		var groups = Arrays.asList(new Group("irods4j_testgroup0"), new Group("irods4j_testgroup1"),
				new Group("irods4j_testgroup2"));

		try {
			assertDoesNotThrow(() -> {
				for (var g : groups) {
					IRODSUsers.addGroup(comm, g);
					IRODSUsers.addUserToGroup(comm, g, rodsuser);
				}
			});

			var groupsContainingMember = IRODSUsers.groups(comm, rodsuser);
			groupsContainingMember.forEach(g -> log.debug("group = {}", g.name));

			assertNotNull(groupsContainingMember);
			assertFalse(groupsContainingMember.isEmpty());
			assertTrue(groupsContainingMember.size() > 1);
			assertTrue(groupsContainingMember.stream().allMatch(g -> "public".equals(g.name) || groups.contains(g)));
		} finally {
			assertDoesNotThrow(() -> {
				for (var g : groups) {
					IRODSUsers.removeGroup(comm, g);
				}
			});
		}
	}

}
