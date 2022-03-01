/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.service.feeds;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.privileges.UserHelper;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author devezhao
 * @since 2019/11/7
 */
public class FeedsHelper {

    /**
     * URL 提取
     */
    public static final Pattern URL_PATTERN = Pattern.compile("((www|https?://)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]{7,300})");

    /**
     * 评论数
     * TODO 缓存
     *
     * @param feedsId
     * @return
     */
    public static int getNumOfComment(ID feedsId) {
        Object[] c = Application.createQueryNoFilter(
                "select count(commentId) from FeedsComment where feedsId = ?")
                .setParameter(1, feedsId)
                .unique();
        return c == null ? 0 : ObjectUtils.toInt(c[0]);
    }

    /**
     * 点赞数
     * TODO 缓存
     *
     * @param feedsOrComment
     * @return
     */
    public static int getNumOfLike(ID feedsOrComment) {
        Object[] c = Application.createQueryNoFilter(
                "select count(likeId) from FeedsLike where source = ?")
                .setParameter(1, feedsOrComment)
                .unique();
        return c == null ? 0 : ObjectUtils.toInt(c[0]);
    }

    /**
     * 指定用户是否点赞
     *
     * @param feedsOrComment
     * @param user           指定用户
     * @return
     */
    public static boolean isMyLike(ID feedsOrComment, ID user) {
        Object[] c = Application.createQueryNoFilter(
                "select likeId from FeedsLike where source = ? and createdBy = ?")
                .setParameter(1, feedsOrComment)
                .setParameter(2, user)
                .unique();
        return c != null;
    }

    /**
     * 获取内容中的 @USERID
     *
     * @param content
     * @return
     * @see #findMentionsMap(String)
     */
    public static ID[] findMentions(String content) {
        Set<ID> set = new HashSet<>(findMentionsMap(content).values());
        return set.toArray(new ID[0]);
    }

    /**
     * 获取内容中的 @USERID
     *
     * @param content
     * @return Returns Map<@NAME, @ID>
     */
    public static Map<String, ID> findMentionsMap(String content) {
        Map<String, ID> found = new HashMap<>();
        for (String ats : content.split("@")) {
            if (StringUtils.isBlank(ats)) continue;
            String[] atsList = ats.split("\\s");

            String fullName = atsList[0];
            // 全名
            ID user = UserHelper.findUserByFullName(fullName);
            // 用户名
            if (user == null && Application.getUserStore().existsName(fullName)) {
                user = Application.getUserStore().getUser(fullName).getId();
            }

            // 兼容全名中有1个空格
            if (user == null && atsList.length >= 2) {
                fullName = atsList[0] + " " + atsList[1];
                user = UserHelper.findUserByFullName(fullName);
            }

            if (user != null) {
                found.put(fullName, user);
            }
        }
        return found;
    }

    /**
     * 用户对指定动态是否可读
     *
     * @param feedsOrComment
     * @param user
     * @return
     */
    public static boolean checkReadable(ID feedsOrComment, ID user) {
        String sql = "select scope,createdBy from Feeds where feedsId = ?";
        if (feedsOrComment.getEntityCode() == EntityHelper.FeedsComment) {
            sql = "select feedsId.scope,feedsId.createdBy from FeedsComment where commentId = ?";
        }

        Object[] o = Application.createQueryNoFilter(sql).setParameter(1, feedsOrComment).unique();
        if (o == null) {
            return false;
        }
        if (o[1].equals(user) || o[0].equals(FeedsScope.ALL.name())) {
            return true;  // 自己 & 公开
        }

        // 团队
        if (ID.isId(o[0])) {
            Team team = Application.getUserStore().getTeam(ID.valueOf((String) o[0]));
            return team.isMember(user);
        }
        return false;
    }

    /**
     * 格式化动态内容
     *
     * @param content
     * @return
     */
    public static String formatContent(String content) {
        return formatContent(content, true);
    }

    /**
     * 格式化动态内容
     *
     * @param content
     * @param xss 是否处理 XSS
     * @return
     */
    public static String formatContent(String content, boolean xss) {
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        Set<String> urls = new HashSet<>();
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            urls.add(url);
        }

        if (xss) content = CommonsUtils.escapeHtml(content);

        if (!urls.isEmpty()) {
            // 排序: 长 -> 短
            List<String> setList = new ArrayList<>(urls);
            setList.sort((o1, o2) -> {
                int len1 = o1.length();
                int len2 = o2.length();
                return Integer.compare(len2, len1);
            });

            Map<String, String> xurlMap = new HashMap<>();
            for (String url : setList) {
                String x = UUID.randomUUID().toString();
                xurlMap.put(x, url);

                String safeUrl = AppUtils.getContextPath("/commons/url-safe?url=" + x);
                content = content.replace(url,
                        String.format("<a href=\"%s\" target=\"_blank\">%s</a>", safeUrl, x));
            }

            for (Map.Entry<String, String> e : xurlMap.entrySet()) {
                content = content
                        .replace("url=" + e.getKey(), "url=" + CodecUtils.urlEncode(e.getValue()))
                        .replace(e.getKey(), e.getValue());
            }
        }

        Matcher atMatcher = MessageBuilder.AT_PATTERN.matcher(content);
        while (atMatcher.find()) {
            String at = atMatcher.group();
            ID user = ID.valueOf(at.substring(1));
            if (user.getEntityCode() == EntityHelper.User && Application.getUserStore().existsUser(user)) {
                String fullName = Application.getUserStore().getUser(user).getFullName();
                content = content.replace(at,
                        String.format("<a data-uid=\"%s\">@%s</a>", user, fullName));
            }
        }

        return content;
    }
}
