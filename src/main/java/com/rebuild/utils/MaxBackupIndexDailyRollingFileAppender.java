/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.utils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Auto delete logs
 * 
 * @author devezhao
 * @since 01/28/2019
 */
public class MaxBackupIndexDailyRollingFileAppender extends DailyRollingFileAppender {

    private long lastCheck = 0;
	
	// default is disabled
	private int maxBackupIndex = -1;

    /**
     * @return
     */
	public int getMaxBackupIndex() {
		return maxBackupIndex;
	}

    /**
     * @param maxBackupIndex
     */
	public void setMaxBackupIndex(int maxBackupIndex) {
		this.maxBackupIndex = maxBackupIndex;
	}

	@Override
	protected void subAppend(LoggingEvent event) {
		super.subAppend(event);
		
		long n = System.currentTimeMillis();
		if (n >= lastCheck) {
            // 5 min
            long checkPeriod = 1000 * 60 * 5;
            lastCheck = n + checkPeriod;
		} else {
			return;
		}

		// unset
		if (maxBackupIndex <= 0) {
			return;
		}
		
		File file = new File(fileName);
		File[] logs = file.getParentFile().listFiles(new LogFileFilter(file.getName()));
		if (logs == null || maxBackupIndex >= logs.length) {
			return;
		}
		
		Arrays.sort(logs, new CompratorByLastModified());
		for (int i = maxBackupIndex; i < logs.length;  i++) {
			File log = logs[i];
			if (log.exists()) {
                FileUtils.deleteQuietly(log);
				LogLog.debug("Deleted log file : " + log);
			}
		}
	}

	/**
	 * Filter
	 */
	static class LogFileFilter implements FileFilter {
		private String logName;
		protected LogFileFilter(String logName) {
			this.logName = logName;
		}
		@Override
		public boolean accept(File file) {
			if (logName == null || file.isDirectory()) {
				return false;
			} else {
				LogLog.debug(file.getName());
				return file.getName().startsWith(logName);
			}
		}
	}
	
	/**
	 * Sort max > min
	 */
	static class CompratorByLastModified implements Comparator<File> {
		@Override
		public int compare(File a, File b) {
			long d = a.lastModified() - b.lastModified();
			return d > 0 ? -1 : (d == 0 ? 0 : 1);
		}
	}
}
