/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.TestSupport;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelperTest extends TestSupport {

    @Test
    public void findMentions() {
        Map<String, ID> map = FeedsHelper.findMentionsMap("@RB示例用户 @没有 @RB 示例用户 @超级管理员\n你还的呵呵我复合 @ @  ");
        System.out.println(map);
    }

    @Test
    public void getNumOfComment() {
        UserContextHolder.setUser(UserService.ADMIN_USER);

        ID feedsId = createFeeds();
        createComment(feedsId);

        int num = FeedsHelper.getNumOfComment(feedsId);
        Assertions.assertEquals(1, num);
        Application.getService(EntityHelper.Feeds).delete(feedsId);

        FeedsHelper.isMyLike(feedsId, SIMPLE_USER);
        FeedsHelper.getNumOfLike(feedsId);
        FeedsHelper.checkReadable(feedsId, SIMPLE_USER);
    }

    @Test
    public void formatContent() {
        FeedsHelper.formatContent("123 @" + UserService.ADMIN_USER);
    }

    private ID createFeeds() {
        Record feeds = EntityHelper.forNew(EntityHelper.Feeds, SIMPLE_USER);
        feeds.setString("content", "你好，测试动态 @RB示例用户 @admin");
        return Application.getService(EntityHelper.Feeds).create(feeds).getPrimary();
    }

    private void createComment(ID feedsId) {
        Record comment = EntityHelper.forNew(EntityHelper.FeedsComment, SIMPLE_USER);
        comment.setString("content", "你好，测试评论");
        comment.setID("feedsId", feedsId);
        Application.getService(EntityHelper.FeedsComment).create(comment).getPrimary();
    }
}