package org.irods.irods4j.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Obfuscation;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.irods.irods4j.low_level.protocol.packing_instructions.GetLimitedPasswordInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GetLimitedPasswordOut_PI;

public class NativeAuthPlugin extends AuthPlugin {

	private static final Logger log = LogManager.getLogger();

	// This operation is unique to irods4j. It operates in the same space as the
	// C++ implementation's "record_authentication_file" operation. The primary
	// difference is that irods4j does not define HOW the limited password is
	// handled. It only writes the token to a location in memory provided by the
	// caller.
	private static final String STORE_TIME_LIMITED_PASSWORD = "store_time_limited_password";
	private static final String COMPLETE_AUTHENTICATION = "complete_authentication";

	private Reference<String> timeLimitedPassword;

	/**
	 * Constructs a new native plugin instance with support for storing
	 * time-limited passwords.
	 *
	 * @param timeLimitedPassword The reference object that will contain the
	 *                            time-limited password on success. The value
	 *                            MUST NOT be used if authentication fails.
	 * @since 0.1.0
	 */
	public NativeAuthPlugin(Reference<String> timeLimitedPassword) {
		if (null == timeLimitedPassword) {
			throw new IllegalArgumentException("timeLimitedPassword is null");
		}

		this.timeLimitedPassword = timeLimitedPassword;

		addOperation(AUTH_ESTABLISH_CONTEXT, this::establishContext);
		addOperation(AUTH_CLIENT_AUTH_REQUEST, this::clientRequest);
		addOperation(AUTH_CLIENT_AUTH_RESPONSE, this::clientResponse);
		addOperation(STORE_TIME_LIMITED_PASSWORD, this::storeTimeLimitedPassword);
		addOperation(COMPLETE_AUTHENTICATION, this::completeAuthentication);
	}

	/**
	 * Constructs a new native plugin instance.
	 *
	 * @since 0.1.0
	 */
	public NativeAuthPlugin() {
		this(new Reference<>());
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
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(String password, long ttl) {
		var input = new HashMap<String, Object>();
		input.put(AUTH_PASSWORD_KEY, password);
		input.put(AUTH_TTL_KEY, String.valueOf(ttl));
		return input;
	}

	/**
	 * @see #initAuthArguments(String, long)
	 * @since 0.8.0
	 */
	public static Map<String, Object> initAuthArguments(String password) {
		var input = new HashMap<String, Object>();
		input.put(AUTH_PASSWORD_KEY, password);
		return input;
	}

	@Override
	public JsonNode authClientStart(RcComm comm, JsonNode context) {
		var resp = (ObjectNode) context.deepCopy();
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_REQUEST);

		// The proxy user's information is used for authentication because it covers
		// all cases for connecting to iRODS (i.e. proxied and non-proxied connections).
		resp.put("user_name", comm.proxyUsername);
		resp.put("zone_name", comm.proxyUserZone);

		return resp;
	}

	private JsonNode establishContext(RcComm comm, JsonNode context) throws NoSuchAlgorithmException {
		if (!context.has("user_name") || !context.has("request_result")) {
			throw new IllegalStateException("Missing user_name and/or request_result");
		}

		var resp = (ObjectNode) context.deepCopy();

		var requestResult = context.get("request_result").asText();
		var requestResultSb = new StringBuilder();
		requestResultSb.append(requestResult);

		// Equivalent to std::string::resize.
		// Pads the end of the content with \u0000.
		final var CHALLENGE_LEN = 64;
		requestResultSb.setLength(CHALLENGE_LEN);
		log.debug("requestResultSb string = [{}]", requestResultSb.toString());

		var md5BufSb = new StringBuilder();
		// [64+1] is the challenge string received by the server.
		// The "+1" is space for the null byte.
		md5BufSb.append(requestResultSb);

		// Compute the session signature and store it in the RcComm.
		// See plugins/authentication/src/native.cpp for details
		// (i.e. native_auth_establish_context).
		comm.sessionSignature = generateSessionSignature(md5BufSb.substring(0, 16));
		log.debug("Session signature = [{}]", comm.sessionSignature);

		// If the anonymous user account is being used, ignore the password.
		// See plugins/authentication/src/native.cpp for details.
		if (!"anonymous".equals(context.get("user_name").asText())) {
			final var MAX_PASSWORD_LEN = 50;
			var passwordSb = new StringBuilder();
			// TODO Enforce the max password length. We need to open an issue for this.
			// The user could enter a long password which gets truncated!
			passwordSb.append(context.get(AUTH_PASSWORD_KEY).asText());
			passwordSb.setLength(MAX_PASSWORD_LEN);

			// [50+1] is the unobfuscated user's password.
			// The "+1" is space for the null byte.
			md5BufSb.append(passwordSb);
			log.debug("MD5 string = [{}]", md5BufSb.toString());
		}

		var hasher = MessageDigest.getInstance("md5");
		hasher.update(md5BufSb.toString().getBytes(StandardCharsets.UTF_8));

		// Make sure the byte sequence doesn't end early.
		// Scrub out any errant terminating characters by incrementing
		// their value by 1.
		final var RESPONSE_LEN = 16;
		var digest = hasher.digest();
		for (var i = 0; i < RESPONSE_LEN; ++i) {
			if (0 == digest[i]) {
				++digest[i];
			}
		}

		resp.put("digest", Base64.getEncoder().encodeToString(digest));
		resp.put(AUTH_NEXT_OPERATION, AUTH_CLIENT_AUTH_RESPONSE);

		return resp;
	}

	private JsonNode clientRequest(RcComm comm, JsonNode context) throws IOException, IRODSException {
		var req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_REQUEST);

		// Do not transmit the password in the request to the server if it was supplied.
		// This plugin does not require secure communication, so we cannot transmit the
		// password in the clear. Copy the password and remove it from the request. It
		// will be restored later.
		var passwordNode = req.remove(AUTH_PASSWORD_KEY);

		var resp = (ObjectNode) request(comm, req);

		// If a password was provided, restore it here.
		if (null != passwordNode) {
			resp.put(AUTH_PASSWORD_KEY, passwordNode.asText());
		}

		resp.put(AUTH_NEXT_OPERATION, AUTH_ESTABLISH_CONTEXT);

		return resp;
	}

	private JsonNode clientResponse(RcComm comm, JsonNode context) throws IOException, IRODSException {
		if (!context.has("digest") || !context.has("user_name") || !context.has("zone_name")) {
			throw new IllegalStateException("Missing digest, user_name, and/or zone_name");
		}

		var req = (ObjectNode) context.deepCopy();
		req.put(AUTH_NEXT_OPERATION, AUTH_AGENT_AUTH_RESPONSE);

		// Do not transmit the password in the request to the server if it was supplied.
		// This plugin does not require secure communication, so we cannot transmit the
		// password in the clear. Copy the password and remove it from the request. It
		// will be restored later.
		var passwordNode = req.remove(AUTH_PASSWORD_KEY);

		var resp = (ObjectNode) request(comm, req);

		// If a password was provided, restore it here.
		if (null != passwordNode) {
			resp.put(AUTH_PASSWORD_KEY, passwordNode.asText());
		}

		resp.put(AUTH_NEXT_OPERATION, STORE_TIME_LIMITED_PASSWORD);

		return resp;
	}

	private JsonNode storeTimeLimitedPassword(RcComm comm, JsonNode context) throws IOException, IRODSException {
		// Get the password from the request payload, if available. If it is not there,
		// this is not necessarily an error. In this case, just complete the authentication
		// flow without storing the time-limited password.

		if (!context.has(AUTH_PASSWORD_KEY)) {
			return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, COMPLETE_AUTHENTICATION);
		}

		var password = context.get(AUTH_PASSWORD_KEY).asText();
		if (password == null || password.isEmpty()) {
			return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, COMPLETE_AUTHENTICATION);
		}

		if (!context.has(AUTH_TTL_KEY)) {
			return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, COMPLETE_AUTHENTICATION);
		}

		var ttlString = context.get(AUTH_TTL_KEY).asText();
		int ttl = -1;
		try {
			ttl = Integer.parseInt(ttlString);
		} catch (NumberFormatException e) {
			throw new IRODSException(IRODSErrorCodes.SYS_INVALID_INPUT_PARAM, "Invalid TTL value");
		}

		// A TTL of 0 means it is not in use.
		if (0 == ttl) {
			return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, COMPLETE_AUTHENTICATION);
		}

		try {
			timeLimitedPassword.value = generateTimeLimitedPassword(comm, ttl, password);
			log.info("TIME LIMITED PASSWORD = [{}]", timeLimitedPassword.value);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		return JsonUtil.getJsonMapper().createObjectNode()
				.put("scheme", getName())
				.put("next_operation", AUTH_CLIENT_START)
				.put(AUTH_PASSWORD_KEY, timeLimitedPassword.value);
	}

	private JsonNode completeAuthentication(RcComm comm, JsonNode context) throws IOException, IRODSException {
		comm.loggedIn = true;
		return JsonUtil.getJsonMapper().createObjectNode().put(AUTH_NEXT_OPERATION, AUTH_FLOW_COMPLETE);
	}

	private static String generateTimeLimitedPassword(RcComm comm, int ttl, String password) throws IOException,
			IRODSException, NoSuchAlgorithmException {
		var input = new GetLimitedPasswordInp_PI();
		input.ttl = ttl;
		var outputRef = new Reference<GetLimitedPasswordOut_PI>();
		var ec = IRODSApi.rcGetLimitedPassword(comm, input, outputRef);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGetLimitedPassword error");
		}

		// Calculate the limited password, which is a hash of the user's password and
		// the returned "stringToHashWith" string.
		final var hashLength = 100;
		var hashBuffer = new StringBuilder();
		hashBuffer.append(outputRef.value.stringToHashWith);
		hashBuffer.append(password);
		hashBuffer.setLength(hashLength);
		var hashBufferBytes = hashBuffer.toString().getBytes(StandardCharsets.UTF_8);

		// Convert the byte array to an integer array.
		var buffer = new int[hashLength];
		var length = Math.min(buffer.length, hashBufferBytes.length);
		for (int i = 0; i < length; ++i) {
			buffer[i] = hashBufferBytes[i] & 0xff;
		}

		var digest = Obfuscation.obfMakeOneWayHash(comm.hashAlgorithm, buffer, buffer.length);
		Arrays.fill(hashBufferBytes, (byte) 0);

		var sb = new StringBuilder();
		for (int i = 0; i < 16; ++i) {
			sb.append(String.format("%02x", digest[i]));
		}
		return sb.toString();
	}

	private static String generateSessionSignature(String buffer) {
		final var requiredSize = 16;
		final var byteBuffer = buffer.getBytes(StandardCharsets.UTF_8);

		if (byteBuffer.length < requiredSize) {
			throw new IllegalArgumentException("Buffer must be at least 16 bytes long");
		}

		var sigSb = new StringBuilder();
		for (var ch : byteBuffer) {
			sigSb.append(String.format("%02x", ch));
		}

		var sig = sigSb.toString();
		if (sig.getBytes(StandardCharsets.UTF_8).length != (2 * requiredSize)) {
			throw new IllegalStateException("Session signature is not 32 bytes in length");
		}

		return sigSb.toString();
	}

}
