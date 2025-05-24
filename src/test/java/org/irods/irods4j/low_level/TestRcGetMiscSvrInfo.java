package org.irods.irods4j.low_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.Versioning;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.protocol.packing_instructions.MiscSvrInfo_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestRcGetMiscSvrInfo {

    static final Logger log = LogManager.getLogger();

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        XmlUtil.enablePrettyPrinting();
        JsonUtil.enablePrettyPrinting();
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        XmlUtil.disablePrettyPrinting();
        JsonUtil.disablePrettyPrinting();
    }

    @Test
    void testRcGetMiscSvrInfoSupportsTLSPayload() {
        final var host = "localhost";
        final var port = 1247;
        final var zone = "tempZone";
        final var username = "rods";

        var comm = assertDoesNotThrow(() -> IRODSApi.rcConnect(host, port, username, zone, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()));
        assertNotNull(comm);

        assertDoesNotThrow(() -> {
            var output = new Reference<MiscSvrInfo_PI>();
            IRODSApi.rcGetMiscSvrInfo(comm, output);
            assertNotNull(output.value);

            if (Versioning.compareVersions(comm.relVersion.substring(4), "4.3.3") > 0) {
                assertNotNull(output.value.BinBytesBuf_PI);
                log.debug("BinBytesBuf_PI = [{}]", output.value.BinBytesBuf_PI.buf);
                log.debug("BinBytesBuf_PI = [{}]", output.value.BinBytesBuf_PI.buf.substring(0, output.value.BinBytesBuf_PI.buflen));
            } else {
                assertNull(output.value.BinBytesBuf_PI);
            }
        });

        assertDoesNotThrow(() -> IRODSApi.rcDisconnect(comm));
    }

}
