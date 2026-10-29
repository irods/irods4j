package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("getLogicalQuotaInp_PI")
public class GetLogicalQuotaInp_PI {

	public String collName;
	public KeyValPair_PI KeyValPair_PI;

}
