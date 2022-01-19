/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.admin.metadata;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.configuration.general.ViewAddonsManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 视图-相关项显示
 *
 * @author devezhao
 * @since 10/23/2018
 */
@RestController
@RequestMapping("/admin/entity/")
public class ViewAddonsController extends BaseController {

    @PostMapping("{entity}/view-addons")
    public RespBody sets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);
        JSON config = ServletUtils.getRequestJson(request);

        ID configId = ViewAddonsManager.instance.detectUseConfig(user, entity, applyType);

        Record record;
        if (configId == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", applyType);
            record.setString("shareTo", ViewAddonsManager.SHARE_ALL);
        } else {
            record = EntityHelper.forUpdate(configId, user);
        }
        record.setString("config", config.toJSONString());
        Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        return RespBody.ok();
    }

    @GetMapping("{entity}/view-addons")
    public JSON gets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String applyType = getParameter(request, "type", ViewAddonsManager.TYPE_TAB);

        ConfigBean config = ViewAddonsManager.instance.getLayout(user, entity, applyType);
        // fix: v2.2 兼容
        JSON configJson = config == null ? null : config.getJSON("config");
        if (configJson instanceof JSONArray) {
            configJson = JSONUtils.toJSONObject("items", configJson);
        }

        Entity entityMeta = MetadataHelper.getEntity(entity);
        Set<Entity> mfRefs = ViewAddonsManager.hasMultiFieldsReferenceTo(entityMeta);

        List<String[]> refs = new ArrayList<>();
        for (Field field : entityMeta.getReferenceToFields(true)) {
            Entity e = field.getOwnEntity();
            if (e.getMainEntity() != null) {
                continue;
            }

            String label = EasyMetaFactory.getLabel(e);
            if (mfRefs.contains(e)) {
                label = EasyMetaFactory.getLabel(field) + " (" + label + ")";
            }
            refs.add(new String[] { e.getName() + ViewAddonsManager.EF_SPLIT + field.getName(), label });
        }

        // 跟进（动态）
        refs.add(new String[] { "Feeds.relatedRecord", Language.L("动态") });
        // 任务（项目）
        refs.add(new String[] { "ProjectTask.relatedRecord", Language.L("任务") });
        // 附件
        if (ViewAddonsManager.TYPE_TAB.equals(applyType)) {
            refs.add(new String[] { "Attachment.relatedRecord", Language.L("附件") });
        }

        return JSONUtils.toJSONObject(
                new String[] { "config", "refs" },
                new Object[] { configJson, refs });
    }
}
