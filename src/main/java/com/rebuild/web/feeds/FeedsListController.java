/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.web.feeds;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.api.RespBody;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.metadata.easymeta.EasyEntity;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.feeds.FeedsHelper;
import com.rebuild.core.service.feeds.FeedsScope;
import com.rebuild.core.service.feeds.FeedsType;
import com.rebuild.core.service.query.AdvFilterParser;
import com.rebuild.core.support.general.FieldValueHelper;
import com.rebuild.core.support.i18n.I18nUtils;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 列表相关
 *
 * @author devezhao
 * @since 2019/11/1
 */
@RestController
public class FeedsListController extends BaseController {

    /**
     * @see FeedsType
     */
    @GetMapping("/feeds/{type}")
    public ModelAndView pageIndex(@PathVariable String type, HttpServletRequest request) {
        ModelAndView mv = createModelAndView("/feeds/home");
        mv.getModel().put("feedsType", type);

        User user = Application.getUserStore().getUser(getRequestUser(request));
        mv.getModel().put("UserEmail", user.getEmail());
        mv.getModel().put("UserMobile", StringUtils.defaultIfBlank(user.getWorkphone(), ""));

        return mv;
    }

    @PostMapping("/feeds/feeds-list")
    public RespBody fetchFeeds(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        JSON filter = ServletUtils.getRequestJson(request);
        final AdvFilterParser parser = new AdvFilterParser((JSONObject) filter);
        String sqlWhere = parser.toSqlWhere();
        if (sqlWhere == null) {
            sqlWhere = "(1=1)";
        }

        int type = getIntParameter(request, "type", 0);
        if (type == 1) {
            sqlWhere += String.format(" and exists (select feedsId from FeedsMention where ^feedsId = feedsId and user = '%s')", user);
        } else if (type == 2) {
            sqlWhere += String.format(" and exists (select feedsId from FeedsComment where ^feedsId = feedsId and createdBy = '%s')", user);
        } else if (type == 3) {
            sqlWhere += String.format(" and exists (select source from FeedsLike where ^feedsId = source and createdBy = '%s')", user);
        } else if (type == 10) {
            sqlWhere += String.format(" and createdBy ='%s'", user);
        }

        // 私密的仅显示在私密标签下
        if (type == 11) {
            sqlWhere += String.format(" and createdBy ='%s' and scope = 'SELF'", user);
        } else if (!parser.getIncludeFields().contains("scope")) {
            Set<Team> teams = Application.getUserStore().getUser(user).getOwningTeams();
            List<String> in = new ArrayList<>();
            in.add("scope = 'ALL'");
            for (Team t : teams) {
                in.add(String.format("scope = '%s'", t.getIdentity()));
            }
            sqlWhere += " and ( " + StringUtils.join(in, " or ") + " )";
        }

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 40);
        String sort = getParameter(request, "sort");

        long count = -1;
        if (pageNo == 1) {
            count = (Long) Application.createQueryNoFilter(
                    "select count(feedsId) from Feeds where " + sqlWhere).unique()[0];
            if (count == 0) {
                return RespBody.ok();
            }
        }

        String sql = "select feedsId,createdBy,createdOn,modifiedOn,content,images,attachments," +
                "scope,type,relatedRecord,contentMore from Feeds where " + sqlWhere;

        // 焦点动态
        ID foucs = getIdParameter(request, "foucs");
        Object[] foucsFeed = null;
        if (foucs != null) {
            foucsFeed = Application.createQueryNoFilter(sql + " and feedsId = ?")
                    .setParameter(1, foucs)
                    .unique();
        }

        if ("older".equalsIgnoreCase(sort)) {
            sql += " order by createdOn asc";
        } else if ("modified".equalsIgnoreCase(sort)) {
            sql += " order by modifiedOn desc";
        } else {
            sql += " order by createdOn desc";
        }

        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        if (foucsFeed != null) {
            List<Object[]> newArray = new ArrayList<>();
            newArray.add(foucsFeed);
            for (Object[] o : array) {
                if (foucsFeed[0].equals(o[0])) {
                    continue;
                }
                newArray.add(o);
            }
            array = newArray.toArray(new Object[0][]);
        }

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            FeedsScope scope = FeedsScope.parse((String) o[7]);
            if (scope == FeedsScope.GROUP) {
                Team team = Application.getUserStore().getTeam(ID.valueOf((String) o[7]));
                item.put("scope", new Object[]{team.getIdentity(), team.getName()});
            } else {
                item.put("scope", scope.getName());
            }
            item.put("type", o[8]);
            item.put("numComments", FeedsHelper.getNumOfComment((ID) o[0]));

            // 相关记录
            ID related = (ID) o[9];
            if (related != null && MetadataHelper.containsEntity(related.getEntityCode())) {
                EasyEntity entity = EasyMetaFactory.valueOf(related.getEntityCode());
                String nameValue = FieldValueHelper.getLabelNotry(related);
                JSONObject mixValue = FieldValueHelper.wrapMixValue(related, nameValue);
                mixValue.put("icon", entity.getIcon());
                mixValue.put("entityLabel", entity.getLabel());
                item.put("relatedRecord", mixValue);
            }

            // 更多内容
            if (o[10] != null) {
                item.put("contentMore", JSON.parse((String) o[10]));
            }

            list.add(item);
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[] { "total", "data" }, new Object[] { count, list }));
    }

    @GetMapping("/feeds/comments-list")
    public RespBody fetchComments(HttpServletRequest request) {
        final ID user = getRequestUser(request);

        ID feeds = getIdParameterNotNull(request, "feeds");
        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 20);

        String sqlWhere = String.format("feedsId = '%s'", feeds);

        long count = -1;
        if (pageNo == 1) {
            count = (Long) Application.createQueryNoFilter(
                    "select count(commentId) from FeedsComment where " + sqlWhere).unique()[0];
            if (count == 0) {
                return RespBody.ok();
            }
        }

        String sql = "select commentId,createdBy,createdOn,modifiedOn,content,images,attachments" +
                " from FeedsComment where " + sqlWhere + " order by createdOn desc";
        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            list.add(item);
        }

        return RespBody.ok(JSONUtils.toJSONObject(
                new String[] { "total", "data" }, new Object[] { count, list }));
    }

    private JSONObject buildBase(Object[] o, ID user) {
        JSONObject item = new JSONObject();
        item.put("id", o[0]);
        item.put("self", o[1].equals(user));
        item.put("createdBy", new Object[]{o[1], UserHelper.getName((ID) o[1])});
        item.put("createdOn", I18nUtils.formatDate((Date) o[2]));
        item.put("modifedOn", I18nUtils.formatDate((Date) o[3]));
        item.put("content", FeedsHelper.formatContent((String) o[4]));
        if (o[5] != null) {
            item.put("images", JSON.parse((String) o[5]));
        }
        if (o[6] != null) {
            item.put("attachments", JSON.parse((String) o[6]));
        }

        int numLike = FeedsHelper.getNumOfLike((ID) o[0]);
        item.put("numLike", numLike);
        if (numLike > 0) {
            item.put("myLike", FeedsHelper.isMyLike((ID) o[0], user));
        }
        return item;
    }
}
