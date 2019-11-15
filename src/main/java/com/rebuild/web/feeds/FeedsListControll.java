/*
rebuild - Building your business-systems freely.
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

package com.rebuild.web.feeds;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.business.feeds.FeedsScope;
import com.rebuild.server.configuration.portals.FieldValueWrapper;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.metadata.entity.EasyMeta;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.server.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 列表相关
 *
 * @author devezhao
 * @since 2019/11/1
 */
@Controller
public class FeedsListControll extends BasePageControll {

    /**
     * @see com.rebuild.server.business.feeds.FeedsType
     */
    @RequestMapping("/feeds/{type}")
    public ModelAndView pageIndex(@PathVariable String type) throws IOException {
        ModelAndView mv = createModelAndView("/feeds/home.jsp");
        mv.getModel().put("feedsType", type);
        return mv;
    }

    @RequestMapping("/feeds/feeds-list")
    public void fetchFeeds(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        String sort = getParameter(request,"sort");

        long count = -1;
        if (pageNo == 1) {
            count = (Long) Application.createQueryNoFilter(
                    "select count(feedsId) from Feeds where " + sqlWhere).unique()[0];
            if (count == 0) {
                writeSuccess(response);
                return;
            }
        }

        String sql = "select feedsId,createdBy,createdOn,modifiedOn,content,images,attachments,scope,type,relatedRecord from Feeds where " + sqlWhere;
        if ("older".equalsIgnoreCase(sort)) sql += " order by createdOn asc";
        else if ("modified".equalsIgnoreCase(sort)) sql += " order by modifiedOn desc";
        else sql += " order by createdOn desc";

        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            FeedsScope scope = FeedsScope.parse((String) o[7]);
            if (scope == FeedsScope.GROUP) {
                Team team = Application.getUserStore().getTeam(ID.valueOf((String) o[7]));
                item.put("scope", new Object[] { team.getIdentity(), team.getName() });
            } else {
                item.put("scope", scope.getName());
            }
            item.put("type", o[8]);
            item.put("numComments", FeedsHelper.getNumOfComment((ID) o[0]));

            ID related = (ID) o[9];
            if (related != null && MetadataHelper.containsEntity(related.getEntityCode())) {
                EasyMeta entity = EasyMeta.valueOf(related.getEntityCode());
                String recordLabel = FieldValueWrapper.getLabelNotry(related);
                item.put("related", new Object[] { related, recordLabel, entity.getLabel(), entity.getIcon(), entity.getName() });
            }
            list.add(item);
        }
        writeSuccess(response,
                JSONUtils.toJSONObject(new String[] { "total", "data" }, new Object[] { count, list }));
    }

    @RequestMapping("/feeds/comments-list")
    public void fetchComments(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
                writeSuccess(response);
                return;
            }
        }

        String sql = "select commentId,createdBy,createdOn,modifiedOn,content,images,attachments from FeedsComment where " + sqlWhere;
        sql += " order by createdOn desc";
        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            list.add(item);
        }
        writeSuccess(response,
                JSONUtils.toJSONObject(new String[] { "total", "data" }, new Object[] { count, list }));
    }

    /**
     * @param o
     * @param user
     * @return
     */
    private JSONObject buildBase(Object[] o, ID user) {
        JSONObject item = new JSONObject();
        item.put("id", o[0]);
        item.put("self", o[1].equals(user));
        item.put("createdBy", new Object[] { o[1], UserHelper.getName((ID) o[1]) });
        item.put("createdOn", CalendarUtils.getUTCDateTimeFormat().format(o[2]));
        item.put("createdOnFN", Moment.moment((Date) o[2]).fromNow());
        item.put("modifedOn", CalendarUtils.getUTCDateTimeFormat().format(o[3]));
        item.put("content", formatContent((String) o[4]));
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

    /**
     * @param content
     * @return
     */
    private String formatContent(String content) {
        Matcher atMatcher = MessageBuilder.AT_PATTERN.matcher(content);
        while (atMatcher.find()) {
            String at = atMatcher.group();
            ID user = ID.valueOf(at.substring(1));
            if (user.getEntityCode() == EntityHelper.User && Application.getUserStore().exists(user)) {
                String fullName = Application.getUserStore().getUser(user).getFullName();
                content = content.replace(at, String.format("<a data-id=\"%s\">@%s</a>", user, fullName));
            }
        }
        return content;
    }
}
