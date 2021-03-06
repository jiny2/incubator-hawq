package com.pivotal.hawq.mapreduce.ft;

import com.google.common.collect.Lists;
import com.pivotal.hawq.mapreduce.HAWQTable;
import com.pivotal.hawq.mapreduce.MRFormatTestUtils;
import com.pivotal.hawq.mapreduce.SimpleTableClusterTester;
import com.pivotal.hawq.mapreduce.metadata.HAWQTableFormat;
import com.pivotal.hawq.mapreduce.metadata.MetadataAccessException;
import com.pivotal.hawq.mapreduce.util.HAWQJdbcUtils;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;


public class HAWQInputFormatFeatureTest_Misc extends SimpleTableClusterTester {

	@BeforeClass
	public static void setUpBeforeClass() {
		System.out.println("Executing test suite: Misc");
	}

	@Test
	public void testReadCOTable() throws Exception {
		final String tableName = "test_co_unsupported";

		final String setupSQLs = String.format(
				"DROP TABLE IF EXISTS %s;\n" +
				"CREATE TABLE %s (id int) with (appendonly=true, orientation=column);",
				tableName, tableName);

		final String expectedErrmsg = String.format(
				"Unsupported storage format for table '%s', only AO and Parquet are supported.",
				tableName);

		System.out.println("Executing test case: " + tableName);

		Connection conn = null;
		try {
			conn = MRFormatTestUtils.getTestDBConnection();
			MRFormatTestUtils.runSQLs(conn, setupSQLs);

		} finally {
			HAWQJdbcUtils.closeConnection(conn);
		}

		try {
			MRFormatTestUtils.runMapReduceOnCluster(tableName, HDFS_OUTPUT_PATH, null);
			Assert.fail("HAWQInputFormat.setInput should throw MetadataAccessException");

		} catch (MetadataAccessException e) {
			Assert.assertEquals(expectedErrmsg, e.getCause().getMessage());
		}

		System.out.println("Successfully finish test case: " + tableName);
	}

	@Test
	public void testTableUnderNonPublicNamespace() throws Exception {
		final String customNamespace = "my_test_schema";

		final String setupSQLs = String.format(
				"DROP SCHEMA IF EXISTS %s CASCADE;\n" +
				"CREATE SCHEMA %s;", customNamespace, customNamespace);

		Connection conn = null;
		try {
			conn = MRFormatTestUtils.getTestDBConnection();
			MRFormatTestUtils.runSQLs(conn, setupSQLs);
		} finally {
			HAWQJdbcUtils.closeConnection(conn);
		}

		HAWQTableFormat[] formats = { HAWQTableFormat.AO, HAWQTableFormat.Parquet };
		for (HAWQTableFormat format : formats) {
			final String tableName = customNamespace + ".test_" + format + "_namespace";

			HAWQTable table = new HAWQTable.Builder(tableName, Lists.newArrayList("int4"))
					.storage(format)
					.build();

			testSimpleTable(table);
		}
	}
}
