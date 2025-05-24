package org.irods.irods4j.low_level.protocol.packing_instructions;

public class DataObjInfo_PI {
	
	public String objPath;
	public String rescName;
	public String rescHier;
	public String dataType;
	public long dataSize; // This is a double in the C impl.
	public String chksum;
	public String version;
	public String filePath;
	public String dataOwnerName;
	public String dataOwnerZone;
	public int replNum;
	public int replStatus;
	public String statusString;
	public long dataId; // This is a double in the C impl.
	public long collId; // This is a double in the C impl.
	public int dataMapId;
	public int flags;
	public String dataComments;
	public String dataMode;
	public String dataExpiry;
	public String dataCreate;
	public String dataModify;
	public String dataAccess;
	public int dataAccessInx;
	public int writeFlag;
	public String destRescName;
	public String backupRescName;
	public String subPath;
	public int specColl; 
	public int regUid;
	public int otherFlags;
	public KeyValPair_PI KeyValPair_PI;
	public String in_pdmo;
	public int next;
	public long rescId; // This is a double in the C impl.
	public String dataAccessTime;

}
