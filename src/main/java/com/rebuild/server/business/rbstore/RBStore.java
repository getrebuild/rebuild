/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
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

    /**
     * RB 仓库地址 https://github.com/getrebuild/rebuild-datas/
     */
	private static final String DATA_REPO = "https://getrebuild.com/gh/getrebuild/rebuild-datas/";

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
			FileUtils.deleteQuietly(tmp);
		} catch (Exception e) {
			LOG.error("Fetch failure from URL : " + fileUrl, e);
		}
		
		if (d == null) {
			throw new RebuildException("无法读取 RB 仓库数据");
		}
		return d;
	}
}
