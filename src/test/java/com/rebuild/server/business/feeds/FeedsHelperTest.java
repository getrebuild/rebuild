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

package com.rebuild.server.business.feeds;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.TestSupportWithUser;
import com.rebuild.server.metadata.EntityHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelperTest extends TestSupportWithUser {

    @Test
    public void findMentions() {
        Map<String, ID> map = FeedsHelper.findMentionsMap("@RB示例用户 @超级管理员 @没有 @RB 示例用户 你还的呵呵我复合 @ @  ");
        System.out.println(map);
    }

    @Test
    public void getNumOfComment() {
        ID feedsId = createFeeds();
        createComment(feedsId);

        int num = FeedsHelper.getNumOfComment(feedsId);
        Assert.assertTrue(num == 1);
        Application.getService(EntityHelper.Feeds).delete(feedsId);
    }

    private ID createFeeds() {
        Record feeds = EntityHelper.forNew(EntityHelper.Feeds, SIMPLE_USER);
        feeds.setString("content", "你好，测试动态 @RB示例用户 @admin");
        return Application.getService(EntityHelper.Feeds).create(feeds).getPrimary();
    }

    private ID createComment(ID feedsId) {
        Record comment = EntityHelper.forNew(EntityHelper.FeedsComment, SIMPLE_USER);
        comment.setString("content", "你好，测试评论");
        comment.setID("feedsId", feedsId);
        return Application.getService(EntityHelper.FeedsComment).create(comment).getPrimary();
    }
}