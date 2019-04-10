/*
rebuild - Building your system freely.
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

package com.rebuild.server.business.dataio;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.RebuildException;
import com.rebuild.server.helper.QiniuCloud;
import com.rebuild.server.helper.SystemConfig;
import com.rebuild.server.helper.task.BulkTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entityhub.ClassificationService;
import com.rebuild.server.portals.ClassificationManager;

import cn.devezhao.commons.CodecUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 导入分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 */
public class ClassificationImporter extends BulkTask {
	
	private static final Log LOG = LogFactory.getLog(ClassificationImporter.class);
	
	private ID user;
	private ID dest;
	private String fileUrl;
	
	/**
	 * @param user
	 * @param dest
	 * @param fileUrl
	 */
	public ClassificationImporter(ID user, ID dest, String fileUrl) {
		this.user = user;
		this.dest = dest;
		this.fileUrl = fileUrl;
	}

	@Override
	public void run() {
		JSONArray data = null;
		try {
			data = (JSONArray) fetchRemoteJson(fileUrl);
		} catch (RebuildException e) {
			this.completedAfter();
			return;
		}
		
		Object[][] olds = Application.createQueryNoFilter(
				"select itemId from ClassificationData where dataId = ?")
				.setParameter(1, dest)
				.array();
		for (Object[] o : olds) {
			ClassificationManager.cleanCache((ID) o[0]);
		}
		String delSql = String.format("delete from `%s` where `DATA_ID` = '%s'",
				MetadataHelper.getEntity(EntityHelper.ClassificationData).getPhysicalName(), dest);
		Application.getSQLExecutor().execute(delSql);
		
		this.setThreadUser(user);
		
		this.setTotal(data.size());
		ID firstOne = null;
		try {
			for (Object o : data) {
				ID id = addNOne((JSONObject) o, null);
				if (firstOne == null) {
					firstOne = id;
				}
				this.setCompleteOne();
			}
		} finally {
			this.completedAfter();
		}
		
		// 更新开放级别
		int openLevel = 0;
		while (firstOne != null) {
			Object[] hasp = Application.createQueryNoFilter(
					"select itemId from ClassificationData where parent = ?")
					.setParameter(1, firstOne)
					.unique();
			if (hasp != null) {
				openLevel++;
			}
			firstOne = hasp == null ? null : (ID) hasp[0];
		}
		
		Record record = EntityHelper.forUpdate(dest, user);
		record.setInt("openLevel", openLevel);
		Application.getCommonService().update(record);
	}
	
	/**
	 * @param node
	 * @param parent
	 * @return
	 */
	private ID addNOne(JSONObject node, ID parent) {
		String code = node.getString("code");
		String name = node.getString("name");
		
		Record item = EntityHelper.forNew(EntityHelper.ClassificationData, Application.getCurrentUser());
		item.setString("name", name);
		if (StringUtils.isNotBlank(code)) {
			item.setString("code", code);
		}
		item.setID("dataId", this.dest);
		if (parent != null) {
			item.setID("parent", parent);
		}
		item = Application.getBean(ClassificationService.class).saveItem(item);
		
		JSONArray children = node.getJSONArray("children");
		if (children != null) {
			for (Object o : children) {
				addNOne((JSONObject) o, item.getPrimary());
			}
		}
		return item.getPrimary();
	}
	
	// -- Helper
	
	/**
	 * 远程仓库
	 */
	public static final String DATA_REPO = "https://raw.githubusercontent.com/getrebuild/rebuild-datas/master/classifications/";

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
		
		File tmp = SystemConfig.getFileOfTemp("classifications.tmp." + CodecUtils.randomCode(6));
		JSON d = null;
		try {
			if (QiniuCloud.instance().download(new URL(fileUrl), tmp)) {
				String t2str = FileUtils.readFileToString(tmp, "utf-8");
				d = (JSON) JSON.parse(t2str);
			}
		} catch (Exception e) {
			LOG.error("Fetch failure from URL : " + fileUrl, e);
		}
		
		if (d == null) {
			throw new RebuildException("无法读取远程数据");
		}
		return d;
	}
}
