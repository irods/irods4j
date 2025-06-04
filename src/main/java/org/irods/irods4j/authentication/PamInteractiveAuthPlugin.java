package org.irods.irods4j.authentication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Function;

/**
 * This class implements the pam_interactive authentication scheme.
 * <p>
 * It enables organizations to develop dynamic authentication flows that
 * meet their needs.
 *
 * @since 0.3.0
 */
public class PamInteractiveAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	private static final String PERFORM_RUNNING = "running";
	private static final String PERFORM_READY = "ready";
	private static final String PERFORM_WAITING = "waiting";
	private static final String PERFORM_WAITING_PW = "waiting_pw";
	private static final String PERFORM_RESPONSE = "response";
	private static final String PERFORM_NEXT = "next";
	private static final String PERFORM_ERROR = "error";
	private static final String PERFORM_TIMEOUT = "timeout";
	private static final String PERFORM_AUTHENTICATED = "authenticated";
	private static final String PERFORM_NOT_AUTHENTICATED = "not_authenticated";
	private static final String PERFORM_NATIVE_AUTH = "perform_native_auth";

	private boolean requireSecureConnection = true;
	private final Function<String, String> getInputHandler;
	private final Function<String, String> getSensitiveInputHandler;

	/**
	 * Construct a new PAM Interactive plugin instance.
	 * <p>
	 * All user input is read from stdin. User input containing sensitive information
	 * is never echoed to stdout.
	 * <p>
	 * Spaces will be removed from non-sensitive user input.
	 *
	 * @param requireSecureConnection Instructs the authentication flow to fail if
	 *                                the connection isn't secure.
	 * @since 0.3.0
	 */
	public PamInteractiveAuthPlugin(boolean requireSecureConnection) {
		this(requireSecureConnection, PamInteractiveAuthPlugin::getInputFromClientStdin, PamInteractiveAuthPlugin::getPasswordFromClientStdin);
	}

	/**
	 * Construct a new PAM Interactive plugin instance with custom input handlers.
	 * <p>
	 * This constructor is for situations where the environment doesn't rely on stdin
	 * - e.g. a GUI application. The callbacks passed to this constructor are responsible
	 * for securing user input.
	 * <p>
	 * Callbacks MUST accept a prompt and return a string representing the user's input.
	 * The returned string MUST be non-null.
	 *
	 * @param requireSecureConnection  Instructs the authentication flow to fail if the
	 *                                 connection isn't secure.
	 * @param getInputHandler          The callback used for accepting non-sensitive user
	 *                                 input. Spaces will be removed from the user input.
	 * @param getSensitiveInputHandler The callback used for accepting sensitive user input.
	 * @throws IllegalArgumentException If either callback parameter is null.
	 * @since 0.3.0
	 */
	public PamInteractiveAuthPlugin(boolean requireSecureConnection, Function<String, String> getInputHandler, Function<String, String> getSensitiveInputHandler) {
		if (null == getInputHandler) {
			throw new IllegalArgumentException("getInputHandler callback is null");
		}

		if (null == getSensitiveInputHandler) {
			throw new IllegalArgumentException("getSensitiveInputHandler callback is null");
		}

		this.requireSecureConnection = requireSecureConnection;
		this.getInputHandler = getInputHandler;
		this.getSensitiveInputHandler = getSensitiveInputHandler;

		addOperation(AUTH_CLIENT_AUTH_REQUEST, this::clientRequest);
		addOperation(AUTH_CLIENT_AUTH_RESPONSE, this::clientResponse);
		addOperation(PERFORM_RUNNING, this::stepGeneric); // C++: step_client_running
		addOperation(PERFORM_READY, this::stepGeneric); // C++: step_client_ready
		addOperation(PERFORM_NEXT, this::stepClientNext);
		addOperation(PERFORM_RESPONSE, this::stepGeneric); // C++: step_client_response
		addOperation(PERFORM_WAITING, this::stepWaiting);
		addOperation(PERFORM_WAITING_PW, this::stepWaitingPw);
		addOperation(PERFORM_ERROR, this::stepError);
		addOperation(PERFORM_TIMEOUT, this::stepTimeout);
		addOperation(PERFORM_AUTHENTICATED, this::stepAuthenticated);
		addOperation(PERFORM_NOT_AUTHENTICATED, this::stepNotAuthenticated);
		addOperation(PERFORM_NATIVE_AUTH, this::performNativeAuth);
	}

	public String getName() {
		return "pam_interactive";
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_REQUEST);

		// Initialize the persistence state of the PAM stack.
		resp.put("pdirty", false);
		resp.set("pstate", JsonUtil.getJsonMapper().createObjectNode());

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put("user_name", comm.proxyUsername);
		resp.put("zone_name", comm.proxyUserZone);

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
		} else if (!comm.secure) {
			log.warn("Using insecure channel for authentication. Password will be visible on the network.");
		}

		ObjectNode resp = (ObjectNode) request(comm, req);
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_RESPONSE);
		return resp;
	}

	private JsonNode clientResponse(RcComm rcComm, JsonNode context) throws IRODSException, IOException {
		if (!context.has("user_name")) {
			throw new IllegalStateException("Missing property: user_name");
		}

		if (!context.has("zone_name")) {
			throw new IllegalStateException("Missing property: zone_name");
		}

		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		return request(rcComm, resp);
	}

	private JsonNode stepClientNext(RcComm rcComm, JsonNode context) throws IRODSException, IOException, JsonPatchException {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		JsonNode prompt = resp.get("msg").get("prompt");
		if (prompt.isTextual()) {
			System.out.println(prompt.asText());
		}
		patchState(resp);
		resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		return request(rcComm, resp);
	}

	private JsonNode stepWaiting(RcComm rcComm, JsonNode context) throws Exception {
		ObjectNode resp = (ObjectNode) context.deepCopy();

		if (retrieveEntry(resp)) {
			resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
			patchState(resp);
			return request(rcComm, resp);
		}

		String prompt = getStringValue(resp.get("msg"), "prompt", "");
		String defaultValue = getDefaultValue(resp);

		String input = getInputHandler.apply(prompt);
		input = input.replaceAll(" ", "");
		if (input.isEmpty()) {
			resp.put("resp", defaultValue);
		} else {
			resp.put("resp", input);
		}

		resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		patchState(resp);
		return request(rcComm, resp);
	}

	private JsonNode stepWaitingPw(RcComm rcComm, JsonNode context) throws Exception {
		ObjectNode resp = (ObjectNode) context.deepCopy();

		if (retrieveEntry(resp)) {
			resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
			patchState(resp);
			return request(rcComm, resp);
		}

		String prompt = getStringValue(resp.get("msg"), "prompt", "");
		String defaultValue = getDefaultValue(resp);

		String pw = getSensitiveInputHandler.apply(prompt);
		if (pw.isEmpty()) {
			resp.put("resp", defaultValue);
		} else {
			resp.put("resp", pw);
		}

		patchState(resp);
		resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		return request(rcComm, resp);
	}

	private JsonNode stepError(RcComm rcComm, JsonNode context) throws IRODSException, IOException {
		System.out.println("error");
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);
		rcComm.loggedIn = false;
		return resp;
	}

	private JsonNode stepTimeout(RcComm rcComm, JsonNode context) throws IRODSException, IOException {
		System.out.println("timeout");
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);
		rcComm.loggedIn = false;
		return resp;
	}

	private JsonNode stepAuthenticated(RcComm rcComm, JsonNode context) throws IRODSException, IOException {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, PERFORM_NATIVE_AUTH);
		return resp;
	}

	private JsonNode stepNotAuthenticated(RcComm rcComm, JsonNode context) throws IRODSException, IOException {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);
		rcComm.loggedIn = false;
		return resp;
	}

	private JsonNode stepGeneric(RcComm rcComm, JsonNode context) throws IRODSException, IOException, JsonPatchException {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		patchState(resp);
		resp.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		return request(rcComm, resp);
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

		// If everything completed successfully, the flow is complete, and we can
		// consider the user to be logged in. The native auth flow was run, and so
		// we trust the result.
		resp.put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);

		// The native auth plugin sets this on success, so this isn't necessary.
		// We'll set it anyway to align with the C++ implementation, just to be on
		// the safe side.
		comm.loggedIn = true;

		return resp;
	}

	private static void patchState(JsonNode state) throws IOException, JsonPatchException {
		if (!state.has("patch")) {
			return;
		}

		ObjectNode patch = state.get("patch").deepCopy();
		patch.forEach(node -> {
			JsonNode op = node.get("op");
			String opStr = null != op ? op.asText("") : "";
			if ("add".equals(opStr) || "replace".equals(opStr)) {
				if (!node.has("value")) {
					String value = getStringValue(state, "resp", "");
					((ObjectNode) node).put("value", value);
				}
			}
		});

		ObjectNode node = (ObjectNode) state;
		// JsonPatch is LGPL 3.0 and ASL 2.0. See the NOTICE file in
		// the root of the repository for additional information.
		JsonPatch jsonPatch = JsonPatch.fromJson((JsonNode) patch);
		node.set("pstate", jsonPatch.apply(node.get("pstate")));
		node.put("pdirty", true);
		((ObjectNode) node.get("msg")).remove("patch");
	}

	private static String getDefaultValue(JsonNode request) {
		String defaultPath = getStringValue(request.get("msg"), "default_path", "");
		if (!defaultPath.isEmpty()) {
			JsonNode pstate = request.get("pstate");
			if (null != pstate) {
				return pstate.at(defaultPath).asText("");
			}
		}
		return "";
	}

	private static boolean retrieveEntry(JsonNode request) {
		JsonNode msg = request.get("msg");
		if (null != msg && msg.has("retrieve")) {
			JsonNode retrPath = msg.get("retrieve");
			if (null != retrPath) {
				JsonNode pstate = request.get("pstate").at(retrPath.asText());
				if (!pstate.isMissingNode()) {
					((ObjectNode) request).put("resp", pstate.asText());
					return true;
				}
			}

			((ObjectNode) request).put("resp", "");
			return true;
		}

		return false;
	}

	private static String getStringValue(JsonNode node, String key, String defaultValue) {
		if (null == node) {
			return defaultValue;
		}
		JsonNode tmp = node.get(key);
		return (null != tmp) ? tmp.asText(defaultValue) : defaultValue;
	}

	private static String getInputFromClientStdin(String prompt) {
		Scanner scanner = new Scanner(System.in);
		return scanner.nextLine();
	}

	private static String getPasswordFromClientStdin(String prompt) {
		Console console = System.console();
		if (null == console) {
			throw new IllegalStateException("No console available. Cannot disable echo.");
		}

		char[] passwdChars = console.readPassword();
		String passwd = new String(passwdChars);
		// Clear sensitive data after use.
		Arrays.fill(passwdChars, ' ');

		System.out.println();

		return passwd;
	}

}
