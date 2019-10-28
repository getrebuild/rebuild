/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.utils;

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

	final private long checkPeriod = 1000 * 60 * 5;  // 5 min
	private long lastCheck = 0;
	
	// default is disabled
	private int maxBackupIndex = -1;

	public int getMaxBackupIndex() {
		return maxBackupIndex;
	}

	public void setMaxBackupIndex(int maxBackupIndex) {
		this.maxBackupIndex = maxBackupIndex;
	}

	@Override
	protected void subAppend(LoggingEvent event) {
		super.subAppend(event);
		
		long n = System.currentTimeMillis();
		if (n >= lastCheck) {
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
				log.delete();
				LogLog.debug("Deleted log : " + log);
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
