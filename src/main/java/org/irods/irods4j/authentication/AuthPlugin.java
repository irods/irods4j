package org.irods.irods4j.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.network.Network;
import org.irods.irods4j.low_level.protocol.packing_instructions.BinBytesBuf_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsgHeader_PI;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	public static final String AUTH_CLIENT_START = "auth_client_start";
	public static final String AUTH_AGENT_START = "auth_agent_start";
	public static final String AUTH_ESTABLISH_CONTEXT = "auth_establish_context";
	public static final String AUTH_CLIENT_AUTH_REQUEST = "auth_client_auth_request";
	public static final String AUTH_AGENT_AUTH_REQUEST = "auth_agent_auth_request";
	public static final String AUTH_CLIENT_AUTH_RESPONSE = "auth_client_auth_response";
	public static final String AUTH_AGENT_AUTH_RESPONSE = "auth_agent_auth_response";
	public static final String AUTH_AGENT_AUTH_VERIFY = "auth_agent_auth_verify";

	public static final String AUTH_FLOW_COMPLETE = "authentication_flow_complete";
	public static final String AUTH_NEXT_OPERATION = "next_operation";

	// TODO This one may not be necessary. It is used in the C++ implementations
	// to control whether the plugins prompt the user for input. That is something
	// the developer can implement ahead of time, I think. Then again, the plugin
	// may be designed to prompt the client at certain times.
	public static final String AUTH_FORCE_PASSWORD_PROMPT = "force_password_prompt";

	// Client Options
	public static final String AUTH_USER_KEY = "a_user";
	public static final String AUTH_SCHEME_KWY = "a_scheme";
	public static final String AUTH_TTL_KEY = "a_ttl";
	public static final String AUTH_PASSWORD_KEY = "a_pw";
	public static final String AUTH_RESPONSE_KEY = "a_resp";

	private Map<String, AuthPluginOperation> operations = new HashMap<>();

	public AuthPlugin() {
		addOperation(AUTH_CLIENT_START, this::authClientStart);
	}

	/**
	 * @return The name of the authentication scheme.
	 */
	public abstract String getName();

	protected void addOperation(String opName, AuthPluginOperation op) {
		operations.put(opName, op);
	}

	public JsonNode execute(RcComm comm, String operation, JsonNode context) throws Exception {
		var op = operations.get(operation);
		if (null == op) {
			throw new NameNotFoundException("Operation not supported: " + operation);
		}
		return op.execute(comm, context);
	}

	public abstract JsonNode authClientStart(RcComm comm, JsonNode context);

	protected JsonNode request(RcComm comm, JsonNode msg) throws IOException, IRODSException {
		var json = JsonUtil.toJsonString(msg);
		var bbbuf = new BinBytesBuf_PI();
		bbbuf.buf = json;
		bbbuf.buflen = json.length();
		var msgbody = XmlUtil.toXmlString(bbbuf);

		var hdr = new MsgHeader_PI();
		hdr.type = MsgHeader_PI.MsgType.RODS_API_REQ;
		hdr.msgLen = msgbody.length();
		hdr.intInfo = 110000; // New auth plugin framework API number.

		Network.write(comm.sout, hdr);
		Network.writeBytes(comm.sout, msgbody.getBytes(StandardCharsets.UTF_8));
		comm.sout.flush();

		// Read the message header from the server.
		var mh = Network.readMsgHeader_PI(comm.sin);
		if (log.isDebugEnabled()) {
			log.debug("Received MsgHeader_PI: {}", XmlUtil.toXmlString(mh));
		}

		// Check for errors.
		if (mh.intInfo < 0) {
			throw new IRODSException(mh.intInfo, "Client request error");
		}

		// TODO Can rError information be returned here? If so, that would break this
		// implementation because we don't handle it. Although it would be weird if the
		// check above indicated success and rError information existed in the stream.

		bbbuf = Network.readObject(comm.sin, mh.msgLen, BinBytesBuf_PI.class);
		var jm = JsonUtil.getJsonMapper();
		return jm.readTree(bbbuf.buf);
	}

}
