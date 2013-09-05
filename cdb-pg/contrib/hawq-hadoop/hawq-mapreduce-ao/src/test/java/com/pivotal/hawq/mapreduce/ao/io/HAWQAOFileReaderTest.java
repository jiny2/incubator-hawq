package com.pivotal.hawq.mapreduce.ao.io;

import java.io.BufferedWriter;
import java.io.FileWriter;

import com.pivotal.hawq.mapreduce.ao.db.Metadata;
import com.pivotal.hawq.mapreduce.ao.file.HAWQAOFileStatus;
import com.pivotal.hawq.mapreduce.ao.file.HAWQAOSplit;
import com.pivotal.hawq.mapreduce.ao.io.HAWQAOFileReader;
import com.pivotal.hawq.mapreduce.ao.io.HAWQAORecord;
import com.pivotal.hawq.mapreduce.file.HAWQFileStatus;
import com.pivotal.hawq.mapreduce.schema.HAWQSchema;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class HAWQAOFileReaderTest
{

	private static int printUsage()
	{
		System.out
				.println("HAWQAOFileReaderTest <database_url> <table_name> <whetherToLong>");
		return 2;
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 3)
		{
			System.exit(printUsage());
		}

		String db_url = args[0];
		String table_name = args[1];
		String whetherToLogStr = args[2];

		Metadata metadata = new Metadata(db_url, null, "", table_name);
		HAWQFileStatus[] fileAttributes = metadata.getFileStatus();

		BufferedWriter bw = null;
		boolean whetherToLog = whetherToLogStr.equals("Y");
		if (whetherToLog)
		{
			bw = new BufferedWriter(new FileWriter(table_name + "_test"));
		}

		Configuration conf = new Configuration();
		conf.addResource(new Path(
				"/Users/wangj77/hadoop-2.0.2-alpha-gphd-2.0.1/etc/hadoop/hdfs-site.xml"));
		conf.addResource(new Path(
				"/home/ioformat/hdfs/etc/hadoop/hdfs-site.xml"));
		conf.reloadConfiguration();

		for (int i = 0; i < fileAttributes.length; i++)
		{
			String pathStr = fileAttributes[i].getPathStr();
			long fileLength = fileAttributes[i].getFileLength();
			HAWQAOFileStatus aofilestatus = (HAWQAOFileStatus) fileAttributes[i];
			boolean checksum = aofilestatus.getChecksum();
			String compressType = aofilestatus.getCompressType();
			int blocksize = aofilestatus.getBlockSize();
			Path path = new Path(pathStr);
			HAWQAOSplit aosplit;
			if (fileLength != 0)
			{
				FileSystem fs = path.getFileSystem(conf);
				BlockLocation[] blkLocations = fs.getFileBlockLocations(
						fs.getFileStatus(path), 0, fileLength);
				// not splitable
				aosplit = new HAWQAOSplit(path, 0, fileLength,
						blkLocations[0].getHosts(), checksum, compressType,
						blocksize);
			}
			else
			{
				// Create empty hosts array for zero length files
				aosplit = new HAWQAOSplit(path, 0, fileLength, new String[0],
						checksum, compressType, blocksize);
			}

			String tableEncoding = metadata.getTableEncoding();
			HAWQSchema schema = metadata.getSchema();

			HAWQAOFileReader reader = new HAWQAOFileReader(conf, aosplit);

			HAWQAORecord record = new HAWQAORecord(schema, tableEncoding);

			long begin = System.currentTimeMillis();
			while (reader.readRecord(record))
			{
				if (whetherToLog)
				{
					int columnCount = record.getSchema().getFieldCount();
					bw.write(record.getString(1));
					for (int j = 2; j <= columnCount; j++)
					{
						bw.write("|");
						bw.write(record.getString(j));
					}
					bw.write("\n");
				}
			}
			long end = System.currentTimeMillis();
			System.out.println("Time elapsed: " + (end - begin) + " for "
					+ fileAttributes[i]);
		}
		if (whetherToLog)
		{
			bw.close();
		}
	}
}