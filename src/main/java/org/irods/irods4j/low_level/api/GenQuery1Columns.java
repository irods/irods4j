package org.irods.irods4j.low_level.api;

public class GenQuery1Columns {

	/* R_ZONE_MAIN: */
	public static final int COL_ZONE_ID = 101;
	public static final int COL_ZONE_NAME = 102;
	public static final int COL_ZONE_TYPE = 103;
	public static final int COL_ZONE_CONNECTION = 104;
	public static final int COL_ZONE_COMMENT = 105;
	public static final int COL_ZONE_CREATE_TIME = 106;
	public static final int COL_ZONE_MODIFY_TIME = 107;

	/* R_USER_MAIN: */
	public static final int COL_USER_ID = 201;
	public static final int COL_USER_NAME = 202;
	public static final int COL_USER_TYPE = 203;
	public static final int COL_USER_ZONE = 204;
	public static final int COL_USER_INFO = 206;
	public static final int COL_USER_COMMENT = 207;
	public static final int COL_USER_CREATE_TIME = 208;
	public static final int COL_USER_MODIFY_TIME = 209;

	public static final int COL_USER_DN_INVALID = 205; /* For backward compatibility, irods 2.1 DN */

	/* R_RESC_MAIN: */
	public static final int COL_R_RESC_ID = 301;
	public static final int COL_R_RESC_NAME = 302;
	public static final int COL_R_ZONE_NAME = 303;
	public static final int COL_R_TYPE_NAME = 304;
	public static final int COL_R_CLASS_NAME = 305;
	public static final int COL_R_LOC = 306;
	public static final int COL_R_VAULT_PATH = 307;
	public static final int COL_R_FREE_SPACE = 308;
	public static final int COL_R_RESC_INFO = 309;
	public static final int COL_R_RESC_COMMENT = 310;
	public static final int COL_R_CREATE_TIME = 311;
	public static final int COL_R_MODIFY_TIME = 312;
	public static final int COL_R_RESC_STATUS = 313;
	public static final int COL_R_FREE_SPACE_TIME = 314;
	public static final int COL_R_RESC_CHILDREN = 315;
	public static final int COL_R_RESC_CONTEXT = 316;
	public static final int COL_R_RESC_PARENT = 317;
	public static final int COL_R_RESC_PARENT_CONTEXT = 318;
	public static final int COL_R_MODIFY_TIME_MILLIS = 319;

	/* R_DATA_MAIN: */
	public static final int COL_D_DATA_ID = 401;
	public static final int COL_D_COLL_ID = 402;
	public static final int COL_DATA_NAME = 403;
	public static final int COL_DATA_REPL_NUM = 404;
	public static final int COL_DATA_VERSION = 405;
	public static final int COL_DATA_TYPE_NAME = 406;
	public static final int COL_DATA_SIZE = 407;
	// public static final int COL_D_RESC_GROUP_NAME = 408 // gone in 4.1 #1472;
	public static final int COL_D_RESC_NAME = 409;
	public static final int COL_D_DATA_PATH = 410;
	public static final int COL_D_OWNER_NAME = 411;
	public static final int COL_D_OWNER_ZONE = 412;
	public static final int COL_D_REPL_STATUS = 413; /* isDirty */
	public static final int COL_D_DATA_STATUS = 414;
	public static final int COL_D_DATA_CHECKSUM = 415;
	public static final int COL_D_EXPIRY = 416;
	public static final int COL_D_MAP_ID = 417;
	public static final int COL_D_COMMENTS = 418;
	public static final int COL_D_CREATE_TIME = 419;
	public static final int COL_D_MODIFY_TIME = 420;
	public static final int COL_DATA_MODE = 421;
	public static final int COL_D_RESC_HIER = 422;
	public static final int COL_D_RESC_ID = 423;
	public static final int COL_D_ACCESS_TIME = 424;

	/* R_COLL_MAIN */
	public static final int COL_COLL_ID = 500;
	public static final int COL_COLL_NAME = 501;
	public static final int COL_COLL_PARENT_NAME = 502;
	public static final int COL_COLL_OWNER_NAME = 503;
	public static final int COL_COLL_OWNER_ZONE = 504;
	public static final int COL_COLL_MAP_ID = 505;
	public static final int COL_COLL_INHERITANCE = 506;
	public static final int COL_COLL_COMMENTS = 507;
	public static final int COL_COLL_CREATE_TIME = 508;
	public static final int COL_COLL_MODIFY_TIME = 509;
	public static final int COL_COLL_TYPE = 510;
	public static final int COL_COLL_INFO1 = 511;
	public static final int COL_COLL_INFO2 = 512;

	/* R_META_MAIN */
	public static final int COL_META_DATA_ATTR_NAME = 600;
	public static final int COL_META_DATA_ATTR_VALUE = 601;
	public static final int COL_META_DATA_ATTR_UNITS = 602;
	public static final int COL_META_DATA_ATTR_ID = 603;
	public static final int COL_META_DATA_CREATE_TIME = 604;
	public static final int COL_META_DATA_MODIFY_TIME = 605;

	public static final int COL_META_COLL_ATTR_NAME = 610;
	public static final int COL_META_COLL_ATTR_VALUE = 611;
	public static final int COL_META_COLL_ATTR_UNITS = 612;
	public static final int COL_META_COLL_ATTR_ID = 613;
	public static final int COL_META_COLL_CREATE_TIME = 614;
	public static final int COL_META_COLL_MODIFY_TIME = 615;

	public static final int COL_META_NAMESPACE_COLL = 620;
	public static final int COL_META_NAMESPACE_DATA = 621;
	public static final int COL_META_NAMESPACE_RESC = 622;
	public static final int COL_META_NAMESPACE_USER = 623;
	public static final int COL_META_NAMESPACE_RESC_GROUP = 624;
	public static final int COL_META_NAMESPACE_RULE = 625;
	public static final int COL_META_NAMESPACE_MSRVC = 626;
	public static final int COL_META_NAMESPACE_MET2 = 627;

	public static final int COL_META_RESC_ATTR_NAME = 630;
	public static final int COL_META_RESC_ATTR_VALUE = 631;
	public static final int COL_META_RESC_ATTR_UNITS = 632;
	public static final int COL_META_RESC_ATTR_ID = 633;
	public static final int COL_META_RESC_CREATE_TIME = 634;
	public static final int COL_META_RESC_MODIFY_TIME = 635;

	public static final int COL_META_USER_ATTR_NAME = 640;
	public static final int COL_META_USER_ATTR_VALUE = 641;
	public static final int COL_META_USER_ATTR_UNITS = 642;
	public static final int COL_META_USER_ATTR_ID = 643;
	public static final int COL_META_USER_CREATE_TIME = 644;
	public static final int COL_META_USER_MODIFY_TIME = 645;

	public static final int COL_META_RESC_GROUP_ATTR_NAME = 650;
	public static final int COL_META_RESC_GROUP_ATTR_VALUE = 651;
	public static final int COL_META_RESC_GROUP_ATTR_UNITS = 652;
	public static final int COL_META_RESC_GROUP_ATTR_ID = 653;
	public static final int COL_META_RESC_GROUP_CREATE_TIME = 654;
	public static final int COL_META_RESC_GROUP_MODIFY_TIME = 655;

	public static final int COL_META_RULE_ATTR_NAME = 660;
	public static final int COL_META_RULE_ATTR_VALUE = 661;
	public static final int COL_META_RULE_ATTR_UNITS = 662;
	public static final int COL_META_RULE_ATTR_ID = 663;
	public static final int COL_META_RULE_CREATE_TIME = 664;
	public static final int COL_META_RULE_MODIFY_TIME = 665;

	public static final int COL_META_MSRVC_ATTR_NAME = 670;
	public static final int COL_META_MSRVC_ATTR_VALUE = 671;
	public static final int COL_META_MSRVC_ATTR_UNITS = 672;
	public static final int COL_META_MSRVC_ATTR_ID = 673;
	public static final int COL_META_MSRVC_CREATE_TIME = 674;
	public static final int COL_META_MSRVC_MODIFY_TIME = 675;

	public static final int COL_META_MET2_ATTR_NAME = 680;
	public static final int COL_META_MET2_ATTR_VALUE = 681;
	public static final int COL_META_MET2_ATTR_UNITS = 682;
	public static final int COL_META_MET2_ATTR_ID = 683;
	public static final int COL_META_MET2_CREATE_TIME = 684;
	public static final int COL_META_MET2_MODIFY_TIME = 685;

	/* R_OBJT_ACCESS */
	public static final int COL_DATA_ACCESS_TYPE = 700;
	public static final int COL_DATA_ACCESS_NAME = 701;
	public static final int COL_DATA_TOKEN_NAMESPACE = 702;
	public static final int COL_DATA_ACCESS_USER_ID = 703;
	public static final int COL_DATA_ACCESS_DATA_ID = 704;

	public static final int COL_COLL_ACCESS_TYPE = 710;
	public static final int COL_COLL_ACCESS_NAME = 711;
	public static final int COL_COLL_TOKEN_NAMESPACE = 712;
	public static final int COL_COLL_ACCESS_USER_ID = 713;
	public static final int COL_COLL_ACCESS_COLL_ID = 714;

	public static final int COL_RESC_ACCESS_TYPE = 720;
	public static final int COL_RESC_ACCESS_NAME = 721;
	public static final int COL_RESC_TOKEN_NAMESPACE = 722;
	public static final int COL_RESC_ACCESS_USER_ID = 723;
	public static final int COL_RESC_ACCESS_RESC_ID = 724;

	public static final int COL_META_ACCESS_TYPE = 730;
	public static final int COL_META_ACCESS_NAME = 731;
	public static final int COL_META_TOKEN_NAMESPACE = 732;
	public static final int COL_META_ACCESS_USER_ID = 733;
	public static final int COL_META_ACCESS_META_ID = 734;

	public static final int COL_RULE_ACCESS_TYPE = 740;
	public static final int COL_RULE_ACCESS_NAME = 741;
	public static final int COL_RULE_TOKEN_NAMESPACE = 742;
	public static final int COL_RULE_ACCESS_USER_ID = 743;
	public static final int COL_RULE_ACCESS_RULE_ID = 744;

	public static final int COL_MSRVC_ACCESS_TYPE = 750;
	public static final int COL_MSRVC_ACCESS_NAME = 751;
	public static final int COL_MSRVC_TOKEN_NAMESPACE = 752;
	public static final int COL_MSRVC_ACCESS_USER_ID = 753;
	public static final int COL_MSRVC_ACCESS_MSRVC_ID = 754;

	/* R_RESC_GROUP */
	// public static final int COL_RESC_GROUP_RESC_ID = 800 // gone in 4.1 #1472;
	// public static final int COL_RESC_GROUP_NAME = 801;
	// public static final int COL_RESC_GROUP_ID = 802;

	/* R_USER_GROUP / USER */
	public static final int COL_USER_GROUP_ID = 900;
	public static final int COL_USER_GROUP_NAME = 901;

	/* R_RULE_EXEC */
	public static final int COL_RULE_EXEC_ID = 1000;
	public static final int COL_RULE_EXEC_NAME = 1001;
	public static final int COL_RULE_EXEC_REI_FILE_PATH = 1002;
	public static final int COL_RULE_EXEC_USER_NAME = 1003;
	public static final int COL_RULE_EXEC_ADDRESS = 1004;
	public static final int COL_RULE_EXEC_TIME = 1005;
	public static final int COL_RULE_EXEC_FREQUENCY = 1006;
	public static final int COL_RULE_EXEC_PRIORITY = 1007;
	public static final int COL_RULE_EXEC_ESTIMATED_EXE_TIME = 1008;
	public static final int COL_RULE_EXEC_NOTIFICATION_ADDR = 1009;
	public static final int COL_RULE_EXEC_LAST_EXE_TIME = 1010;
	public static final int COL_RULE_EXEC_STATUS = 1011;
	public static final int COL_RULE_EXEC_CONTEXT = 1012;
	public static final int COL_RULE_EXEC_LOCK_HOST = 1013;
	public static final int COL_RULE_EXEC_LOCK_HOST_PID = 1014;
	public static final int COL_RULE_EXEC_LOCK_TIME = 1015;

	/* R_TOKN_MAIN */
	public static final int COL_TOKEN_NAMESPACE = 1100;
	public static final int COL_TOKEN_ID = 1101;
	public static final int COL_TOKEN_NAME = 1102;
	public static final int COL_TOKEN_VALUE = 1103;
	public static final int COL_TOKEN_VALUE2 = 1104;
	public static final int COL_TOKEN_VALUE3 = 1105;
	public static final int COL_TOKEN_COMMENT = 1106;
	public static final int COL_TOKEN_CREATE_TIME = 1107;
	public static final int COL_TOKEN_MODIFY_TIME = 1108;

	/* R_OBJT_AUDIT */
	public static final int COL_AUDIT_OBJ_ID = 1200;
	public static final int COL_AUDIT_USER_ID = 1201;
	public static final int COL_AUDIT_ACTION_ID = 1202;
	public static final int COL_AUDIT_COMMENT = 1203;
	public static final int COL_AUDIT_CREATE_TIME = 1204;
	public static final int COL_AUDIT_MODIFY_TIME = 1205;

	/* Range of the Audit columns; used sometimes to restrict access */
	public static final int COL_AUDIT_RANGE_START = 1200;
	public static final int COL_AUDIT_RANGE_END = 1299;

	/* R_COLL_USER_MAIN (r_user_main for Collection information) */
	public static final int COL_COLL_USER_NAME = 1300;
	public static final int COL_COLL_USER_ZONE = 1301;

	/* R_DATA_USER_MAIN (r_user_main for Data information specifically) */
	public static final int COL_DATA_USER_NAME = 1310;
	public static final int COL_DATA_USER_ZONE = 1311;

	/* R_DATA_USER_MAIN (r_user_main for Data information specifically) */
	public static final int COL_RESC_USER_NAME = 1320;
	public static final int COL_RESC_USER_ZONE = 1321;

	/* R_SERVER_LOAD */
	public static final int COL_SL_HOST_NAME = 1400;
	public static final int COL_SL_RESC_NAME = 1401;
	public static final int COL_SL_CPU_USED = 1402;
	public static final int COL_SL_MEM_USED = 1403;
	public static final int COL_SL_SWAP_USED = 1404;
	public static final int COL_SL_RUNQ_LOAD = 1405;
	public static final int COL_SL_DISK_SPACE = 1406;
	public static final int COL_SL_NET_INPUT = 1407;
	public static final int COL_SL_NET_OUTPUT = 1408;
	public static final int COL_SL_CREATE_TIME = 1409;

	/* R_SERVER_LOAD_DIGEST */
	public static final int COL_SLD_RESC_NAME = 1500;
	public static final int COL_SLD_LOAD_FACTOR = 1501;
	public static final int COL_SLD_CREATE_TIME = 1502;

	/* R_USER_AUTH (for GSI/KRB) */
	public static final int COL_USER_AUTH_ID = 1600;
	public static final int COL_USER_DN = 1601;

	/* R_RULE_MAIN */
	public static final int COL_RULE_ID = 1700;
	public static final int COL_RULE_VERSION = 1701;
	public static final int COL_RULE_BASE_NAME = 1702;
	public static final int COL_RULE_NAME = 1703;
	public static final int COL_RULE_EVENT = 1704;
	public static final int COL_RULE_CONDITION = 1705;
	public static final int COL_RULE_BODY = 1706;
	public static final int COL_RULE_RECOVERY = 1707;
	public static final int COL_RULE_STATUS = 1708;
	public static final int COL_RULE_OWNER_NAME = 1709;
	public static final int COL_RULE_OWNER_ZONE = 1710;
	public static final int COL_RULE_DESCR_1 = 1711;
	public static final int COL_RULE_DESCR_2 = 1712;
	public static final int COL_RULE_INPUT_PARAMS = 1713;
	public static final int COL_RULE_OUTPUT_PARAMS = 1714;
	public static final int COL_RULE_DOLLAR_VARS = 1715;
	public static final int COL_RULE_ICAT_ELEMENTS = 1716;
	public static final int COL_RULE_SIDEEFFECTS = 1717;
	public static final int COL_RULE_COMMENT = 1718;
	public static final int COL_RULE_CREATE_TIME = 1719;
	public static final int COL_RULE_MODIFY_TIME = 1720;

	/* R_RULE_BASE_MAP (for storing versions of the rules */
	public static final int COL_RULE_BASE_MAP_VERSION = 1721;
	public static final int COL_RULE_BASE_MAP_BASE_NAME = 1722;
	public static final int COL_RULE_BASE_MAP_OWNER_NAME = 1723;
	public static final int COL_RULE_BASE_MAP_OWNER_ZONE = 1724;
	public static final int COL_RULE_BASE_MAP_COMMENT = 1725;
	public static final int COL_RULE_BASE_MAP_CREATE_TIME = 1726;
	public static final int COL_RULE_BASE_MAP_MODIFY_TIME = 1727;
	public static final int COL_RULE_BASE_MAP_PRIORITY = 1728;

	/* R_RULE_DVM (Data Variable Mapping) */
	public static final int COL_DVM_ID = 1800;
	public static final int COL_DVM_VERSION = 1801;
	public static final int COL_DVM_BASE_NAME = 1802;
	public static final int COL_DVM_EXT_VAR_NAME = 1803;
	public static final int COL_DVM_CONDITION = 1804;
	public static final int COL_DVM_INT_MAP_PATH = 1805;
	public static final int COL_DVM_STATUS = 1806;
	public static final int COL_DVM_OWNER_NAME = 1807;
	public static final int COL_DVM_OWNER_ZONE = 1808;
	public static final int COL_DVM_COMMENT = 1809;
	public static final int COL_DVM_CREATE_TIME = 1810;
	public static final int COL_DVM_MODIFY_TIME = 1811;

	/* R_RULE_DVM_MAP (for storing versions of the rules */
	public static final int COL_DVM_BASE_MAP_VERSION = 1812;
	public static final int COL_DVM_BASE_MAP_BASE_NAME = 1813;
	public static final int COL_DVM_BASE_MAP_OWNER_NAME = 1814;
	public static final int COL_DVM_BASE_MAP_OWNER_ZONE = 1815;
	public static final int COL_DVM_BASE_MAP_COMMENT = 1816;
	public static final int COL_DVM_BASE_MAP_CREATE_TIME = 1817;
	public static final int COL_DVM_BASE_MAP_MODIFY_TIME = 1818;

	/* R_RULE_FNM (Function Name Mapping) */
	public static final int COL_FNM_ID = 1900;
	public static final int COL_FNM_VERSION = 1901;
	public static final int COL_FNM_BASE_NAME = 1902;
	public static final int COL_FNM_EXT_FUNC_NAME = 1903;
	public static final int COL_FNM_INT_FUNC_NAME = 1904;
	public static final int COL_FNM_STATUS = 1905;
	public static final int COL_FNM_OWNER_NAME = 1906;
	public static final int COL_FNM_OWNER_ZONE = 1907;
	public static final int COL_FNM_COMMENT = 1908;
	public static final int COL_FNM_CREATE_TIME = 1909;
	public static final int COL_FNM_MODIFY_TIME = 1910;

	/* R_RULE_FNM_MAP (for storing versions of the rules */
	public static final int COL_FNM_BASE_MAP_VERSION = 1911;
	public static final int COL_FNM_BASE_MAP_BASE_NAME = 1912;
	public static final int COL_FNM_BASE_MAP_OWNER_NAME = 1913;
	public static final int COL_FNM_BASE_MAP_OWNER_ZONE = 1914;
	public static final int COL_FNM_BASE_MAP_COMMENT = 1915;
	public static final int COL_FNM_BASE_MAP_CREATE_TIME = 1916;
	public static final int COL_FNM_BASE_MAP_MODIFY_TIME = 1917;

	/* R_QUOTA_MAIN */
	public static final int COL_QUOTA_USER_ID = 2000;
	public static final int COL_QUOTA_RESC_ID = 2001;
	public static final int COL_QUOTA_LIMIT = 2002;
	public static final int COL_QUOTA_OVER = 2003;
	public static final int COL_QUOTA_MODIFY_TIME = 2004;

	/* R_QUOTA_USAGE */
	public static final int COL_QUOTA_USAGE_USER_ID = 2010;
	public static final int COL_QUOTA_USAGE_RESC_ID = 2011;
	public static final int COL_QUOTA_USAGE = 2012;
	public static final int COL_QUOTA_USAGE_MODIFY_TIME = 2013;

	/* For use with quotas */
	public static final int COL_QUOTA_RESC_NAME = 2020;
	public static final int COL_QUOTA_USER_NAME = 2021;
	public static final int COL_QUOTA_USER_ZONE = 2022;
	public static final int COL_QUOTA_USER_TYPE = 2023;

	public static final int COL_MSRVC_ID = 2100;
	public static final int COL_MSRVC_NAME = 2101;
	public static final int COL_MSRVC_SIGNATURE = 2102;
	public static final int COL_MSRVC_DOXYGEN = 2103;
	public static final int COL_MSRVC_VARIATIONS = 2104;
	public static final int COL_MSRVC_STATUS = 2105;
	public static final int COL_MSRVC_OWNER_NAME = 2106;
	public static final int COL_MSRVC_OWNER_ZONE = 2107;
	public static final int COL_MSRVC_COMMENT = 2108;
	public static final int COL_MSRVC_CREATE_TIME = 2109;
	public static final int COL_MSRVC_MODIFY_TIME = 2110;
	public static final int COL_MSRVC_VERSION = 2111;
	public static final int COL_MSRVC_HOST = 2112;
	public static final int COL_MSRVC_LOCATION = 2113;
	public static final int COL_MSRVC_LANGUAGE = 2114;
	public static final int COL_MSRVC_TYPE_NAME = 2115;
	public static final int COL_MSRVC_MODULE_NAME = 2116;

	public static final int COL_MSRVC_VER_OWNER_NAME = 2150;
	public static final int COL_MSRVC_VER_OWNER_ZONE = 2151;
	public static final int COL_MSRVC_VER_COMMENT = 2152;
	public static final int COL_MSRVC_VER_CREATE_TIME = 2153;
	public static final int COL_MSRVC_VER_MODIFY_TIME = 2154;

	/* Tickets */
	public static final int COL_TICKET_ID = 2200;
	public static final int COL_TICKET_STRING = 2201;
	public static final int COL_TICKET_TYPE = 2202;
	public static final int COL_TICKET_USER_ID = 2203;
	public static final int COL_TICKET_OBJECT_ID = 2204;
	public static final int COL_TICKET_OBJECT_TYPE = 2205;
	public static final int COL_TICKET_USES_LIMIT = 2206;
	public static final int COL_TICKET_USES_COUNT = 2207;
	public static final int COL_TICKET_EXPIRY_TS = 2208;
	public static final int COL_TICKET_CREATE_TIME = 2209;
	public static final int COL_TICKET_MODIFY_TIME = 2210;
	public static final int COL_TICKET_WRITE_FILE_COUNT = 2211;
	public static final int COL_TICKET_WRITE_FILE_LIMIT = 2212;
	public static final int COL_TICKET_WRITE_BYTE_COUNT = 2213;
	public static final int COL_TICKET_WRITE_BYTE_LIMIT = 2214;

	public static final int COL_TICKET_ALLOWED_HOST_TICKET_ID = 2220;
	public static final int COL_TICKET_ALLOWED_HOST = 2221;
	public static final int COL_TICKET_ALLOWED_USER_TICKET_ID = 2222;
	public static final int COL_TICKET_ALLOWED_USER_NAME = 2223;
	public static final int COL_TICKET_ALLOWED_GROUP_TICKET_ID = 2224;
	public static final int COL_TICKET_ALLOWED_GROUP_NAME = 2225;

	public static final int COL_TICKET_DATA_NAME = 2226;
	public static final int COL_TICKET_DATA_COLL_NAME = 2227;
	public static final int COL_TICKET_COLL_NAME = 2228;

	public static final int COL_TICKET_OWNER_NAME = 2229;
	public static final int COL_TICKET_OWNER_ZONE = 2230;

}
