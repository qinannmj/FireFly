package cn.com.sparkle.firefly.stablestorage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.upgrade.VersionUpgradeTool;

/**
 * 
 * This is the version of local file. This will be the foundation of file upgrade in future.
 * @author qinan.qn
 *
 */
public class StoreVersion {
	private final static Logger logger = Logger.getLogger(StoreVersion.class);
	public final static String VERSION = "v1";
	public final static Map<String, String> operatorClassMap = new HashMap<String, String>();

	static {
		operatorClassMap.put("v1", "cn.com.sparkle.firefly.stablestorage.v1.RecordFileOperatorDefault");
		operatorClassMap.put("v1-mem", "cn.com.sparkle.firefly.stablestorage.v1.MemFileOperatorDefault");
	}

	@SuppressWarnings("unchecked")
	public static RecordFileOperator loadRecordFileOperator(String dir, long lastExpectSafeInstanceId, InstanceExecutor instanceExecutor, Configuration conf)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedChecksumAlgorithm {
		String version = readCurVersion(new File(dir));
		clearWorkspace(dir, version);
		if (!operatorClassMap.containsKey(version)) {
			throw new RuntimeException("not find RecordFileOperator of version " + version);
		}

		Class<RecordFileOperator> clazz = (Class<RecordFileOperator>) Class.forName(operatorClassMap.get(version));
		RecordFileOperator oper = clazz.newInstance();
		File f = new File(dir + "/log/" + version);

		Class<RecordFileOutFactory> factoryClazz = (Class<RecordFileOutFactory>) Class.forName(conf.getFileOutFacotryClass());
		RecordFileOutFactory outFactory = factoryClazz.newInstance();
		outFactory.init(conf);

		oper.initOperator(f, lastExpectSafeInstanceId, instanceExecutor, outFactory, conf);

		if (version.equals(VERSION)) {
			return oper;
		} else {
			//upgrade process
			Class<RecordFileOperator> newClazz = (Class<RecordFileOperator>) Class.forName(operatorClassMap.get(VERSION));

			RecordFileOperator _oper = newClazz.newInstance();
			File newFile = new File(dir + "/log/" + VERSION);
			_oper.initOperator(newFile, oper.getMinSuccessRecordInstanceId(), null, outFactory, conf);
			VersionUpgradeTool vut = new VersionUpgradeTool();
			boolean isSucc = vut.update(oper, _oper);
			if (isSucc) {
				clearWorkspace(dir, VERSION);
				File nVer = new File(dir + "/tmpversion-" + VERSION);
				File oVer = new File(dir + "/version-" + version);
				oVer.delete();
				nVer.renameTo(new File(dir + "/version-" + VERSION));
				oper = clazz.newInstance();
				oper.initOperator(f, lastExpectSafeInstanceId, instanceExecutor, outFactory, conf);
			} else {
				logger.warn("upgrade process is failed, retry next start.");
			}
		}
		return oper;

	}

	private static void clearWorkspace(String dir, String version) {

		File[] fs = new File(dir).listFiles();
		for (File f : fs) {
			if (!f.getName().equals(version)) {
				f.delete();
			}
		}
	}

	public static String readCurVersion(File f) throws IOException {
		File[] files = f.listFiles();
		File tmpVersion = null;
		File version = null;
		for (File file : files) {
			if (!file.isDirectory()) {
				String[] name = file.getName().split("-");
				if (name.length == 2) {
					//restore from tmp
					if (name[0].equals("tmpversion")) {
						tmpVersion = file;
					} else if (name[0].equals("version")) {
						version = file;
					}
				}
			}
		}
		if (tmpVersion != null) {
			//the tmp version is latest, restore from tmp version
			if (version != null) {
				version.delete();
			}
			String[] name = tmpVersion.getName().split("-");
			File newFile = new File(f.getCanonicalPath() + "/version-" + name[1]);
			tmpVersion.renameTo(newFile);
			return name[1];
		} else if (version != null) {
			String[] name = version.getName().split("-");
			return name[1];
		} else {
			File newFile = new File(f.getCanonicalPath() + "/version-" + VERSION);
			newFile.createNewFile();
			return VERSION;
		}
	}
}
