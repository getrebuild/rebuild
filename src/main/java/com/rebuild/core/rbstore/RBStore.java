/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.RebuildException;
import com.rebuild.core.support.License;
import com.rebuild.utils.HttpUtils;
import com.rebuild.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RB 在线元数据仓库
 *
 * @author devezhao-mbp zhaofang123@gmail.com
 * @since 2019/04/28
 */
public class RBStore {

    private static final Logger LOG = LoggerFactory.getLogger(RBStore.class);

    /**
     * RB 仓库地址 https://github.com/getrebuild/rebuild-datas/
     */
    private static final String DATA_REPO = "https://getrebuild.com/gh/getrebuild/rebuild-datas/";

    /**
     * for Classification
     *
     * @param fileUri
     * @return
     */
    public static JSON fetchClassification(String fileUri) {
        return fetchRemoteJson("classifications/" + fileUri);
    }

    /**
     * for Metaschema
     *
     * @param fileUri
     * @return
     */
    public static JSON fetchMetaschema(String fileUri) {
        return fetchRemoteJson("metaschemas/" + fileUri);
    }

    /**
     * for BusinessModel
     *
     * @param fileUri
     * @return
     * @see License
     */
    public static JSONObject fetchBusinessModel(String fileUri) {
        return License.siteApi("api/business-model/" + fileUri, true);
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

        try {
            String content = HttpUtils.get(fileUrl);
            if (JSONUtils.wellFormat(content)) {
                return (JSON) JSON.parse(content);
            }

        } catch (Exception e) {
            LOG.error("Fetch failure from URL : " + fileUrl, e);
        }

        throw new RebuildException("Unable to read data from RB-Store");
    }
}
