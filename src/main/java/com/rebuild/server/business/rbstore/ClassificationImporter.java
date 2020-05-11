/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.business.rbstore;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.helper.task.HeavyTask;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.configuration.ClassificationService;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 导入分类数据
 * 
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/04/08
 */
public class ClassificationImporter extends HeavyTask<Integer> {

    protected static final int LEVEL_BEGIN = 0;
	
	final private ID dest;
	final private String fileUrl;

	/**
	 * @param dest
	 * @param fileUrl
	 */
	public ClassificationImporter(ID dest, String fileUrl) {
		this.dest = dest;
		this.fileUrl = fileUrl;
	}

	@Override
	protected Integer exec() throws Exception {
		final JSONArray data = (JSONArray) RBStore.fetchClassification(fileUrl);
		this.setTotal(data.size());

		for (Object o : data) {
			addNItem((JSONObject) o, null, LEVEL_BEGIN);
			this.addCompleted();
		}
		return getSucceeded();
	}

	private ID addNItem(JSONObject node, ID parent, int level) {
		String name = node.getString("name");
		String code = node.getString("code");

		final ID itemId = findOrCreate(name, code, parent, level);

		JSONArray children = node.getJSONArray("children");
		if (children != null) {
		    int nexeLevel = level + 1;
			for (Object ch : children) {
				addNItem((JSONObject) ch, itemId, nexeLevel);
			}
		}
		return itemId;
	}

    /**
     * 查找分类项，找不到则创建
     *
     * @param name
     * @param code
     * @param parent
     * @param level
     * @return
     */
	protected ID findOrCreate(String name, String code, ID parent, int level) {
	    String sql = "select itemId from ClassificationData where dataId = ? and ";
	    if (StringUtils.isNotBlank(code)) {
	        sql += String.format("(code = '%s' or name = '%s')",
                    StringEscapeUtils.escapeSql(code), StringEscapeUtils.escapeSql(name));
        } else {
            sql += String.format("name = '%s'", StringEscapeUtils.escapeSql(name));
        }

	    if (parent != null) {
	        sql += String.format(" and parent = '%s'", parent);
        }

	    Object[] exists = Application.createQueryNoFilter(sql).setParameter(1, dest).unique();
	    if (exists != null) {
	        return (ID) exists[0];
        }

        Record item = EntityHelper.forNew(EntityHelper.ClassificationData, this.getUser());
        item.setString("name", name);
        item.setInt("level", level);
        item.setID("dataId", dest);
        if (StringUtils.isNotBlank(code)) {
            item.setString("code", code);
        }
        if (parent != null) {
            item.setID("parent", parent);
        }

        item = Application.getBean(ClassificationService.class).createOrUpdateItem(item);
        this.addSucceeded();
        return item.getPrimary();
    }
}
