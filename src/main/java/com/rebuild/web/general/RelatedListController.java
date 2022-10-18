/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.general;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Field;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.configuration.general.DataListManager;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.service.approval.ApprovalState;
import com.rebuild.core.service.feeds.FeedsType;
import com.rebuild.core.service.query.ParseHelper;
import com.rebuild.core.service.query.QueryHelper;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.general.ProtocolFilterParser;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import com.rebuild.web.EntityParam;
import com.rebuild.web.IdParam;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 相关项列表
 *
 * @author devezhao
 * @since 10/22/2018
 */
@RestController
@RequestMapping("/app/entity/")
public class RelatedListController extends BaseController {

    @GetMapping("related-list")
    public JSON relatedList(@IdParam(name = "mainid") ID mainid, HttpServletRequest request) {
        final ID user = getRequestUser(request);

        String related = getParameterNotNull(request, "related");
        String q = getParameter(request, "q");
        String sql = buildBaseSql(mainid, related, q, false, user);

        Entity relatedEntity = MetadataHelper.getEntity(related.split("\\.")[0]);

        String sort = getParameter(request, "sort", "modifiedOn:desc");
        // 名称字段排序
        if ("NAME".equalsIgnoreCase(sort)) {
            sort = relatedEntity.getNameField().getName() + ":asc";
        }
        sql += " order by " + sort.replace(":", " ");

        int pn = NumberUtils.toInt(getParameter(request, "pageNo"), 1);
        int ps = NumberUtils.toInt(getParameter(request, "pageSize"), 200);

        Object[][] array = QueryHelper.createQuery(sql, relatedEntity).setLimit(ps, pn * ps - ps).array();

        List<Object> res = new ArrayList<>();
        for (Object[] o : array) {
            Object nameValue = o[1];
            nameValue = FieldValueHelper.wrapFieldValue(nameValue, relatedEntity.getNameField(), true);
            if (nameValue == null || StringUtils.isEmpty(nameValue.toString())) {
                nameValue = FieldValueHelper.NO_LABEL_PREFIX + o[0].toString().toUpperCase();
            }

            int approvalState = o.length > 3 ? ObjectUtils.toInt(o[3]) : 0;
            boolean canUpdate = approvalState != ApprovalState.APPROVED.getState()
                    && approvalState != ApprovalState.PROCESSING.getState()
                    && Application.getPrivilegesManager().allowUpdate(user, (ID) o[0]);

            res.add(new Object[] {
                    o[0], nameValue, I18nUtils.formatDate((Date) o[2]),
                    approvalState, canUpdate });
        }

        return JSONUtils.toJSONObject(
                new String[] { "total", "data" },
                new Object[] { 0, res });
    }

    @GetMapping("related-counts")
    public Map<String, Integer> relatedCounts(@IdParam(name = "mainid") ID mainid, HttpServletRequest request) {
        final ID user = getRequestUser(request);
        String[] relateds = getParameterNotNull(request, "relateds").split(",");

        Map<String, Integer> countMap = new HashMap<>();
        for (String related : relateds) {
            String sql = buildBaseSql(mainid, related, null, true, user);

            // 任务是获取了全部的相关记录，因此总数可能与实际显示的条目数量不一致
            Entity relatedEntity = MetadataHelper.getEntity(related.split("\\.")[0]);
            Object[] count = QueryHelper.createQuery(sql, relatedEntity).unique();
            countMap.put(related, ObjectUtils.toInt(count[0]));
        }
        return  countMap;
    }

    private String buildBaseSql(ID mainid, String relatedExpr, String q, boolean count, ID user) {
        // format: Entity.Field
        Entity relatedEntity = MetadataHelper.getEntity(relatedExpr.split("\\.")[0]);

        String where = new ProtocolFilterParser(null).parseRelated(relatedExpr, mainid);

        // @see FeedsListController#fetchFeeds
        if (relatedEntity.getEntityCode() == EntityHelper.Feeds) {
            where += String.format(" and (type = %d or type = %d)",
                    FeedsType.FOLLOWUP.getMask(),
                    FeedsType.SCHEDULE.getMask());

            List<String> in = new ArrayList<>();
            in.add("scope = 'ALL'");
            for (Team t : Application.getUserStore().getUser(user).getOwningTeams()) {
                in.add(String.format("scope = '%s'", t.getIdentity()));
            }
            where += " and ( " + StringUtils.join(in, " or ") + " )";
        }

        if (StringUtils.isNotBlank(q)) {
            Set<String> searchFields = ParseHelper.buildQuickFields(relatedEntity, null);

            if (!searchFields.isEmpty()) {
                String like = " like '%" + StringEscapeUtils.escapeSql(q) + "%'";
                String searchWhere = " and ( " + StringUtils.join(searchFields.iterator(), like + " or ") + like + " )";
                where += searchWhere;
            }
        }

        Field primaryField = relatedEntity.getPrimaryField();
        Field namedField = relatedEntity.getNameField();

        StringBuilder sql = new StringBuilder("select ");
        if (count) {
            sql.append("count(").append(primaryField.getName()).append(")");
        } else {
            sql.append(primaryField.getName()).append(",")
                    .append(namedField.getName()).append(",")
                    .append(EntityHelper.ModifiedOn);

            if (MetadataHelper.hasApprovalField(relatedEntity)) {
                sql.append(",").append(EntityHelper.ApprovalState);
            }
        }

        sql.append(" from ").append(relatedEntity.getName()).append(" where ").append(where);
        return sql.toString();
    }

    @GetMapping("related-list-config")
    public RespBody getDataListConfig(HttpServletRequest req, @EntityParam Entity listEntity) {
        final ID user = getRequestUser(req);
        JSON config = DataListManager.instance.getFieldsLayout(listEntity.getName(), user);
        return RespBody.ok(config);
    }
}
