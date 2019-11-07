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

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.momentjava.Moment;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import com.rebuild.server.business.feeds.FeedsHelper;
import com.rebuild.server.business.feeds.FeedsScope;
import com.rebuild.server.business.feeds.FeedsType;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.server.service.query.AdvFilterParser;
import com.rebuild.utils.JSONUtils;
import com.rebuild.web.BasePageControll;
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
        String sqlWhere = new AdvFilterParser((JSONObject) filter).toSqlWhere();
        if (sqlWhere == null) {
            sqlWhere = "(1=1)";
        }

        int type = getIntParameter(request, "type", 0);
        if (type == 10) {
            sqlWhere += String.format(" and createdBy ='%s'", user);
        } else if (type == 2) {
            sqlWhere += String.format(" and feedsId in (select feedsId from FeedsComment where createdBy = '%s')", user);
        } else if (type == 3) {
            sqlWhere += String.format(" and feedsId in (select source from FeedsLike where createdBy = '%s')", user);
        } else if (type == 1) {
            sqlWhere += String.format(" and feedsId in (select source from FeedsMention where user = '%s')", user);
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

        String sql = "select feedsId,createdBy,createdOn,modifiedOn,content,attachment,scope,type,relatedRecord from Feeds where " + sqlWhere;
        if ("older".equalsIgnoreCase(sort)) sql += " order by createdOn asc";
        else if ("modified".equalsIgnoreCase(sort)) sql += " order by modifiedOn desc";
        else sql += " order by createdOn desc";

        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            item.put("scope", FeedsScope.parse((String) o[6]).getName());
            item.put("type", FeedsType.parse((Integer) o[7]).getName());
            item.put("releated", o[8]);
            item.put("numLike", FeedsHelper.getNumOfLike((ID) o[0]));
            item.put("numComments", FeedsHelper.getNumOfComment((ID) o[0]));
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

        String sql = "select commentId,createdBy,createdOn,modifiedOn,content,attachment from FeedsComment where " + sqlWhere;
        sql += " order by createdOn desc";
        Object[][] array = Application.createQueryNoFilter(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> list = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);
            item.put("numLike", FeedsHelper.getNumOfLike((ID) o[0]));
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
        item.put("createdOn", Moment.moment((Date) o[2]).fromNow());
        item.put("content", formatContent((String) o[4]));
        item.put("attachment", o[5]);
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
