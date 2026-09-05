/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.aibot2.service;

import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.service.general.ObservableService;
import com.rebuild.core.service.general.recyclebin.RecycleStore;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.integration.QiniuCloud;
import com.rebuild.utils.CommonsUtils;
import com.rebuild.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * AI 助手会话
 *
 * @author devezhao
 * @since 2026/9/5
 */
@Service
@Slf4j
public class AibotChatService extends ObservableService {

    protected AibotChatService(PersistManagerFactory aPMFactory) {
        super(aPMFactory, false, false);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.AibotChat;
    }

    @Override
    public int delete(ID recordId) {
        final RecycleStore recycleBin = useRecycleStore(recordId);
        if (recycleBin == null) {
            cleanChatFiles(recordId);
            deleteChatAttach(recordId);
        }

        int d = super.delete(recordId);
        Application.getCommonsCache().evict("chat2-" + recordId);

        if (recycleBin != null) recycleBin.store();
        return d;
    }

    /**
     * 清理会话关联的物理文件
     *
     * @param chatid
     */
    public static void cleanChatFiles(ID chatid) {
        Object[][] attaches = Application.createQueryNoFilter(
                "select content from AibotChatAttach where chatId = ?")
                .setParameter(1, chatid)
                .array();

        for (Object[] attach : attaches) {
            if (attach[0] == null || JSONUtils.wellFormat(attach[0].toString())) continue;
            try {
                JSONArray attachArray = JSON.parseArray((String) attach[0]);
                if (attachArray == null) continue;

                for (int i = 0; i < attachArray.size(); i++) {
                    JSONObject item = attachArray.getJSONObject(i);
                    String fp = item.getString("file");
                    if (StringUtils.isBlank(fp)) continue;

                    try {
                        if (QiniuCloud.instance().available()) {
                            QiniuCloud.instance().delete(fp);
                        } else {
                            FileUtils.deleteQuietly(RebuildConfiguration.getFileOfData(fp));
                        }
                        log.info("Deleted chat file : {}", fp);
                    } catch (Exception ex) {
                        log.warn("Failed to delete chat file : {}", fp, ex);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to parse attach content for chat {}, skipped", chatid, ex);
            }
        }
    }

    /**
     * 删除 AibotChatAttach 记录
     *
     * @param chatid
     */
    private static void deleteChatAttach(ID chatid) {
        Object[][] attaches = Application.createQueryNoFilter(
                "select attachId from AibotChatAttach where chatId = ?")
                .setParameter(1, chatid)
                .array();
        if (attaches.length == 0) return;

        ID[] ids = new ID[attaches.length];
        for (int i = 0; i < attaches.length; i++) {
            ids[i] = (ID) attaches[i][0];
        }
        Application.getCommonsService().delete(ids, false);
    }
}
