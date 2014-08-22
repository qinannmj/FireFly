package cn.com.sparkle.firefly.stablestorage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.firefly.stablestorage.upgrade.VersionUpgradeTool;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;

/**
 * 
 * This is the version of local file. This will be the foundation of file upgrade in future.
 * @author qinan.qn
 *
 */
public class StoreVersion {
	private final static Logger logger = Logger.getLogger(StoreVersion.class);
	public final static String VERSION = "v2";
	public final static Map<String, String> operatorClassMap = new HashMap<String, String>();

	static {
		operatorClassMap.put("v1", "cn.com.sparkle.firefly.stablestorage.v1.RecordFileOperatorDefault");
		operatorClassMap.put("v2", "cn.com.sparkle.firefly.stablestorage.v2.RecordFileOperatorV2");
	}

	@SuppressWarnings("unchecked")
	public static RecordFileOperator loadRecordFileOperator(String dir, long lastExpectSafeInstanceId, InstanceExecutor instanceExecutor, Context context)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedChecksumAlgorithm {
		String logDir = dir + "/log";
		String version = readCurVersion(FileUtil.getDir(logDir));
		clearWorkspace(logDir, version);
		if (!operatorClassMap.containsKey(version)) {
			throw new RuntimeException("not find RecordFileOperator of version " + version);
		}

		Class<RecordFileOperator> clazz = (Class<RecordFileOperator>) Class.forName(operatorClassMap.get(version));
		RecordFileOperator oper = clazz.newInstance();
		File f = FileUtil.getDir(logDir + "/" + version);

		Class<RecordFileOutFactory> factoryClazz = (Class<RecordFileOutFactory>) Class.forName(context.getConfiguration().getFileOutFacotryClass());
		RecordFileOutFactory outFactory = factoryClazz.newInstance();
		outFactory.init(context.getConfiguration());

		oper.initOperator(f, lastExpectSafeInstanceId, instanceExecutor, outFactory, context);
		
		if (version.equals(VERSION)) {
			return oper;
		} else {
			//upgrade process
			Class<RecordFileOperator> newClazz = (Class<RecordFileOperator>) Class.forName(operatorClassMap.get(VERSION));

			RecordFileOperator _oper = newClazz.newInstance();
			File newFile = FileUtil.getDir(logDir + "/" + VERSION);
			if (!oper.isDamaged()) {
				_oper.initOperator(newFile, oper.getMinSuccessRecordInstanceId(), new NullExecutor(), outFactory, context);
				_oper.loadData();
				VersionUpgradeTool vut = new VersionUpgradeTool();
				boolean isSucc = vut.update(oper, _oper);
				if (isSucc) {

					File nVer = FileUtil.getFile(logDir + "/tmpversion-" + VERSION);
					File oVer = new File(logDir + "/version-" + version);
					FileUtil.deleteFile(oVer);
					FileUtil.rename(nVer, new File(logDir + "/version-" + VERSION));

					oper.close();
					clearWorkspace(logDir, VERSION);
					oper = _oper;
					oper.setExecutor(instanceExecutor);
					logger.info("file system upgrade successfully");
				} else {
					logger.warn("upgrade process is failed, retry next start.");
				}
			}
		}
		return oper;

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
				FileUtil.deleteFile(version);
			}
			String[] name = tmpVersion.getName().split("-");
			File newFile = new File(f.getCanonicalPath() + "/version-" + name[1]);
			FileUtil.rename(tmpVersion, newFile);
			return name[1];
		} else if (version != null) {
			String[] name = version.getName().split("-");
			return name[1];
		} else {
			FileUtil.getFile(f.getCanonicalPath() + "/version-" + VERSION);
			return VERSION;
		}
	}

	private static void clearWorkspace(String dir, String version) {

		File[] fs = new File(dir).listFiles();
		for (File f : fs) {

			if (f.isDirectory() && !f.getName().equals(version)) {
				FileUtil.deleteDir(f);
			}
		}
	}

	private static class NullExecutor extends InstanceExecutor {
		public NullExecutor() {
			super(null, null, 0);
		}

		@Override
		public void execute(SuccessfulRecordWrap recordWrap) {
		}
	}

}
