/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.rbstore;

import cn.devezhao.commons.CodecUtils;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.RebuildException;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * RB 在线元数据仓库
 * 
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
public class RBStore {
	
	private static final Log LOG = LogFactory.getLog(RBStore.class);

	public static final String DATA_REPO = "https://raw.githubusercontent.com/getrebuild/rebuild-datas/master/";

	/**
	 * for Classification
	 * 
	 * @param fileUrl
	 * @return
	 */
	public static JSON fetchClassification(String fileUrl) {
		return fetchRemoteJson("classifications/" + fileUrl);
	}
	
	/**
	 * for Metaschema
	 * 
	 * @param fileUrl
	 * @return
	 */
	public static JSON fetchMetaschema(String fileUrl) {
		return fetchRemoteJson("metaschemas/" + fileUrl);
	}
	
	/**
	 * 远程读取 JSON 文件
	 * 
	 * @param fileUrl
	 * @return
	 * @throws RebuildException 如果读取失败
	 */
	public static JSON fetchRemoteJson(String fileUrl) throws RebuildException {
		if (!fileUrl.startsWith("http")) {
			fileUrl = DATA_REPO + fileUrl;
		}
		
		File tmp = SysConfiguration.getFileOfTemp("rbstore." + CodecUtils.randomCode(6));
		JSON d = null;
		try {
			CommonsUtils.readBinary(fileUrl, tmp);
			String t2str = FileUtils.readFileToString(tmp, "utf-8");
			d = (JSON) JSON.parse(t2str);
			tmp.delete();
		} catch (Exception e) {
			LOG.error("Fetch failure from URL : " + fileUrl, e);
		}
		
		if (d == null) {
			throw new RebuildException("无法读取远程数据");
		}
		return d;
	}
}
