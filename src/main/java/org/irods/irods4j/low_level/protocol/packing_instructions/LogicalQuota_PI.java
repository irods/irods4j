package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("logicalQuota_PI")
public class LogicalQuota_PI {

	public String collName;
	public long maxBytes;
	public long maxObjects;
	public long overBytes;
	public long overObjects;
	public String modify_time;

}
