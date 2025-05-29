package org.irods.irods4j.high_level.administration;

import java.io.IOException;
import java.util.List;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Provides utilities for common miscellaneous operations.
 *
 * @since 0.1.0
 */
class Common {

	/**
	 * Retrieves the zone name which the connected server belongs to.
	 *
	 * @param comm The connection to the iRODS server.
	 * @return The zone name which the connected server belongs to.
	 * @throws IOException
	 * @throws IRODSException
	 *
	 * @since 0.1.0
	 */
	static String getLocalZone(RcComm comm) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		var input = new Genquery2Input_PI();
		input.query_string = "select ZONE_NAME where ZONE_TYPE = 'local'";

		var output = new Reference<String>();
		var ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		var typeRef = new TypeReference<List<List<String>>>() {
		};
		var rows = JsonUtil.fromJsonString(output.value, typeRef);
		return rows.get(0).get(0);
	}

}
