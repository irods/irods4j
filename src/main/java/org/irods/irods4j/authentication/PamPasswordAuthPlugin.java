package org.irods.irods4j.authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PamPasswordAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	private static final String PERFORM_NATIVE_AUTH = "perform_native_auth";

	// Notifies the plugin that the user has previously authenticated and
	// provided a session token instead of their true password. This causes
	// the plugin to perform native authentication only.
	private static final String KW_SESSION_TOKEN = "session_token";

	private boolean requireSecureConnection = true;
	private Reference<String> sessionToken;

	/**
	 * Construct a new PAM-password plugin instance.
	 *
	 * @param requireSecureConnection Instructs the authentication flow to fail if the
	 *                                connection isn't secure.
	 * @param sessionToken            The reference object that will contain the generated
	 *                                session token on success. The value MUST NOT be used
	 *                                if authentication fails.
	 * @throws IllegalArgumentException If the reference object for storing the session
	 *                                  token is null.
	 * @since 0.1.0
	 */
	public PamPasswordAuthPlugin(boolean requireSecureConnection, Reference<String> sessionToken) {
		if (null == sessionToken) {
			throw new IllegalArgumentException("sessionToken is null");
		}

		this.requireSecureConnection = requireSecureConnection;
		this.sessionToken = sessionToken;

		addOperation(AUTH_CLIENT_AUTH_REQUEST, this::clientRequest);
		addOperation(PERFORM_NATIVE_AUTH, this::performNativeAuth);
	}

	/**
	 * @see #PamPasswordAuthPlugin(boolean, Reference)
	 * <p>
	 * This constructor is for cases where a valid session token is expected to be
	 * used.
	 *
	 * @since 0.1.0
	 */
	public PamPasswordAuthPlugin(boolean requireSecureConnection) {
		this(requireSecureConnection, new Reference<>());
	}

	public String getName() {
		return "pam_password";
	}

	/**
	 * Returns a map containing inputs for authenticating via
	 * {@link org.irods.irods4j.low_level.api.IRODSApi#rcAuthenticateClient}
	 *
	 * @param password The password of the proxy user or client user.
	 * @param ttl      The amount of time (in hours) before the session expires.
	 *                 Set to 0 to disable session expiration.
	 *
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(Password password, long ttl) {
		Map<String, Object> input = new HashMap<>();
		input.put(AUTH_PASSWORD_KEY, password.value);
		input.put(AUTH_TTL_KEY, String.valueOf(ttl));
		return input;
	}

	/**
	 * @see #initAuthArguments(Password, long)
	 *
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(Password password) {
		Map<String, Object> input = new HashMap<>();
		input.put(AUTH_PASSWORD_KEY, password.value);
		return input;
	}

	/**
	 * Returns a map containing inputs for authenticating via
	 * {@link org.irods.irods4j.low_level.api.IRODSApi#rcAuthenticateClient}
	 *
	 * @param sessionToken The authentication token for the proxy or client user.
	 *
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(SessionToken sessionToken) {
		Map<String, Object> input = new HashMap<>();
		input.put(KW_SESSION_TOKEN, sessionToken.value);
		return input;
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		ObjectNode resp = (ObjectNode) context.deepCopy();

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put("user_name", comm.proxyUsername);
		resp.put("zone_name", comm.proxyUserZone);

		// irods4j does not support .irodsA files. This keyword enables functionality
		// similar to the obfGetPw/obfSavePw functions provided by the iRODS C API.
		// This keyword is a feature of irods4j. The C++ implementation does not support it.
		JsonNode sessionTokenNode = resp.remove(KW_SESSION_TOKEN);
		if (null != sessionTokenNode) {
			sessionToken.value = sessionTokenNode.asText();
			resp.put(AUTH_NEXT_OPERATION, PERFORM_NATIVE_AUTH);
		}
		else {
			resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_REQUEST);
		}

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

		ObjectNode resp = (ObjectNode) request(comm, req);

		if (!resp.has("request_result")) {
			throw new IllegalStateException("Missing property: request_result");
		}

		// The C++ implementation obfuscates "request_result" and stores it in
		// the user's .irodsA file for native authentication to pick up. irods4j
		// does not prescribe how the value should be stored. That decision is left
		// up to the user of the library.
		sessionToken.value = resp.get("request_result").asText();

		resp.put(AUTH_NEXT_OPERATION, PERFORM_NATIVE_AUTH);

		return resp;
	}

	private JsonNode performNativeAuth(RcComm comm, JsonNode context) throws Exception {
		// Authenticate using native authentication. This operation just runs the
		// entire native authentication flow because that is how PAM authentication
		// has worked historically. This is done in order to minimize communication
		// with the PAM server.
		ObjectNode input = JsonUtil.getJsonMapper().createObjectNode();
		input.put(AUTH_PASSWORD_KEY, sessionToken.value);
		AuthManager.authenticateClient(comm, new NativeAuthPlugin(), input);

		// The native auth plugin sets this on success, so this isn't necessary.
		// We'll set it anyway to align with the C++ implementation, just to be on
		// the safe side.
		comm.loggedIn = true;

		// If everything completed successfully, the flow is complete, and we can
		// consider the user to be logged in. The native auth flow was run, and so
		// we trust the result.
		return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);
	}

}
