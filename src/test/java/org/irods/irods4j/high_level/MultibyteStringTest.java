package org.irods.irods4j.high_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectInputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class MultibyteStringTest {

	static final Logger log = LogManager.getLogger();

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
	void testWritingToADataObjectWithMultibyteCharactersInItsName() throws Exception {
		String dataObject = '/' + String.join("/", zone, "home", username, "project-데이터_пример_संख्या_وثيقة_تجربة_東京_サンプル_текст_Überraschung_Énergie_456.iso");
		byte[] data = "created a data object that has a name containing multibyte characters.".getBytes(StandardCharsets.UTF_8);

		try {
			try (IRODSDataObjectOutputStream out = new IRODSDataObjectOutputStream(conn.getRcComm(), dataObject, true, false)) {
				out.write(data);
			}
			assertTrue(IRODSFilesystem.exists(conn.getRcComm(), dataObject));

			byte[] buffer = new byte[data.length];
			try (IRODSDataObjectInputStream in = new IRODSDataObjectInputStream(conn.getRcComm(), dataObject)) {
				assertEquals(buffer.length, in.read(buffer, 0, buffer.length));
			}
			assertArrayEquals(data, buffer);
		} finally {
			IRODSFilesystem.remove(conn.getRcComm(), dataObject, IRODSFilesystem.RemoveOptions.NO_TRASH);
		}
	}

}
