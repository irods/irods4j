package org.irods.irods4j.authentication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;

import java.io.IOException;

/**
 * This class implements the irods authentication scheme.
 * <p>
 * It was introduced in iRODS 5.1.0 and acts as a modern replacement for
 * the native authentication scheme.
 * <p>
 * For more information, see <a href="https://docs.irods.org/5.1.0/plugins/pluggable_authentication/">...</a>.
 *
 * @since 0.8.0
 */
public class IRODSAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	// Operation names
	private static final String CLIENT_INIT_AUTH_WITH_SERVER = "client_init_auth_with_server";
	private static final String CLIENT_PREPARE_AUTH_CHECK = "client_prepare_auth_check";
	private static final String CLIENT_AUTH_WITH_PASSWORD = "client_auth_with_password";
	private static final String CLIENT_AUTH_WITH_SESSION_TOKEN = "client_auth_with_session_token";
	private static final String SERVER_PREPARE_AUTH_CHECK = "server_prepare_auth_check";
	private static final String SERVER_AUTH_WITH_PASSWORD = "server_auth_with_password";
	private static final String SERVER_AUTH_WITH_SESSION_TOKEN = "server_auth_with_session_token";

	// Other keys and constants
	private static final String KW_PASSWORD = "password";
	private static final String KW_SESSION_TOKEN = "session_token";
	private static final String KW_USER_NAME = "user_name";
	private static final String KW_ZONE_NAME = "zone_name";

	private boolean requireSecureConnection = true;
	private Reference<String> sessionToken;

	/**
	 * Construct a new irods plugin instance.
	 * <p>
	 * The connected iRODS server MUST be iRODS 5.1.0 or later.
	 *
	 * @param requireSecureConnection Instructs the authentication flow to fail if the
	 *                                connection isn't secure.
	 * @param sessionToken            The reference object that will contain the generated
	 *                                session token on success.
	 * @throws IllegalArgumentException If the reference object for storing the session
	 *                                  token is null.
	 * @since 0.8.0
	 */
	public IRODSAuthPlugin(boolean requireSecureConnection, Reference<String> sessionToken) {
		if (null == sessionToken) {
			throw new IllegalArgumentException("sessionToken is null");
		}

		this.requireSecureConnection = requireSecureConnection;
		this.sessionToken = sessionToken;

		addOperation(CLIENT_INIT_AUTH_WITH_SERVER, this::clientInitAuthWithServer);
		addOperation(CLIENT_PREPARE_AUTH_CHECK, this::clientPrepareAuthCheck);
		addOperation(CLIENT_AUTH_WITH_PASSWORD, this::clientAuthWithPassword);
		addOperation(CLIENT_AUTH_WITH_SESSION_TOKEN, this::clientAuthWithSessionToken);
	}

	@Override
	public String getName() {
		return "irods";
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, CLIENT_INIT_AUTH_WITH_SERVER);

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put(KW_USER_NAME, comm.proxyUsername);
		resp.put(KW_ZONE_NAME, comm.proxyUserZone);

		return resp;
	}

	private JsonNode clientInitAuthWithServer(RcComm comm, JsonNode context) throws IRODSException, IOException {
		if (requireSecureConnection) {
			if (!comm.secure) {
				throw new IllegalStateException("SSL/TLS is required for irods authentication");
			}
		} else if (!comm.secure) {
			log.warn("Using insecure channel for authentication. Password will be visible on the network.");
		}

		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, SERVER_PREPARE_AUTH_CHECK);
		ObjectNode resp = (ObjectNode) request(comm, req);
		resp.put(AUTH_NEXT_OPERATION, CLIENT_PREPARE_AUTH_CHECK);
		return resp;
	}

	private JsonNode clientPrepareAuthCheck(RcComm comm, JsonNode context) {
		if (!context.has(KW_USER_NAME) || !context.has(KW_ZONE_NAME)) {
			throw new IllegalStateException("Missing user_name and/or zone_name");
		}

		ObjectNode resp = (ObjectNode) context.deepCopy();

		// TODO(#31): Update this if force-password-prompt option is ever supported.

		// The anonymous user does not require a session token or password to authenticate.
		if ("anonymous".equals(context.get(KW_USER_NAME).asText())) {
			resp.put(KW_PASSWORD, "");
			resp.put(AUTH_NEXT_OPERATION, CLIENT_AUTH_WITH_PASSWORD);
			return resp;
		}

		// If a password is provided by the client to the plugin, authenticate with that.
		if (context.has(KW_PASSWORD)) {
			resp.put(AUTH_NEXT_OPERATION, CLIENT_AUTH_WITH_PASSWORD);
			return resp;
		}

		// if a session token is provided by the client to the plugin, authenticate with that.
		if (context.has(KW_SESSION_TOKEN)) {
			resp.put(AUTH_NEXT_OPERATION, CLIENT_AUTH_WITH_SESSION_TOKEN);
			return resp;
		}

		// The C++ implementation checks the session token file locations here. irods4j does
		// not support this. This is intentionally left out.

		// At this point, no password or session token was provided. The user needs to
		// include a password or session token in the JSON payload.

		throw new IllegalStateException("Missing password or session token");
	}

	private JsonNode clientAuthWithPassword(RcComm comm, JsonNode context) throws IRODSException, IOException {
		if (!context.has(KW_USER_NAME) || !context.has(KW_ZONE_NAME) || !context.has(KW_PASSWORD)) {
			throw new IllegalStateException("Missing user_name, zone_name, and/or password");
		}

		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, SERVER_AUTH_WITH_PASSWORD);
		ObjectNode resp = (ObjectNode) request(comm, req);
		resp.put(AUTH_NEXT_OPERATION, CLIENT_AUTH_WITH_SESSION_TOKEN);

		return resp;
	}

	private JsonNode clientAuthWithSessionToken(RcComm comm, JsonNode context) throws IRODSException, IOException {
		if (!context.has(KW_USER_NAME) || !context.has(KW_ZONE_NAME) || !context.has(KW_SESSION_TOKEN)) {
			throw new IllegalStateException("Missing user_name, zone_name, and/or session token");
		}

		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, SERVER_AUTH_WITH_SESSION_TOKEN);
		ObjectNode resp = (ObjectNode) request(comm, req);

		// The C++ implementation writes the token to the session file here. irods4j
		// does not support this. Instead, irods4j exposes the session object through
		// a reference object.
		sessionToken.value = req.get(KW_SESSION_TOKEN).asText();

		comm.loggedIn = true;
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);

		return resp;
	}

}
