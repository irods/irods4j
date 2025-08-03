package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSUsers.User;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserPasswordProperty;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.administration.IRODSZones.ZoneType;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.high_level.vfs.IRODSReplicas;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSReplicasTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static IRODSConnection conn;

	static User rodsuser = new User("TestIRODSReplicas_alice", Optional.of(zone));

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
	void testCalculateChecksumForReplicaAsAdminWithoutPermissionsOnDataObject() throws Exception {
		var logicalPath = '/' + String.join("/", zone, "home", rodsuser.name, "testUpdateModificationTimeOfReplicaAsAdmin.txt");
		var rodsuserPassword = "thepassword";

		try {
			// Create a user.
			IRODSUsers.addUser(conn.getRcComm(), rodsuser, UserType.RODSUSER, ZoneType.LOCAL);

			// Set the user's password so we can authenticate as them.
			var prop = new UserPasswordProperty();
			prop.value = rodsuserPassword;
			prop.requesterPassword = password;
			IRODSUsers.modifyUser(conn.getRcComm(), rodsuser, prop);

			// Create a new data object as the rodsuser.
			try (var rodsuserConn = new IRODSConnection()) {
				rodsuserConn.connect(host, port, new QualifiedUsername(rodsuser.name, rodsuser.zone));
				rodsuserConn.authenticate(new NativeAuthPlugin(), prop.value);

				try (var stream = new IRODSDataObjectStream()) {
					stream.open(rodsuserConn.getRcComm(), logicalPath,
							OpenFlags.O_CREAT | OpenFlags.O_WRONLY | OpenFlags.O_TRUNC);

					var buffer = "Testing IRODSReplicas.lastWriteTime() implementation using irods4j."
							.getBytes(StandardCharsets.UTF_8);
					stream.write(buffer, buffer.length);
				}
			}

			// Show the admin cannot calculate a checksum if the admin flag isn't passed.
			var ex = assertThrows(IRODSException.class, () -> IRODSReplicas.replicaChecksum(conn.getRcComm(),
					logicalPath, 0, IRODSReplicas.VerificationCalculation.ALWAYS));
			assertEquals(ex.getErrorCode(), IRODSErrorCodes.CAT_NO_ACCESS_PERMISSION);

			// Show the admin can calculate a checksum if the admin flag is passed.
			var checksum = IRODSReplicas.replicaChecksum(IRODSReplicas.asAdmin, conn.getRcComm(), logicalPath, 0,
					IRODSReplicas.VerificationCalculation.ALWAYS);
			log.debug("checksum = {}", checksum);
			assertFalse(checksum.isEmpty());
		} finally {
			try (var rodsuserConn = new IRODSConnection()) {
				rodsuserConn.connect(host, port, new QualifiedUsername(rodsuser.name, rodsuser.zone));
				rodsuserConn.authenticate(new NativeAuthPlugin(), rodsuserPassword);
				IRODSFilesystem.remove(rodsuserConn.getRcComm(), logicalPath, RemoveOptions.NO_TRASH);
			} catch (Exception e) {
				log.error(e.getMessage());
			}

			IRODSUsers.removeUser(conn.getRcComm(), rodsuser);
		}
	}

}
