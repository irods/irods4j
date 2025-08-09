package org.irods.irods4j.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthManager {
	
	private static final Logger log = LogManager.getLogger();

	public static void authenticateClient(RcComm comm, AuthPlugin authPlugin, JsonNode context) throws Exception {
		log.trace(">>> AUTHENTICATION START <<<");

		if (null == authPlugin) {
			throw new IllegalArgumentException("Authentication plugin is null");
		}

		var nextOp = AuthPlugin.AUTH_CLIENT_START;

		var req = context.deepCopy();
		var mutableReq = (ObjectNode) req;
		mutableReq.put("scheme", authPlugin.getName());
		mutableReq.put("next_operation", nextOp);
		
		log.debug("Initial context = {}", JsonUtil.toJsonString(req));

		while (true) {
			JsonNode resp = authPlugin.execute(comm, nextOp, req);
			log.debug("Server response = {}", JsonUtil.toJsonString(resp));

			if (comm.loggedIn) {
				break;
			}

			if (!resp.has("next_operation")) {
				throw new IRODSException(IRODSErrorCodes.SYS_INVALID_INPUT_PARAM, "Authentication request missing [next_operation] parameter");
			}

			nextOp = resp.get("next_operation").asText();
			if (nextOp.isEmpty() || "flow_complete".equals(nextOp)) {
				throw new IRODSException(IRODSErrorCodes.CAT_INVALID_AUTHENTICATION, "Authentication flow completed without success");
			}

			req = resp;
		}

		log.trace(">>> AUTHENTICATION COMPLETE <<<");
	}

}
