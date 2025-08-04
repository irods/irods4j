package org.irods.irods4j.high_level;

import org.irods.irods4j.high_level.vfs.LogicalPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LogicalPathTest {

	@Test
	void testLogicalPathIsAbsolute() {
		Assertions.assertTrue(LogicalPath.isAbsolute("/"));
		Assertions.assertTrue(LogicalPath.isAbsolute("/tempZone"));
		Assertions.assertTrue(LogicalPath.isAbsolute("/tempZone/home"));

		Assertions.assertFalse(LogicalPath.isAbsolute(""));
		Assertions.assertFalse(LogicalPath.isAbsolute(" /"));
		Assertions.assertFalse(LogicalPath.isAbsolute("tempZone"));
		Assertions.assertFalse(LogicalPath.isAbsolute("tempZone/home/rods"));
		Assertions.assertFalse(LogicalPath.isAbsolute("."));
		Assertions.assertFalse(LogicalPath.isAbsolute(".."));

		Assertions.assertThrows(NullPointerException.class, () -> LogicalPath.isAbsolute(null));
	}

	@Test
	void testGetParentPathFromLogicalPath() {
		Assertions.assertNull(LogicalPath.parentPath(""));

		Assertions.assertEquals("/", LogicalPath.parentPath("/"));
		Assertions.assertEquals("/", LogicalPath.parentPath("/tempZone"));
		Assertions.assertEquals("/tempZone", LogicalPath.parentPath("/tempZone/home"));
		Assertions.assertEquals("/tempZone/home", LogicalPath.parentPath("/tempZone/home/rods"));

		Assertions.assertThrows(NullPointerException.class, () -> LogicalPath.parentPath(null));
	}

	@Test
	void testGetObjectNameFromLogicalPath() {
		Assertions.assertEquals("", LogicalPath.objectName(""));
		Assertions.assertEquals("", LogicalPath.objectName("/"));
		Assertions.assertEquals("tempZone", LogicalPath.objectName("/tempZone"));
		Assertions.assertEquals("home", LogicalPath.objectName("/tempZone/home"));
		Assertions.assertEquals("file.txt", LogicalPath.objectName("/tempZone/home/rods/file.txt"));
		Assertions.assertEquals("", LogicalPath.objectName("/tempZone/home/rods/"));

		Assertions.assertThrows(NullPointerException.class, () -> LogicalPath.objectName(null));
	}

	@Test
	void testSplitLogicalPathIntoPathElements() {
		Assertions.assertEquals(List.of(""), LogicalPath.segments(""));
		Assertions.assertEquals(List.of("/"), LogicalPath.segments("/"));
		Assertions.assertEquals(List.of("/", "tempZone", ""), LogicalPath.segments("/tempZone/"));
		Assertions.assertEquals(List.of("/", "tempZone", ".", ".."), LogicalPath.segments("/tempZone/./.."));

		Assertions.assertThrows(NullPointerException.class, () -> LogicalPath.segments(null));
	}

}
