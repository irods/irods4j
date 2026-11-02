package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("getLimitedPasswordInp_PI")
public class GetLimitedPasswordInp_PI {

	public int ttl;

	// Currently unused, but available without protocol change if needed.
	public String unused1;

}
