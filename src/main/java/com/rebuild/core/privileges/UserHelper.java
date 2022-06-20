/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.core.Application;
import com.rebuild.core.RebuildException;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.MetadataHelper;
import com.rebuild.core.privileges.bizz.CombinedRole;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.general.N2NReferenceSupport;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.*;

/**
 * 用户帮助类
 *
 * @author devezhao
 * @since 10/14/2018
 */
@Slf4j
public class UserHelper {

    // 默认头像
    public static final String DEFAULT_AVATAR = "/assets/img/avatar.png";

    /**
     * 是否管理员
     *
     * @param userId
     * @return
     */
    public static boolean isAdmin(ID userId) {
        try {
            return Application.getUserStore().getUser(userId).isAdmin();
        } catch (NoMemberFoundException ex) {
            log.error("No User found : " + userId);
        }
        return false;
    }

    /**
     * 是否超级管理员
     *
     * @param userId
     * @return
     */
    public static boolean isSuperAdmin(ID userId) {
        return UserService.ADMIN_USER.equals(userId) || UserService.SYSTEM_USER.equals(userId);
    }

    /**
     * 是否激活
     *
     * @param bizzId ID of User/Role/Department/Team
     * @return
     */
    public static boolean isActive(ID bizzId) {
        try {
            switch (bizzId.getEntityCode()) {
                case EntityHelper.User:
                    return Application.getUserStore().getUser(bizzId).isActive();
                case EntityHelper.Department:
                    return !Application.getUserStore().getDepartment(bizzId).isDisabled();
                case EntityHelper.Role:
                    return !Application.getUserStore().getRole(bizzId).isDisabled();
                case EntityHelper.Team:
                    return !Application.getUserStore().getTeam(bizzId).isDisabled();
            }

        } catch (NoMemberFoundException ex) {
            log.error("No bizz found : " + bizzId);
        }
        return false;
    }

    /**
     * 获取用户部门
     *
     * @param userId
     * @return
     */
    public static Department getDepartment(ID userId) {
        try {
            User u = Application.getUserStore().getUser(userId);
            return u.getOwningDept();
        } catch (NoMemberFoundException ex) {
            log.error("No User found : " + userId);
        }
        return null;
    }

    /**
     * 获取所有子部门ID（包括自己）
     *
     * @param parent
     * @return
     */
    public static Set<ID> getAllChildren(Department parent) {
        Set<ID> children = new HashSet<>();
        children.add((ID) parent.getIdentity());
        for (BusinessUnit child : parent.getAllChildren()) {
            children.add((ID) child.getIdentity());
        }
        return children;
    }

    /**
     * 获取名称
     *
     * @param bizzId ID of User/Role/Department/Team
     * @return
     */
    public static String getName(ID bizzId) {
        try {
            switch (bizzId.getEntityCode()) {
                case EntityHelper.User:
                    return Application.getUserStore().getUser(bizzId).getFullName();
                case EntityHelper.Department:
                    return Application.getUserStore().getDepartment(bizzId).getName();
                case EntityHelper.Role:
                    return Application.getUserStore().getRole(bizzId).getName();
                case EntityHelper.Team:
                    return Application.getUserStore().getTeam(bizzId).getName();
            }

        } catch (NoMemberFoundException ex) {
            log.error("No bizz found : " + bizzId);
        }
        return null;
    }

    /**
     * 获取部门或角色下的成员
     *
     * @param groupId ID of Role/Department/Team
     * @return
     */
    public static Member[] getMembers(ID groupId) {
        Set<Principal> ms = null;
        try {
            switch (groupId.getEntityCode()) {
                case EntityHelper.Department:
                    ms = Application.getUserStore().getDepartment(groupId).getMembers();
                    break;
                case EntityHelper.Role:
                    ms = Application.getUserStore().getRole(groupId).getMembers();
                    break;
                case EntityHelper.Team:
                    ms = Application.getUserStore().getTeam(groupId).getMembers();
                    break;
                default:
                    break;
            }

        } catch (NoMemberFoundException ex) {
            log.error("No group found : " + groupId);
        }

        if (ms == null || ms.isEmpty()) {
            return new Member[0];
        }
        //noinspection SuspiciousToArrayCall
        return ms.toArray(new Member[0]);
    }

    /**
     * @param userDefs
     * @param record
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(JSONArray userDefs, ID record) {
        return parseUsers(userDefs, record, false);
    }

    /**
     * @param userDefs
     * @param record
     * @param filterDisabled
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(JSONArray userDefs, ID record, boolean filterDisabled) {
        if (userDefs == null) return Collections.emptySet();

        Set<String> users = new HashSet<>();
        for (Object u : userDefs) {
            users.add((String) u);
        }
        return parseUsers(users, record, filterDisabled);
    }

    /**
     * @param userDefs
     * @param record
     * @return
     * @see #parseUsers(Collection, ID, boolean)
     */
    public static Set<ID> parseUsers(Collection<String> userDefs, ID record) {
        return parseUsers(userDefs, record, false);
    }

    /**
     * 解析用户列表
     *
     * @param userDefs
     * @param recordId
     * @param filterDisabled
     * @return
     */
    public static Set<ID> parseUsers(Collection<String> userDefs, ID recordId, boolean filterDisabled) {
        Entity entity = recordId == null ? null : MetadataHelper.getEntity(recordId.getEntityCode());

        Set<ID> bizzs = new HashSet<>();
        Set<String> useFields = new HashSet<>();
        for (String def : userDefs) {
            if (ID.isId(def)) {
                bizzs.add(ID.valueOf(def));
            } else if (entity != null && MetadataHelper.getLastJoinField(entity, def) != null) {
                useFields.add(def);
            } else {
                log.warn("Invalid id or field : " + def);
            }
        }

        if (!useFields.isEmpty()) {
            String sql = String.format("select %s from %s where %s = ?",
                    StringUtils.join(useFields, ","), entity.getName(), entity.getPrimaryField().getName());
            Record bizzValue = Application.createQueryNoFilter(sql).setParameter(1, recordId).record();

            if (bizzValue != null) {
                for (String field : bizzValue.getAvailableFields()) {
                    Object value = bizzValue.getObjectValue(field);
                    if (value == null) continue;

                    if (value instanceof ID[]) {
                        CollectionUtils.addAll(bizzs, N2NReferenceSupport.items(field, recordId));
                    } else {
                        bizzs.add((ID) value);
                    }
                }
            }
        }

        Set<ID> users = new HashSet<>();
        for (ID bizz : bizzs) {
            if (bizz.getEntityCode() == EntityHelper.User) {
                users.add(bizz);
            } else {
                Member[] ms = getMembers(bizz);
                for (Member m : ms) {
                    if (m.getIdentity().equals(UserService.SYSTEM_USER)) continue;
                    users.add((ID) m.getIdentity());
                }
            }
        }

        // 过滤禁用用户
        if (filterDisabled) {
            for (Iterator<ID> iter = users.iterator(); iter.hasNext(); ) {
                User u = Application.getUserStore().getUser(iter.next());
                if (!u.isActive()) iter.remove();
            }
        }

        return users;
    }

    private static final Color[] RB_COLORS = new Color[]{
            new Color(66, 133, 244),
            new Color(52, 168, 83),
            new Color(251, 188, 5),
            new Color(234, 67, 53),
            new Color(155, 82, 222),
            new Color(22, 168, 143),
    };

    /**
     * 生成用户头像
     *
     * @param name
     * @param forceMake
     * @return
     */
    public static File generateAvatar(String name, boolean forceMake) {
        if (StringUtils.isBlank(name)) name = "RB";
        File avatarFile = RebuildConfiguration.getFileOfData("avatar-" + name + "29.jpg");
        if (avatarFile.exists()) {
            if (forceMake) {
                FileUtils.deleteQuietly(avatarFile);
            } else {
                return avatarFile;
            }
        }

        if (name.length() > 2) {
            name = name.substring(name.length() - 2);
        }
        name = name.toUpperCase();

        BufferedImage bi = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) bi.getGraphics();

        g2d.setColor(RB_COLORS[RandomUtils.nextInt(RB_COLORS.length)]);
        g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        try {
            final Font font = createFont();
            g2d.setFont(font);
            g2d.setColor(Color.WHITE);
            FontMetrics fontMetrics = g2d.getFontMetrics(font);
            int x = fontMetrics.stringWidth(name);
            g2d.drawString(name, (200 - x) / 2, 128);
            g2d.setColor(new Color(0, 0, 0, 1));
            g2d.drawString("wbr", 0, 62);
            g2d.dispose();

            try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                ImageIO.write(bi, "png", fos);
                fos.flush();
            }

        } catch (Throwable ex) {
            log.warn("Cannot make font-avatar : {}", name, ex);

            InputStream is = null;
            try {
                is = CommonsUtils.getStreamOfRes("/web" + DEFAULT_AVATAR);

                bi = ImageIO.read(is);
                try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                    ImageIO.write(bi, "png", fos);
                    fos.flush();
                }

            } catch (IOException ignored) {
                IOUtils.closeQuietly(is);
            }
        }

        return avatarFile;
    }

    private static Font createFont() {
        File fontFile = RebuildConfiguration.getFileOfData("SourceHanSansK-Regular.ttf");
        if (fontFile.exists()) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                font = font.deriveFont((float) 81.0);
                return font;
            } catch (Throwable ex) {
                log.warn("Cannot create Font: SourceHanSansK-Regular.ttf", ex);
            }
        }
        // Use default
        return new Font("SimHei", Font.BOLD, (int) (float) 81.0);
    }

    /**
     * 通过用户全称找用户（注意同名问题）
     *
     * @param fullName
     * @return
     */
    public static ID findUserByFullName(String fullName) {
        for (User u : Application.getUserStore().getAllUsers()) {
            if (fullName.equalsIgnoreCase(u.getFullName())) {
                return u.getId();
            }
        }
        return null;
    }

    /**
     * @return
     * @see #sortUsers(boolean)
     */
    public static User[] sortUsers() {
        return sortUsers(false);
    }

    /**
     * 按全称排序的用户列表
     *
     * @param isAll 是否包括未激活用户
     * @return
     * @see UserStore#getAllUsers()
     */
    public static User[] sortUsers(boolean isAll) {
        User[] users = Application.getUserStore().getAllUsers();
        // 排除未激活
        if (!isAll) {
            List<User> list = new ArrayList<>();
            for (User u : users) {
                if (u.isActive()) {
                    list.add(u);
                }
            }
            users = list.toArray(new User[0]);
        }

        sortMembers(users);
        return users;
    }

    /**
     * 成员排序
     *
     * @param members
     * @return
     */
    public static Member[] sortMembers(Member[] members) {
        if (members == null || members.length == 0) {
            return new Member[0];
        }

        if (members[0] instanceof User) {
            Arrays.sort(members, Comparator.comparing(o -> ((User) o).getFullName()));
        } else {
            Arrays.sort(members, Comparator.comparing(Member::getName));
        }
        return members;
    }

    /**
     * 获取用户的附加角色
     *
     * @param user
     * @return
     * @see CombinedRole
     */
    public static Set<ID> getRoleAppends(ID user) {
        Role role = Application.getUserStore().getUser(user).getOwningRole();
        if (role instanceof CombinedRole) {
            return ((CombinedRole) role).getRoleAppends();
        }
        return null;
    }

    /**
     * 获取附加了指定角色的用户
     *
     * @param roleId
     * @return
     */
    public static Set<ID> getRoleMembers(ID roleId) {
        Object[][] array = Application.createQueryNoFilter(
                "select userId from RoleMember where roleId = ?")
                .setParameter(1, roleId)
                .array();

        Set<ID> set = new HashSet<>();
        for (Object[] o : array) set.add((ID) o[0]);
        return set;
    }

    /**
     * 是否是自己的（管理员有特殊处理）
     *
     * @param user
     * @param otherUserOrAnyRecordId
     * @return
     */
    public static boolean isSelf(ID user, ID otherUserOrAnyRecordId) {
        ID createdBy = otherUserOrAnyRecordId;
        if (otherUserOrAnyRecordId.getEntityCode() != EntityHelper.User) {
            createdBy = getCreatedBy(otherUserOrAnyRecordId);
            if (createdBy == null) return false;
        }

        if (createdBy.equals(user)) return true;

        // 所有管理员被视为同一用户
        return isAdmin(createdBy) && isAdmin(user);
    }

    private static ID getCreatedBy(ID anyRecordId) {
        final String ckey = "CreatedBy-" + anyRecordId;
        ID createdBy = (ID) Application.getCommonsCache().getx(ckey);
        if (createdBy != null) {
            return createdBy;
        }

        Entity entity = MetadataHelper.getEntity(anyRecordId.getEntityCode());
        if (!entity.containsField(EntityHelper.CreatedBy)) {
            log.warn("No [createdBy] field in [{}]", entity.getEntityCode());
            return null;
        }

        Object[] c = Application.getQueryFactory().uniqueNoFilter(anyRecordId, EntityHelper.CreatedBy);
        if (c == null) {
            throw new RebuildException("No record found : " + anyRecordId);
        }

        createdBy = (ID) c[0];
        Application.getCommonsCache().putx(ckey, createdBy);
        return createdBy;
    }
}
