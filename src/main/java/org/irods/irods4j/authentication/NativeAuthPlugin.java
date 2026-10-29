package org.irods.irods4j.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class NativeAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	public NativeAuthPlugin() {
		addOperation(AUTH_ESTABLISH_CONTEXT, this::establishContext);
		addOperation(AUTH_CLIENT_AUTH_REQUEST, this::clientRequest);
		addOperation(AUTH_CLIENT_AUTH_RESPONSE, this::clientResponse);
	}

	public String getName() {
		return "native";
	}

	/**
	 * Constructs a container holding the minimum set of inputs needed for
	 * password-based authentication via
	 * {@link org.irods.irods4j.low_level.api.IRODSApi#rcAuthenticateClient}.
	 *
	 * @param password The password of the proxy user or client user.
	 * @param ttl      The amount of time (in hours) before the session expires.
	 *                 Set to 0 to disable session expiration.
	 * @return The container holding the input arguments.
	 *
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(String password, long ttl) {
		Map<String, Object> input = new HashMap<>();
		input.put(AUTH_PASSWORD_KEY, password);
		input.put(AUTH_TTL_KEY, String.valueOf(ttl));
		return input;
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		ObjectNode resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_REQUEST);

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put("user_name", comm.proxyUsername);
		resp.put("zone_name", comm.proxyUserZone);

		return resp;
	}

	private JsonNode establishContext(RcComm comm, JsonNode context) throws NoSuchAlgorithmException {
		ObjectNode resp = (ObjectNode) context.deepCopy();

		String requestResult = context.get("request_result").asText();
		StringBuilder requestResultSb = new StringBuilder();
		requestResultSb.append(requestResult);

		// Equivalent to std::string::resize.
		// Pads the end of the content with \u0000.
		final int CHALLENGE_LEN = 64;
		requestResultSb.setLength(CHALLENGE_LEN);
		log.debug("requestResultSb string = [{}]", requestResultSb.toString());

		StringBuilder md5BufSb = new StringBuilder();
		// [64+1] is the challenge string received by the server.
		// The "+1" is space for the null byte.
		md5BufSb.append(requestResultSb);

		// Compute the session signature and store it in the RcComm.
		// See plugins/auth/src/native.cpp for details
		// (i.e. native_auth_establish_context.cpp).
		comm.sessionSignature = generateSessionSignature(md5BufSb.substring(0, 16));
		log.debug("Session signature = [{}]", comm.sessionSignature);

		// If the anonymous user account is being used, ignore the password.
		// See plugins/auth/src/native.cpp for details.
		if (!"anonymous".equals(context.get("user_name").asText())) {
			final int MAX_PASSWORD_LEN = 50;
			StringBuilder passwordSb = new StringBuilder();
			// TODO Enforce the max password length. We need to open an issue for this.
			// The user could enter a long password which gets truncated!
			passwordSb.append(context.get(AUTH_PASSWORD_KEY).asText());
			passwordSb.setLength(MAX_PASSWORD_LEN);

			// [50+1] is the unobfuscated user's password.
			// The "+1" is space for the null byte.
			md5BufSb.append(passwordSb);
			log.debug("MD5 string = [{}]", md5BufSb.toString());
		}

		MessageDigest hasher = MessageDigest.getInstance("md5");
		hasher.update(md5BufSb.toString().getBytes(StandardCharsets.UTF_8));

		// Make sure the byte sequence doesn't end early.
		// Scrub out any errant terminating characters by incrementing
		// their value by 1.
		final int RESPONSE_LEN = 16;
		byte[] digest = hasher.digest();
		for (int i = 0; i < RESPONSE_LEN; ++i) {
			if (0 == digest[i]) {
				++digest[i];
			}
		}

		// DO NOT leak the user's plaintext password!
		resp.remove(AUTH_PASSWORD_KEY);

		resp.put("digest", Base64.getEncoder().encodeToString(digest));
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_RESPONSE);

		return resp;
	}

	private JsonNode clientRequest(RcComm comm, JsonNode context) throws IOException, IRODSException {
		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_REQUEST);

		// Do not transmit the password in the request to the server if it was supplied.
		// This plugin does not require secure communication, so we cannot transmit the
		// password in the clear. Copy the password and remove it from the request. It
		// will be restored later.
		JsonNode passwordNode = req.remove(AUTH_PASSWORD_KEY);

		ObjectNode resp = (ObjectNode) request(comm, req);
		resp.put(AUTH_NEXT_OPERATION, AUTH_ESTABLISH_CONTEXT);

		// If a password was provided, restore it here.
		if (null != passwordNode) {
			resp.put(AUTH_PASSWORD_KEY, passwordNode.asText());
		}

		return resp;
	}

	private JsonNode clientResponse(RcComm comm, JsonNode context) throws IOException, IRODSException {
		if (!context.has("digest") || !context.has("user_name") || !context.has("zone_name")) {
			throw new IllegalStateException("Missing digest, user_name, and/or zone_name");
		}

		ObjectNode req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);
		JsonNode resp = request(comm, req);

		comm.loggedIn = true;
		((ObjectNode) resp).put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);

		return resp;
	}

	private static String generateSessionSignature(String buffer) {
		final int requiredSize = 16;
		final byte[] byteBuffer = buffer.getBytes(StandardCharsets.UTF_8);

		if (byteBuffer.length < requiredSize) {
			throw new IllegalArgumentException("Buffer must be at least 16 bytes long");
		}

		StringBuilder sigSb = new StringBuilder();
		for (byte ch : byteBuffer) {
			sigSb.append(String.format("%02x", ch));
		}

		String sig = sigSb.toString();
		if (sig.getBytes(StandardCharsets.UTF_8).length != (2 * requiredSize)) {
			throw new IllegalStateException("Session signature is not 32 bytes in length");
		}

		return sigSb.toString();
	}

}
