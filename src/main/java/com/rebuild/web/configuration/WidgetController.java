/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.BaseLayoutManager;
import com.rebuild.core.configuration.general.DataListClass;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.configuration.general.LayoutConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.web.BaseController;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author devezhao
 * @since 2019/10/18
 */
@RestController
@RequestMapping("/app/{entity}/")
public class WidgetController extends BaseController implements ShareTo {

    @PostMapping("widget-charts")
    public RespBody sets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSON config = ServletUtils.getRequestJson(request);
        ID cfgid = getIdParameter(request, "id");
        if (cfgid != null && !UserHelper.isSelf(user, cfgid)) {
            cfgid = null;
        }

        Record record;
        if (cfgid == null) {
            record = EntityHelper.forNew(EntityHelper.LayoutConfig, user);
            record.setString("belongEntity", entity);
            record.setString("applyType", BaseLayoutManager.TYPE_WCHARTS);
            record.setString("shareTo", BaseLayoutManager.SHARE_SELF);
        } else {
            record = EntityHelper.forUpdate(cfgid, user);
        }
        record.setString("config", config.toJSONString());
        putCommonsFields(request, record);
        record = Application.getBean(LayoutConfigService.class).createOrUpdate(record);

        return RespBody.ok(record.getPrimary());
    }

    @GetMapping("widget-charts")
    public RespBody gets(@PathVariable String entity, HttpServletRequest request) {
        ConfigBean config = DataListManager.instance.getWidgetCharts(getRequestUser(request), entity);
        return RespBody.ok(config == null ? null : config.toJSON());
    }

    @GetMapping("widget-class-data")
    public RespBody getClassData(@PathVariable String entity, HttpServletRequest request) {
        JSON data = DataListClass.datas(MetadataHelper.getEntity(entity), getRequestUser(request));
        return RespBody.ok(data);
    }
}
