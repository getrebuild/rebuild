/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.configuration;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.ConfigBean;
import com.rebuild.core.configuration.general.AdvFilterManager;
import com.rebuild.core.configuration.general.AdvFilterService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 高级查询
 *
 * @author devezhao
 * @since 10/14/2018
 */
@RestController
@RequestMapping("/app/{entity}/")
public class AdvFilterController extends BaseController implements ShareTo {

    @PostMapping("advfilter/post")
    public RespBody sets(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ID filterId = getIdParameter(request, "id");
        String filterName = getParameter(request, "name");

        // 不是自己的就另存为
        if (filterId != null && !UserHelper.isSelf(user, filterId)) {
            if (StringUtils.isBlank(filterName)) {
                ConfigBean o = AdvFilterManager.instance.getAdvFilter(filterId);
                if (o != null) {
                    filterName = o.getString("name") + "-" + Language.L("复制");
                }
            }
            filterId = null;
        }

        JSON filter = ServletUtils.getRequestJson(request);
        Record record;
        if (filterId == null) {
            record = EntityHelper.forNew(EntityHelper.FilterConfig, user);
            record.setString("belongEntity", entity);
            if (StringUtils.isBlank(filterName)) {
                filterName =  Language.L("查询") + "-" + CalendarUtils.format("MMddHHmm", CalendarUtils.now());
            }
        } else {
            record = EntityHelper.forUpdate(filterId, user);
        }

        record.setString("config", filter.toJSONString());
        putCommonsFields(request, record);
        if (StringUtils.isNotBlank(filterName)) {
            record.setString("filterName", filterName);
        }
        record = Application.getBean(AdvFilterService.class).createOrUpdate(record);

        return RespBody.ok(JSONUtils.toJSONObject("id", record.getPrimary()));
    }

    @GetMapping("advfilter/get")
    public RespBody gets(@PathVariable String entity, HttpServletRequest request) {
        ID filterId = getIdParameter(request, "id");
        ConfigBean filter = AdvFilterManager.instance.getAdvFilter(filterId);
        if (filter == null) {
            return RespBody.errorl("未知过滤条件");
        } else {
            return RespBody.ok(filter.toJSON());
        }
    }

    @GetMapping("advfilter/list")
    public JSON list(@PathVariable String entity, HttpServletRequest request) {
        return AdvFilterManager.instance.getAdvFilterList(entity, getRequestUser(request));
    }

    @RequestMapping("advfilter/test-equation")
    public RespBody testEquation(@PathVariable String entity, HttpServletRequest request) {
        final String equation = ServletUtils.getRequestString(request);
        if (StringUtils.isBlank(equation)) return RespBody.ok();

        String valid = AdvFilterParser.validEquation(equation);
        return valid == null ? RespBody.error() : RespBody.ok();
    }

    @RequestMapping("advfilter/add-commons")
    public RespBody addCommons(@PathVariable String entity, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String adds = request.getParameter("adds");

        if (adds.contains("DAY_NEW")) {
            String f = "[{index:1,field:\"createdOn\",op:\"TDA\"}]";
            addAdvFilter(entity, Language.L("今天新增的"), f, user);
        }
        if (adds.contains("WEEK_NEW")) {
            String f = "[{index:1,field:\"createdOn\",op:\"CUW\"}]";
            addAdvFilter(entity, Language.L("本周新增的"), f, user);
        }
        if (adds.contains("MONTH_NEW")) {
            String f = "[{index:1,field:\"createdOn\",op:\"CUM\"}]";
            addAdvFilter(entity, Language.L("本月新增的"), f, user);
        }

        if (adds.contains("MY_OWN")) {
            String f = "[{index:1,field:\"owningUser\",op:\"SFU\"}]";
            addAdvFilter(entity, Language.L("属于我的"), f, user);
        }
        if (adds.contains("MY_CREATE")) {
            String f = "[{index:1,field:\"createdBy\",op:\"SFU\"}]";
            addAdvFilter(entity, Language.L("我创建的"), f, user);
        }
        if (adds.contains("MY_MODIFY")) {
            String f = "[{index:1,field:\"modifiedBy\",op:\"SFU\"}]";
            addAdvFilter(entity, Language.L("我修改的"), f, user);
        }

        return RespBody.ok();
    }

    private void addAdvFilter(String entity, String name, String filterItems, ID user) {
        Record record = EntityHelper.forNew(EntityHelper.FilterConfig, user);
        record.setString("belongEntity", entity);
        record.setString("filterName", name);

        JSONObject filterConfig = JSONUtils.toJSONObject(
                new String[] { "entity", "items" },
                new Object[] { entity, JSONArray.parse(filterItems) });
        record.setString("config", filterConfig.toJSONString());

        Application.getBean(AdvFilterService.class).createOrUpdate(record);
    }
}
