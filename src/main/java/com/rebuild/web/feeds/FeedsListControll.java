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
import com.rebuild.server.business.feeds.FeedsScope;
import com.rebuild.server.business.feeds.FeedsType;
import com.rebuild.server.service.bizz.UserHelper;
import com.rebuild.server.service.query.AdvFilterParser;
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

/**
 * TODO
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

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 20);
        String sort = getParameter(request,"sort");

        String sql = "select feedsId,createdBy,createdOn,modifiedOn,content,attachment,scope,type,relatedRecord from Feeds";
        if (sqlWhere != null) {
            sql += " where " + sqlWhere;
        }
        if ("older".equalsIgnoreCase(sort)) sql += " order by createdOn asc";
        else if ("modified".equalsIgnoreCase(sort)) sql += " order by modifiedOn desc";
        else sql += " order by createdOn desc";

        Object[][] array = Application.getQueryFactory().createQuery(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> ret = new ArrayList<>();
        for (Object[] o : array) {
            JSONObject item = buildBase(o, user);

            item.put("scope", FeedsScope.parse((String) o[6]).getName());
            item.put("type", FeedsType.parse((Integer) o[7]).getName());
            item.put("releated", o[8]);

            item.put("numLike", 0);
            item.put("numComments", 0);

            ret.add(item);
        }
        writeSuccess(response, ret);
    }

    @RequestMapping("/feeds/comments-list")
    public void fetchComments(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ID user = getRequestUser(request);

        int pageNo = getIntParameter(request, "pageNo", 1);
        int pageSize = getIntParameter(request, "pageSize", 20);

        String sql = "select commentId,createdBy,createdOn,modifiedOn,content,attachment from FeedsComment order by createdOn desc";
        Object[][] array = Application.getQueryFactory().createQuery(sql)
                .setLimit(pageSize, pageNo * pageSize - pageSize)
                .array();

        List<JSON> ret = new ArrayList<>();
        for (Object[] o : array) {
            ret.add(buildBase(o, user));
        }
        writeSuccess(response, ret);
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
        item.put("content", o[4]);
        item.put("attachment", o[5]);
        return item;
    }
}
