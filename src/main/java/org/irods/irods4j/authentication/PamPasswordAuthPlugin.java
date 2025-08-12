package org.irods.irods4j.authentication;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PamPasswordAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	private static final String PERFORM_NATIVE_AUTH = "perform_native_auth";

	private boolean requireSecureConnection = true;

	public PamPasswordAuthPlugin(boolean requireSecureConnection) {
		this.requireSecureConnection = requireSecureConnection;
		addOperation(AUTH_CLIENT_AUTH_REQUEST, this::clientRequest);
		addOperation(PERFORM_NATIVE_AUTH, this::performNativeAuth);
	}

	public String getName() {
		return "pam_password";
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_REQUEST);

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put("user_name", comm.proxyUsername);
		resp.put("zone_name", comm.proxyUserZone);

		// Map the user's password to the correct property name.
		resp.put(AUTH_PASSWORD_KEY, context.get("password").asText());

		// DO NOT leak the user's password!
		resp.remove("password");

		return resp;
	}

	private JsonNode clientRequest(RcComm comm, JsonNode context) throws IOException, IRODSException {
		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_REQUEST);

		if (requireSecureConnection) {
			if (!comm.secure) {
				throw new IllegalStateException("SSL/TLS is required for PAM authentication");
			}
		}
		else if (!comm.secure) {
			log.warn("Using insecure channel for authentication. Password will be visible on the network.");
		}

		JsonNode resp = request(comm, req);
		((ObjectNode) resp).put(AUTH_NEXT_OPERATION, PERFORM_NATIVE_AUTH);

		if (!resp.has("request_result")) {
			throw new IllegalStateException("Missing property: request_result");
		}

		// The C++ implementation obfuscates "request_result" and stores it in
		// the user's .irodsA file for native authentication to pick up.

		return resp;
	}

	private JsonNode performNativeAuth(RcComm comm, JsonNode context) throws Exception {
		ObjectNode resp = (ObjectNode) context.deepCopy();

		// Remove the PAM password from the payload so it's not sent over the network.
		resp.remove(AUTH_PASSWORD_KEY);

		// Authenticate using the native authentication and the server generated
		// password mapped to "request_result".
		ObjectNode input = JsonUtil.getJsonMapper().createObjectNode();
		input.put("password", resp.get("request_result").asText());
		AuthManager.authenticateClient(comm, new NativeAuthPlugin(), input);

		// If everything completed successfully, the flow is complete and we can
		// consider the user to be logged in. The native auth flow was run and so
		// we trust the result.
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);

		// The native auth plugin sets this on success, so this isn't necessary.
		// We'll set it anyway to align with the C++ implementation, just to be on
		// the safe side.
		comm.loggedIn = true;

		return resp;
	}

}
