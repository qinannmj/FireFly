package cn.com.sparkle.firefly.stablestorage.util;

import java.io.File;
import java.io.IOException;

public class FileUtil {
	public static File getFile(String file) throws IOException {
		File f = new File(file);
		createNewFile(f);
		return f;
	}

	public static void createNewFile(File f) throws IOException {
		if (!f.exists()) {
			boolean r = f.createNewFile();
			if (!r) {
				throw new RuntimeException(String.format("%s create file failed!", f.getAbsoluteFile()));
			}
		} else if (f.isDirectory()) {
			throw new RuntimeException(String.format("%s is dir!", f.getAbsoluteFile()));
		}
	}

	public static File getDir(String dir) {
		File f = new File(dir);
		mkdirs(f);
		return f;
	}

	public static void mkdirs(File f) {
		if (!f.exists()) {
			boolean r = f.mkdirs();
			if (!r) {
				throw new RuntimeException(String.format("%s mkdirs failed!", f.getAbsoluteFile()));
			}
		} else if (f.isFile()) {
			throw new RuntimeException(String.format("%s is file!", f.getAbsoluteFile()));
		}
	}

	public static void deleteFile(File f) {
		if (f.exists()) {
			boolean r = f.delete();
			if (!r) {
				throw new RuntimeException(String.format("can't delete file %s", f.getAbsoluteFile()));
			}
		}
	}

	public static void rename(File oldFile, File newFile) {
		boolean r = oldFile.renameTo(newFile);
		if (!r) {
			throw new RuntimeException(String.format("can't rename %s to %s", oldFile.getAbsoluteFile(), newFile.getAbsoluteFile()));
		}
	}

	public static void deleteDir(File f) {
		if (!f.isDirectory()) {
			throw new RuntimeException(String.format("%s is not a dirctory!", f.getAbsoluteFile()));
		} else {
			File[] fs = f.listFiles();
			for (File file : fs) {
				if (file.isDirectory()) {
					deleteDir(file);
				} else {
					deleteFile(file);
				}
			}
			boolean r = f.delete();
			if (!r) {
				throw new RuntimeException(String.format("can't delete dir %s", f.getAbsoluteFile()));
			}
		}
	}
}
