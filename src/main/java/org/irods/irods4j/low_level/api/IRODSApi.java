package org.irods.irods4j.low_level.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.AuthManager;
import org.irods.irods4j.authentication.AuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.network.Network;
import org.irods.irods4j.low_level.protocol.packing_instructions.BinBytesBuf_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.BytesBuf_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CS_NEG_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollEnt_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollInpNew_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollOprStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjCopyInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInfo_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DelayRuleLockInput_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DelayRuleUnlockInput_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DelayServerMigrationInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ExecMyRuleInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.FileLseekOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GenQueryInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GenQueryOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GeneralAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GridConfigurationInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GridConfigurationOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.INT_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.IRODS_STR_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MiscSvrInfo_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModAVUMetadataInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModAccessControlInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModDataObjMeta_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParamArray_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsgHeader_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.OpenedDataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ProcStatInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RErrMsg_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RError_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RULE_EXEC_DEL_INP_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RULE_EXEC_MOD_INP_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RegReplica_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RodsObjStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SSLEndInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SSLStartInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.STR_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SpecificQueryInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.StartupPack_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SwitchUserInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TicketAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TransferStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.UnregDataObj_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.UserAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.Version_PI;

/**
 * A low-level class providing functions mirroring the iRODS C API.
 * 
 * All public API functions share identical names with the C API. Users should
 * prefer the abstractions provided by the high_level package in production
 * code. Use of the functions provided by this class should be a last resort.
 * 
 * @since 0.1.0
 */
public class IRODSApi {

	public static final Logger log = LogManager.getLogger();

	private static String appName = "irods4j";

	/**
	 * The iRODS connection object which enables communication with an iRODS server.
	 * 
	 * Users of the library are allowed to read the contents of this class. While
	 * the properties of this structure can be modified, doing so is highly
	 * discouraged.
	 * 
	 * @since 0.1.0
	 */
	public static class RcComm {
		public Socket socket;
		public Socket plainSocket;
		public SSLSocket sslSocket;

		public InputStream sin;
		public OutputStream sout;

		public boolean usingTLS = false;
		public boolean secure = false;
		public boolean loggedIn = false;

		public String clientUsername;
		public String clientUserZone;

		public String proxyUsername;
		public String proxyUserZone;

		public String sessionSignature;

		public String relVersion;
		public String apiVersion;
		public int status;
		public int cookie;

		public String hashAlgorithm;

		public RError_PI rError;
	}

	/**
	 * A class acting as a reference to a byte array.
	 * 
	 * The byte array holds byte[] instead of Byte[].
	 * 
	 * @since 0.1.0
	 */
	public static class ByteArrayReference {
		public byte[] data;
	}

	/**
	 * Sets the global application name identifying the client to the iRODS server.
	 * 
	 * This function is designed to be called only once. Attempting to call it more
	 * than one time will result in an exception. All connections made to the iRODS
	 * server will share the name set by this function.
	 * 
	 * Users of this library should call this function before establishing any
	 * connections to an iRODS server.
	 * 
	 * @param name The name used by the iRODS server to identify the client
	 *             application.
	 * 
	 * @since 0.1.0
	 */
	public static void setApplicationName(String name) {
		if (null == name || name.isEmpty()) {
			throw new IllegalArgumentException("Application name is null or empty");
		}

		if (!"irods4j".equals(appName)) {
			throw new RuntimeException("setApplicationName() invoked more than once");
		}

		appName = name;
	}

	private static void sendApiRequest(OutputStream out, int apiNumber) throws IOException {
		// Create the header describing the message.
		MsgHeader_PI mh = new MsgHeader_PI();
		mh.type = MsgHeader_PI.MsgType.RODS_API_REQ;
		mh.intInfo = apiNumber;

		// Send request.
		Network.write(out, mh);
		out.flush();
	}

	private static void sendApiRequest(OutputStream out, int apiNumber, Object data) throws IOException {
		String msgbody = XmlUtil.toXmlString(data);

		// Create the header describing the message.
		MsgHeader_PI mh = new MsgHeader_PI();
		mh.type = MsgHeader_PI.MsgType.RODS_API_REQ;
		mh.intInfo = apiNumber;
		mh.msgLen = msgbody.length();

		// Send request.
		Network.write(out, mh);
		Network.writeBytes(out, msgbody.getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	private static void sendApiRequest(OutputStream out, int apiNumber, Object data, byte[] bytes, int byteCount) throws IOException {
		String msgbody = XmlUtil.toXmlString(data);

		// Create the header describing the message.
		MsgHeader_PI mh = new MsgHeader_PI();
		mh.type = MsgHeader_PI.MsgType.RODS_API_REQ;
		mh.intInfo = apiNumber;
		mh.msgLen = msgbody.length();
		mh.bsLen = byteCount;

		// Send request.
		Network.write(out, mh);
		Network.writeBytes(out, msgbody.getBytes(StandardCharsets.UTF_8));
		Network.writeBytes(out, bytes, byteCount);
		out.flush();
	}

	private static <T> int receiveServerResponse(RcComm comm, Class<T> targetClass, Reference<T> output,
			ByteArrayReference bsBuffer) throws IOException {
		MsgHeader_PI mh = Network.readMsgHeader_PI(comm.sin);

		if (mh.msgLen > 0 && null != targetClass) {
			output.value = Network.readObject(comm.sin, mh.msgLen, targetClass);
		}

		if (mh.errorLen > 0) {
			comm.rError = Network.readObject(comm.sin, mh.errorLen, RError_PI.class);
		}

		if (mh.bsLen > 0 && null != bsBuffer) {
			Network.readBytes(comm.sin, bsBuffer.data, mh.bsLen);
		}

		return mh.intInfo;
	}

	/**
	 * A class which enables users to configure various connection options.
	 * 
	 * @since 0.1.0
	 */
	public static final class ConnectionOptions {
		public String clientServerNegotiation = "CS_NEG_REFUSE";

		public String sslTruststore;
		public String sslTruststorePassword;
		public String sslProtocol;

		public String encryptionAlgorithm = "AES-256-CBC";
		public int encryptionKeySize = 32;
		public int encryptionNumHashRounds = 16;
		public int encryptionSaltSize = 8;

		public String hashAlgorithm = "md5";

		public boolean enableTcpKeepAlive = true;
		public boolean enableTcpNoDelay = false;

		public int tcpSendBufferSize = -1;
		public int tcpReceiveBufferSize = -1;

		public boolean applySocketPerformancePreferences = false;
		public int ppConnectionTime = 0;
		public int ppLatency = 0;
		public int ppBandwidth = 0;

		public ConnectionOptions copy() {
			ConnectionOptions copy = new ConnectionOptions();

			copy.clientServerNegotiation = clientServerNegotiation;

			copy.sslTruststore = sslTruststore;
			copy.sslTruststorePassword = sslTruststorePassword;
			copy.sslProtocol = sslProtocol;

			copy.encryptionAlgorithm = encryptionAlgorithm;
			copy.encryptionKeySize = encryptionKeySize;
			copy.encryptionNumHashRounds = encryptionNumHashRounds;
			copy.encryptionSaltSize = encryptionSaltSize;

			copy.hashAlgorithm = hashAlgorithm;

			copy.enableTcpKeepAlive = enableTcpKeepAlive;
			copy.enableTcpNoDelay = enableTcpNoDelay;

			copy.tcpSendBufferSize = tcpSendBufferSize;
			copy.tcpReceiveBufferSize = tcpReceiveBufferSize;

			copy.applySocketPerformancePreferences = applySocketPerformancePreferences;
			copy.ppConnectionTime = ppConnectionTime;
			copy.ppLatency = ppLatency;
			copy.ppBandwidth = ppBandwidth;

			return copy;
		}
	}

	public static RcComm rcConnect(String host, int port, String clientUsername, String clientUserZone,
			Optional<String> proxyUsername, Optional<String> proxyUserZone, Optional<ConnectionOptions> options,
			Optional<RErrMsg_PI> errorInfo) throws Exception {
		if (null == host || host.isEmpty()) {
			throw new IllegalArgumentException("Host is null or empty");
		}

		if (port <= 0) {
			throw new IllegalArgumentException("Port is less than or equal to 0");
		}

		if (null == clientUsername || clientUsername.isEmpty()) {
			throw new IllegalArgumentException("Client username is null or empty");
		}

		if (null == clientUserZone || clientUserZone.isEmpty()) {
			throw new IllegalArgumentException("Client zone is null or empty");
		}

		ConnectionOptions connOptions = options.orElse(new ConnectionOptions());

		RcComm comm = new RcComm();
		comm.socket = comm.plainSocket = new Socket();

		comm.socket.setKeepAlive(connOptions.enableTcpKeepAlive);
		comm.socket.setTcpNoDelay(connOptions.enableTcpNoDelay);

		if (connOptions.tcpSendBufferSize > 0) {
			log.debug("Old socket send buffer size = {}", comm.socket.getSendBufferSize());
			comm.socket.setSendBufferSize(connOptions.tcpSendBufferSize);
			log.debug("New socket send buffer size = {}", comm.socket.getSendBufferSize());
		} else {
			log.debug("Socket send buffer size = {}", comm.socket.getSendBufferSize());
		}

		if (connOptions.tcpReceiveBufferSize > 0) {
			log.debug("Old socket receive buffer size = {}", comm.socket.getReceiveBufferSize());
			comm.socket.setReceiveBufferSize(connOptions.tcpReceiveBufferSize);
			log.debug("New socket receive buffer size = {}", comm.socket.getReceiveBufferSize());
		} else {
			log.debug("Socket receive buffer size = {}", comm.socket.getReceiveBufferSize());
		}

		if (connOptions.applySocketPerformancePreferences) {
			comm.socket.setPerformancePreferences(connOptions.ppConnectionTime, connOptions.ppLatency,
					connOptions.ppBandwidth);
		}

		comm.socket.connect(new InetSocketAddress(host, port));

		comm.sin = new BufferedInputStream(comm.socket.getInputStream());
		comm.sout = new BufferedOutputStream(comm.socket.getOutputStream());

		// Create the StartupPack message.
		// This is how a connection to iRODS is always initiated.
		StartupPack_PI sp = new StartupPack_PI();
		sp.clientUser = comm.clientUsername = clientUsername;
		sp.clientRcatZone = comm.clientUserZone = clientUserZone;
		sp.proxyUser = comm.proxyUsername = proxyUsername.orElse(clientUsername);
		sp.proxyRcatZone = comm.proxyUserZone = proxyUserZone.orElse(clientUserZone);
		sp.option = appName + "request_server_negotiation";
		String msgbody = XmlUtil.toXmlString(sp);

		// Create the header describing the StartupPack message.
		MsgHeader_PI hdr = new MsgHeader_PI();
		hdr.type = MsgHeader_PI.MsgType.RODS_CONNECT;
		hdr.msgLen = msgbody.length();

		// Send the message header and StartupPack (i.e. the message body).
		Network.write(comm.sout, hdr);
		Network.writeBytes(comm.sout, msgbody.getBytes(StandardCharsets.UTF_8));
		comm.sout.flush();

		// Read the message header from the server.
		MsgHeader_PI mh = Network.readMsgHeader_PI(comm.sin);
		log.debug("Received MsgHeader_PI: {}", XmlUtil.toXmlString(mh));

		if (mh.intInfo < 0) {
			if (errorInfo.isPresent()) {
				errorInfo.get().status = mh.intInfo;
				errorInfo.get().msg = "StartupPack error";
			}
			return null;
		}

		// Negotiation

		// Prepare to the negotiate whether a secure communication
		// channel is needed. The server's response will contain its
		// choice for secure communication.
		CS_NEG_PI csneg = Network.readObject(comm.sin, mh.msgLen, CS_NEG_PI.class);
		log.debug("Received CS_NEG_PI: {}", XmlUtil.toXmlString(csneg));

		// Check for negotiation errors.
		// 1 = CS_NEG_STATUS_SUCCESS
		// 0 = CS_NEG_STATUS_FAILURE
		// See irods_client_server_negotiation.hpp for these.
		if (1 != csneg.status) {
			log.error("Client-Server negotiation error: CS_NEG_STATUS={}", csneg.status);
			return null;
		}

		csneg = clientServerNegotiation(comm, connOptions.clientServerNegotiation, csneg.result);

		msgbody = XmlUtil.toXmlString(csneg);
		hdr.type = MsgHeader_PI.MsgType.RODS_CS_NEG_T;
		hdr.msgLen = msgbody.length();
		Network.write(comm.sout, hdr);
		Network.writeBytes(comm.sout, msgbody.getBytes(StandardCharsets.UTF_8));
		comm.sout.flush();

		// Read the message header from the server.
		mh = Network.readMsgHeader_PI(comm.sin);
		log.debug("Received MsgHeader_PI: {}", XmlUtil.toXmlString(mh));

		if (mh.intInfo < 0) {
			if (errorInfo.isPresent()) {
				errorInfo.get().status = mh.intInfo;
				errorInfo.get().msg = "Client-Server negotiation error";
			}
			return null;
		}

		// Capture the server version information.
		Version_PI vers = Network.readObject(comm.sin, mh.msgLen, Version_PI.class);
		log.debug("Received Version_PI: {}", XmlUtil.toXmlString(vers));
		comm.apiVersion = vers.apiVersion;
		comm.relVersion = vers.relVersion;
		comm.status = vers.status;
		comm.cookie = vers.cookie;

		// Store the desired hashing algorithm in the RcComm. This is needed
		// for password obfuscation (if the client wishes to manipulate user's
		// passwords).
		comm.hashAlgorithm = connOptions.hashAlgorithm;

		// TODO In the C implementation, this is where the network_plugin
		// is instantiated and the decision to use TLS happens. That decision
		// is based on the negotiation results, which are stored in the RcComm.
		// The RcComm holds information about encryption and other parameters.
		// That's why messages appear to be encrypted following the version
		// response from the server.
		enableTLS(comm, connOptions);

		return comm;
	}

	private static CS_NEG_PI clientServerNegotiation(RcComm comm, String clientNeg, String serverNeg) {
		CS_NEG_PI csneg = new CS_NEG_PI();

		csneg.status = 1;

		if (CS_NEG_PI.RESULT_CS_NEG_REQUIRE.equals(clientNeg)) {
			if (CS_NEG_PI.RESULT_CS_NEG_REQUIRE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_SSL;";
				comm.usingTLS = true;
			} else if (CS_NEG_PI.RESULT_CS_NEG_DONT_CARE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_SSL;";
				comm.usingTLS = true;
			} else if (CS_NEG_PI.RESULT_CS_NEG_REFUSE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_FAILURE;";
			}
		} else if (CS_NEG_PI.RESULT_CS_NEG_DONT_CARE.equals(clientNeg)) {
			if (CS_NEG_PI.RESULT_CS_NEG_REQUIRE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_SSL;";
				comm.usingTLS = true;
			} else if (CS_NEG_PI.RESULT_CS_NEG_DONT_CARE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_SSL;";
				comm.usingTLS = true;
			} else if (CS_NEG_PI.RESULT_CS_NEG_REFUSE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_TCP;";
			}
		} else if (CS_NEG_PI.RESULT_CS_NEG_REFUSE.equals(clientNeg)) {
			if (CS_NEG_PI.RESULT_CS_NEG_REQUIRE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_FAILURE;";
			} else if (CS_NEG_PI.RESULT_CS_NEG_DONT_CARE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_TCP;";
			} else if (CS_NEG_PI.RESULT_CS_NEG_REFUSE.equals(serverNeg)) {
				csneg.result = "cs_neg_result_kw=CS_NEG_USE_TCP;";
			}
		} else {
			csneg.status = 0; // CS_NEG_STATUS_FAILURE
			csneg.result = "cs_neg_result_kw=CS_NEG_FAILURE;";
		}

		return csneg;
	}

	private static void enableTLS(RcComm comm, ConnectionOptions options) throws Exception {
		if (comm.secure) {
			log.debug("SSL/TLS is already in use.");
			return;
		}

		if (!comm.usingTLS) {
			log.debug("Continuing without SSL/TLS.");
			return;
		}

		// This block is for loading self-signed certificates.
		if (null != options.sslTruststore) {
			log.debug("Loading Truststore.");
			KeyStore trustStore = KeyStore.getInstance("JKS");

			if (null != options.sslTruststorePassword) {
				try (FileInputStream fis = new FileInputStream(options.sslTruststore)) {
					trustStore.load(fis, options.sslTruststorePassword.toCharArray());
				}
			}

			log.debug("Initializing Truststore.");
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(trustStore);

			log.debug("Initializing SSL context.");
			SSLContext sslContext = null;
			if (null == options.sslProtocol) {
				sslContext = SSLContext.getDefault();
			} else {
				sslContext = SSLContext.getInstance(options.sslProtocol);
				sslContext.init(null, tmf.getTrustManagers(), null);
			}

			// Create SSLSocket and connect.
			log.debug("Securing socket communication.");
			SSLSocketFactory factory = sslContext.getSocketFactory();
			String host = comm.socket.getInetAddress().getHostAddress();
			int port = comm.socket.getPort();
			boolean autoCloseUnderlyingSocket = true;
			comm.sslSocket = (SSLSocket) factory.createSocket(comm.socket, host, port, autoCloseUnderlyingSocket);
			for (String p : comm.sslSocket.getSupportedProtocols()) {
				log.debug("Supported TLS protocol: {}", p);
			}
			for (String p : comm.sslSocket.getEnabledProtocols()) {
				log.debug("Enabled TLS protocol: {}", p);
			}
			comm.sslSocket.startHandshake();
			log.debug("Connection secured!");
		} else {
			// Handle certificates which live in the normal OS directories.
			log.debug("Securing socket communication.");
			SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			String host = comm.socket.getInetAddress().getHostAddress();
			int port = comm.socket.getPort();
			boolean autoCloseUnderlyingSocket = true;
			comm.sslSocket = (SSLSocket) factory.createSocket(comm.socket, host, port, autoCloseUnderlyingSocket);
			comm.sslSocket.startHandshake();
			log.debug("Connection secured!");
		}

		// See ssl.cpp in irods/irods to understand the following sequence of
		// operations. The code below follows ssl_client_start() and ssl_agent_start().

		// Send SSL encryption information to server.
		MsgHeader_PI mh = new MsgHeader_PI();
		mh.type = options.encryptionAlgorithm;
		mh.msgLen = options.encryptionKeySize;
		mh.errorLen = options.encryptionSaltSize;
		mh.bsLen = options.encryptionNumHashRounds;

		BufferedOutputStream sout = new BufferedOutputStream(comm.sslSocket.getOutputStream());
		Network.write(sout, mh);

		// Generate a random byte sequence as a key and send it to the server.
		byte[] key = new byte[options.encryptionKeySize];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(key);

		BytesBuf_PI bbuf = new BytesBuf_PI();
		bbuf.buflen = key.length;
		bbuf.buf = key;
		String msgbody = XmlUtil.toXmlString(bbuf);

		mh.type = "SHARED_SECRET";
		mh.msgLen = msgbody.length();
		mh.errorLen = 0;
		mh.bsLen = 0;
		mh.intInfo = 0;

		Network.write(sout, mh);
		Network.writeBytes(sout, msgbody.getBytes(StandardCharsets.UTF_8));
		sout.flush();

		// TODO Do equivalent of sslPostConnectionCheck().
		// See sslSockComm.cpp#L90 on the main branch. The function of
		// interest is sslStart(). If the check fails, sslEnd() is called,
		// which invokes rcSslEnd() and returns an error of SSL_CERT_ERROR.

		// This keeps the rest of the code from needing to know
		// the type of the socket used for communication.
		comm.socket = comm.sslSocket;
		comm.sin = new BufferedInputStream(comm.sslSocket.getInputStream());
		comm.sout = sout;

		// Used as a signal to this function to guard against this
		// function being executed multiple times.
		comm.secure = true;
	}

	// TODO Consider removing this function.
	private static int rcSslStart(RcComm comm) throws IOException {
		SSLStartInp_PI input = new SSLStartInp_PI();
		input.arg0 = null;
		sendApiRequest(comm.sout, 1100, input);

		// Read the message header from the server.
		MsgHeader_PI mh = Network.readMsgHeader_PI(comm.sin);
		log.debug("Received MsgHeader_PI: {}", XmlUtil.toXmlString(mh));

		return mh.intInfo;
	}

	// TODO Consider removing this function.
	private static int rcSslEnd(RcComm comm) throws IOException {
		SSLEndInp_PI input = new SSLEndInp_PI();
		sendApiRequest(comm.sout, 1101, input);

		// Read the message header from the server.
		MsgHeader_PI mh = Network.readMsgHeader_PI(comm.sin);
		log.debug("Received MsgHeader_PI: {}", XmlUtil.toXmlString(mh));

		return mh.intInfo;
	}

	public static void rcDisconnect(RcComm comm) throws IOException {
		final MsgHeader_PI hdr = new MsgHeader_PI();
		hdr.type = MsgHeader_PI.MsgType.RODS_DISCONNECT;
		Network.write(comm.sout, hdr);
		comm.sout.flush();
		if (comm.secure) {
			comm.sslSocket.close();
		}
		else {
			comm.socket.close();
		}
	}

	public static void rcAuthenticateClient(RcComm comm, AuthPlugin authPlugin, String password) throws Exception {
		ObjectNode input = JsonUtil.getJsonMapper().createObjectNode();
		input.put("password", password);
		input.put(AuthPlugin.AUTH_TTL_KEY, "0"); // TODO Expose this option and others.
		AuthManager.authenticateClient(comm, authPlugin, input);
	}

	public static int rcObjStat(RcComm comm, DataObjInp_PI input, Reference<RodsObjStat_PI> output) throws IOException {
		sendApiRequest(comm.sout, 633, input);
		return receiveServerResponse(comm, RodsObjStat_PI.class, output, null);
	}

	public static int rcGenQuery(RcComm comm, GenQueryInp_PI input, Reference<GenQueryOut_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 702, input);
		return receiveServerResponse(comm, GenQueryOut_PI.class, output, null);
	}

	public static int rcGenQuery2(RcComm comm, Genquery2Input_PI input, Reference<String> output) throws IOException {
		sendApiRequest(comm.sout, 10221, input);
		Reference<STR_PI> outputPI = new Reference<STR_PI>();
		int ec = receiveServerResponse(comm, STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

	public static int rcReplicaOpen(RcComm comm, DataObjInp_PI input, Reference<String> output) throws IOException {
		sendApiRequest(comm.sout, 20003, input);
		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}
		return ec;
	}

	public static int rcReplicaTruncate(RcComm comm, DataObjInp_PI input, Reference<String> output) throws IOException {
		sendApiRequest(comm.sout, 802, input);
		Reference<STR_PI> outputPI = new Reference<STR_PI>();
		int ec = receiveServerResponse(comm, STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

	public static int rcDataObjLseek(RcComm comm, OpenedDataObjInp_PI input, Reference<FileLseekOut_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 674, input);
		return receiveServerResponse(comm, FileLseekOut_PI.class, output, null);
	}

	public static int rcDataObjRead(RcComm comm, OpenedDataObjInp_PI input, ByteArrayReference byteArray)
			throws IOException {
		sendApiRequest(comm.sout, 675, input);
		return receiveServerResponse(comm, null, null, byteArray);
	}

	public static int rcDataObjWrite(RcComm comm, OpenedDataObjInp_PI input, byte[] buffer) throws IOException {
		sendApiRequest(comm.sout, 676, input, buffer, input.len);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcReplicaClose(RcComm comm, String closeOptions) throws IOException {
		BinBytesBuf_PI input = new BinBytesBuf_PI();
		input.buf = closeOptions;
		input.buflen = closeOptions.length();
		sendApiRequest(comm.sout, 20004, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcAtomicApplyMetadataOperations(RcComm comm, String input, Reference<String> output)
			throws IOException {
		BinBytesBuf_PI bbbuf = new BinBytesBuf_PI();
		bbbuf.buf = input;
		bbbuf.buflen = input.length();
		sendApiRequest(comm.sout, 20002, bbbuf);

		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}

		return ec;
	}

	public static int rcAtomicApplyAclOperations(RcComm comm, String input, Reference<String> output)
			throws IOException {
		BinBytesBuf_PI bbbuf = new BinBytesBuf_PI();
		bbbuf.buf = input;
		bbbuf.buflen = input.length();
		sendApiRequest(comm.sout, 20005, bbbuf);

		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}

		return ec;
	}

	public static int rcTouch(RcComm comm, String input) throws IOException {
		BinBytesBuf_PI bbbuf = new BinBytesBuf_PI();
		bbbuf.buf = input;
		bbbuf.buflen = input.length();
		sendApiRequest(comm.sout, 20007, bbbuf);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcGetGridConfigurationValue(RcComm comm, GridConfigurationInp_PI input,
			Reference<GridConfigurationOut_PI> output) throws IOException {
		sendApiRequest(comm.sout, 20009, input);
		return receiveServerResponse(comm, GridConfigurationOut_PI.class, output, null);
	}

	public static int rcSetGridConfigurationValue(RcComm comm, GridConfigurationInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 20010, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcSetDelayServerMigrationInfo(RcComm comm, DelayServerMigrationInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 20011, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcGetDelayRuleInfo(RcComm comm, String input, Reference<String> output) throws IOException {
		STR_PI strPI = new STR_PI();
		strPI.myStr = input;
		sendApiRequest(comm.sout, 20013, strPI);

		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}

		return ec;
	}

	public static int rcGetFileDescriptorInfo(RcComm comm, String input, Reference<String> output) throws IOException {
		BinBytesBuf_PI bbbuf = new BinBytesBuf_PI();
		bbbuf.buf = input;
		bbbuf.buflen = input.length();
		sendApiRequest(comm.sout, 20000, bbbuf);

		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}

		return ec;
	}

	public static int rcSwitchUser(RcComm comm, SwitchUserInp_PI input) throws IOException {
		// Accepting empty strings for either parameter is not allowed.
		// This is especially true for the "_zone" parameter because this function
		// copies the string pointed to by "_zone" into the RcComm. Copying an empty
		// string into the RcComm's.
		if (null == input.username || input.username.isEmpty()) {
			throw new IllegalArgumentException("Username is null or empty");
		}

		if (null == input.zone || input.zone.isEmpty()) {
			throw new IllegalArgumentException("Zone is null or empty");
		}

		if (comm.clientUsername.equals(input.username) && comm.clientUserZone.equals(input.zone)) {
			return 0;
		}

		sendApiRequest(comm.sout, 20012, input);
		int ec = receiveServerResponse(comm, null, null, null);

		if (0 == ec) {
			comm.clientUsername = input.username;
			comm.clientUserZone = input.zone;

			if (null != input.KeyValPair_PI && input.KeyValPair_PI.ssLen > 0
					&& input.KeyValPair_PI.keyWord.contains(SwitchUserInp_PI.KW_SWITCH_PROXY_USER)) {
				comm.proxyUsername = input.username;
				comm.proxyUserZone = input.zone;
			}
		}

		return ec;
	}

	public static int rcCheckAuthCredentials(RcComm comm, DataObjInp_PI input, Reference<Integer> output)
			throws IOException {
		sendApiRequest(comm.sout, 800, input);
		Reference<INT_PI> outputPI = new Reference<INT_PI>();
		int ec = receiveServerResponse(comm, INT_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myInt;
		}
		return ec;
	}

	// TODO Should this API be exposed to clients?
	public static int rcRegisterPhysicalPath(RcComm comm, DataObjInp_PI input, Reference<String> output)
			throws IOException {
		sendApiRequest(comm.sout, 20008, input);
		Reference<BinBytesBuf_PI> outputPI = new Reference<BinBytesBuf_PI>();
		int ec = receiveServerResponse(comm, BinBytesBuf_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.buf;
		}
		return ec;
	}

	public static int rcModAVUMetadata(RcComm comm, ModAVUMetadataInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 706, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcModAccessControl(RcComm comm, ModAccessControlInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 707, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcModDataObjMeta(RcComm comm, ModDataObjMeta_PI input) throws IOException {
		sendApiRequest(comm.sout, 622, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDataObjectModifyInfo(RcComm comm, ModDataObjMeta_PI input) throws IOException {
		sendApiRequest(comm.sout, 20001, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDataObjRename(RcComm comm, DataObjCopyInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 627, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDataObjCopy(RcComm comm, DataObjCopyInp_PI input, Reference<TransferStat_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 696, input);
		return receiveServerResponse(comm, TransferStat_PI.class, output, null);
	}

	public static int rcDataObjRepl(RcComm comm, DataObjInp_PI input, Reference<TransferStat_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 695, input);
		return receiveServerResponse(comm, TransferStat_PI.class, output, null);
	}

	public static int rcDataObjCreate(RcComm comm, DataObjInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 601, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDataObjChksum(RcComm comm, DataObjInp_PI input, Reference<String> output) throws IOException {
		sendApiRequest(comm.sout, 629, input);
		Reference<STR_PI> outputPI = new Reference<STR_PI>();
		int ec = receiveServerResponse(comm, STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

	public static int rcDataObjUnlink(RcComm comm, DataObjInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 615, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDataObjTrim(RcComm comm, DataObjInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 632, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDelayRuleLock(RcComm comm, DelayRuleLockInput_PI input) throws IOException {
		sendApiRequest(comm.sout, 10222, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcDelayRuleUnlock(RcComm comm, DelayRuleUnlockInput_PI input) throws IOException {
		sendApiRequest(comm.sout, 10223, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcCollCreate(RcComm comm, CollInpNew_PI input) throws IOException {
		sendApiRequest(comm.sout, 681, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcModColl(RcComm comm, CollInpNew_PI input) throws IOException {
		sendApiRequest(comm.sout, 680, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcOpenCollection(RcComm comm, CollInpNew_PI input) throws IOException {
		sendApiRequest(comm.sout, 678, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcReadCollection(RcComm comm, int handle, Reference<CollEnt_PI> output) throws IOException {
		INT_PI input = new INT_PI();
		input.myInt = handle;
		sendApiRequest(comm.sout, 713, input);
		return receiveServerResponse(comm, CollEnt_PI.class, output, null);
	}

	public static int rcCloseCollection(RcComm comm, int handle) throws IOException {
		INT_PI input = new INT_PI();
		input.myInt = handle;
		sendApiRequest(comm.sout, 661, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcRmColl(RcComm comm, CollInpNew_PI input, Reference<CollOprStat_PI> output) throws IOException {
		sendApiRequest(comm.sout, 679, input);
		return receiveServerResponse(comm, CollOprStat_PI.class, output, null);
	}

	public static int rcTicketAdmin(RcComm comm, TicketAdminInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 723, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcUnregDataObj(RcComm comm, UnregDataObj_PI input) throws IOException {
		sendApiRequest(comm.sout, 620, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcUserAdmin(RcComm comm, UserAdminInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 714, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcSpecificQuery(RcComm comm, SpecificQueryInp_PI input, Reference<GenQueryOut_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 722, input);
		return receiveServerResponse(comm, GenQueryOut_PI.class, output, null);
	}

	public static int rcGetResourceInfoForOperation(RcComm comm, DataObjInp_PI input, Reference<String> output)
			throws IOException {
		sendApiRequest(comm.sout, 10220, input);
		Reference<STR_PI> outputPI = new Reference<STR_PI>();
		int ec = receiveServerResponse(comm, STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

	public static int rcZoneReport(RcComm comm, Reference<BytesBuf_PI> output) throws IOException {
		sendApiRequest(comm.sout, 10205);
		return receiveServerResponse(comm, BytesBuf_PI.class, output, null);
	}

	public static int rcGetMiscSvrInfo(RcComm comm, Reference<MiscSvrInfo_PI> output) throws IOException {
		sendApiRequest(comm.sout, 700);
		return receiveServerResponse(comm, MiscSvrInfo_PI.class, output, null);
	}

	public static int rcGeneralAdmin(RcComm comm, GeneralAdminInp_PI input) throws IOException {
		sendApiRequest(comm.sout, 701, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcExecMyRule(RcComm comm, ExecMyRuleInp_PI input, Reference<MsParamArray_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 625, input);
		return receiveServerResponse(comm, MsParamArray_PI.class, output, null);
	}

	public static int rcProcStat(RcComm comm, ProcStatInp_PI input, Reference<GenQueryOut_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 690);
		return receiveServerResponse(comm, GenQueryOut_PI.class, output, null);
	}

	public static int rcRuleExecSubmit(RcComm comm, RULE_EXEC_DEL_INP_PI input, Reference<String> output)
			throws IOException {
		sendApiRequest(comm.sout, 623, input);
		Reference<IRODS_STR_PI> outputPI = new Reference<IRODS_STR_PI>();
		int ec = receiveServerResponse(comm, IRODS_STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

	public static int rcRuleExecMod(RcComm comm, RULE_EXEC_MOD_INP_PI input) throws IOException {
		sendApiRequest(comm.sout, 708, input);
		return receiveServerResponse(comm, null, null, null);
	}

	public static int rcRuleExecDel(RcComm comm, RULE_EXEC_DEL_INP_PI input) throws IOException {
		sendApiRequest(comm.sout, 624, input);
		return receiveServerResponse(comm, null, null, null);
	}

	// TODO This API is likely for server-side use only due to it only being invoked
	// within the server. Consider removing this.
	private static int rcRegReplica(RcComm comm, RegReplica_PI input) throws IOException {
		sendApiRequest(comm.sout, 621, input);
		return receiveServerResponse(comm, null, null, null);
	}

	// TODO This API is not used in server at all. We should probably deprecate it
	// or remove
	// it from the public interface. Consider removing this.
	private static int rcRegDataOb(RcComm comm, DataObjInfo_PI input, Reference<DataObjInfo_PI> output)
			throws IOException {
		sendApiRequest(comm.sout, 619, input);
		return receiveServerResponse(comm, DataObjInfo_PI.class, output, null);
	}

	public static int rcGetLibraryFeatures(RcComm comm, Reference<String> output) throws IOException {
		sendApiRequest(comm.sout, 801);
		Reference<STR_PI> outputPI = new Reference<STR_PI>();
		int ec = receiveServerResponse(comm, STR_PI.class, outputPI, null);
		if (null != outputPI.value) {
			output.value = outputPI.value.myStr;
		}
		return ec;
	}

}
