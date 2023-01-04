/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.AccessDeniedException;
import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.commons.CodecUtils;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.commons.ObjectUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.UserContextHolder;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.RecordBuilder;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.service.BaseService;
import com.rebuild.core.service.DataSpecificationException;
import com.rebuild.core.service.notification.Message;
import com.rebuild.core.service.notification.MessageBuilder;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.License;
import com.rebuild.core.support.NeedRbvException;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.core.support.task.TaskExecutors;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.BlockList;
import com.rebuild.utils.CommonsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * for User
 *
 * @author Zixin (RB)
 * @since 07/25/2018
 */
@Slf4j
@Service
public class UserService extends BaseService {

    // 系统用户
    public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
    // 管理员
    public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");

    // 全部用户（注意这是一个虚拟用户 ID，并不真实存在）
    public static final ID ALLUSERS = ID.valueOf("001-9999999999999999");

    protected UserService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.User;
    }

    @Override
    public Record create(Record record) {
        return create(record, true);
    }

    /**
     * @param record
     * @param notifyUser
     * @return
     */
    private Record create(Record record, boolean notifyUser) {
        if (!License.isCommercial() && Application.getUserStore().getAllUsers().length >= 100) {
            throw new NeedRbvException("用户数量超出免费版限制");
        }

        checkAdminGuard(BizzPermission.CREATE, null);

        final String passwd = record.getString("password");
        saveBefore(record);
        record = super.create(record);
        Application.getUserStore().refreshUser(record.getPrimary());

        if (notifyUser) {
            notifyNewUser(record, passwd);
        }
        return record;
    }

    @Override
    public Record update(Record record) {
        checkAdminGuard(BizzPermission.UPDATE, record.getPrimary());

        saveBefore(record);
        Record r = super.update(record);
        Application.getUserStore().refreshUser(record.getPrimary());

        // @see #getPasswdExpiredDayLeft
        if (record.hasValue("password")) {
            String key = ConfigurationItem.PasswordExpiredDays.name() + r.getPrimary();
            RebuildConfiguration.setCustomValue(key,
                    CalendarUtils.getUTCDateTimeFormat().format(CalendarUtils.now()));
        }

        return r;
    }

    @Override
    public int delete(ID record) {
        checkAdminGuard(BizzPermission.DELETE, null);

        if (ADMIN_USER.equals(record) || SYSTEM_USER.equals(record)) {
            throw new OperationDeniedException(Language.L("内置用户禁止删除"));
        }

        Object[] hasLogin = Application.createQueryNoFilter(
                "select count(logId) from LoginLog where user = ?")
                .setParameter(1, record)
                .unique();
        if (ObjectUtils.toInt(hasLogin[0]) > 0) {
            throw new OperationDeniedException(Language.L("已使用过的用户禁止删除"));
        }

        super.delete(record);
        Application.getUserStore().removeUser(record);
        return 1;
    }

    /**
     * @param record
     */
    protected void saveBefore(Record record) {
        if (record.hasValue("loginName")) {
            checkLoginName(record.getString("loginName"));
        }

        if (record.hasValue("password")) {
            String password = record.getString("password");
            checkPassword(password);
            record.setString("password", EncryptUtils.toSHA256Hex(password));
        }

        if (record.hasValue("email") && Application.getUserStore().existsUser(record.getString("email"))) {
            throw new DataSpecificationException(Language.L("邮箱已存在"));
        }

        if (record.getPrimary() == null && !record.hasValue("fullName")) {
            record.setString("fullName", record.getString("loginName").toUpperCase());
        }

        if (record.hasValue("fullName")) {
            UserHelper.generateAvatar(record.getString("fullName"), true);
        }
    }

    /**
     * @param loginName
     * @throws DataSpecificationException
     */
    private void checkLoginName(String loginName) throws DataSpecificationException {
        if (loginName.length() < 4) {
            throw new DataSpecificationException(Language.L("用户名不能小于 4 位"));
        }

        if (Application.getUserStore().existsUser(loginName)) {
            throw new DataSpecificationException(Language.L("用户名已存在"));
        }

        if (!CommonsUtils.isPlainText(loginName) || BlockList.isBlock(loginName)) {
            throw new DataSpecificationException(Language.L("用户名无效"));
        }
    }

    /**
     * @param action
     * @param user
     * @see AdminGuard
     */
    private void checkAdminGuard(Permission action, ID user) {
        ID currentUser = UserContextHolder.getUser();
        if (UserHelper.isAdmin(currentUser)) return;

        if (action == BizzPermission.CREATE || action == BizzPermission.DELETE) {
            throw new AccessDeniedException(Language.L("无操作权限"));
        }

        // 用户可自己改自己
        if (action == BizzPermission.UPDATE && currentUser.equals(user)) return;

        throw new AccessDeniedException(Language.L("无操作权限"));
    }

    /**
     * 检查密码是否符合安全策略
     *
     * @param password
     * @throws DataSpecificationException
     */
    protected void checkPassword(String password) throws DataSpecificationException {
        if (password.length() < 6) {
            throw new DataSpecificationException(Language.L("密码不能小于 6 位"));
        }

        int policy = RebuildConfiguration.getInt(ConfigurationItem.PasswordPolicy);
        if (policy <= 1) {
            return;
        }

        int countUpper = 0;
        int countLower = 0;
        int countDigit = 0;
        int countSpecial = 0;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                countUpper++;
            } else if (Character.isLowerCase(ch)) {
                countLower++;
            } else if (Character.isDigit(ch)) {
                countDigit++;
            } else if (CommonsUtils.isSpecialChar(ch)) {
                countSpecial++;
            }
        }

        if (countUpper == 0 || countLower == 0 || countDigit == 0) {
            throw new DataSpecificationException(Language.L("密码不能小于 6 位，且必须包含数字和大小写字母"));
        }
        if (policy >= 3 && (countSpecial == 0 || password.length() < 8)) {
            throw new DataSpecificationException(Language.L("密码不能小于 8 位，且必须包含数字和大小写字母及特殊字符"));
        }
    }

    private boolean notifyNewUser(Record newUser, String passwd) {
        if (RebuildConfiguration.getMailAccount() == null || !newUser.hasValue("email")) {
            return false;
        }

        String appName = RebuildConfiguration.get(ConfigurationItem.AppName);
        String homeUrl = RebuildConfiguration.getHomeUrl();

        LanguageBundle bundle = Language.getSysDefaultBundle();
        String content = bundle.L(
                "系统管理员已经为你开通了 %s 账号！以下为你的登录信息，请妥善保管。 [] 登录账号 : **%s** [] 登录密码 : **%s** [] 登录地址 : [%s](%s) [][] 首次登陆，建议你立即修改登陆密码。修改方式 : 登陆后点击右上角头像 - 个人设置 - 安全设置 - 更改密码",
                appName, newUser.getString("loginName"), passwd, homeUrl, homeUrl);

        SMSender.sendMailAsync(newUser.getString("email"), Language.L("你的账号已就绪"), content);
        return true;
    }

    /**
     * xxxNew 值为 null 表示不做修改
     *
     * @param user
     * @param deptNew     新部门
     * @param roleNew     新角色
     * @param roleAppends 附加角色
     * @param enableNew   激活状态
     */
    public void updateEnableUser(ID user, ID deptNew, ID roleNew, ID[] roleAppends, Boolean enableNew) {
        User enUser = Application.getUserStore().getUser(user);
        // 当前是从未激活状态
        final boolean beforeUnEnabled = enUser.isDisabled()
                && (enUser.getOwningDept() == null || enUser.getOwningRole() == null);

        ID deptOld = null;
        // 检查是否需要更新部门
        if (deptNew != null) {
            deptOld = enUser.getOwningBizUnit() == null ? null : (ID) enUser.getOwningBizUnit().getIdentity();
            if (deptNew.equals(deptOld)) {
                deptNew = null;
                deptOld = null;
            }
        }

        // 检查是否需要更新角色
        if (enUser.getOwningRole() != null && enUser.getOwningRole().getIdentity().equals(roleNew)) {
            roleNew = null;
        }

        Record record = EntityHelper.forUpdate(user, UserContextHolder.getUser());
        boolean changed = false;
        if (deptNew != null) {
            record.setID("deptId", deptNew);
            changed = true;
        }
        if (roleNew != null) {
            record.setID("roleId", roleNew);
            changed = true;
        }
        if (enableNew != null) {
            record.setBoolean("isDisabled", !enableNew);
            changed = true;
        }

        if (changed) {
            super.update(record);
        }

        if (changed || updateRoleAppends(user, roleAppends)) {
            Application.getUserStore().refreshUser(user);
        }

        // 改变记录的所属部门
        if (deptOld != null) {
            TaskExecutors.submit(new ChangeOwningDeptTask(user, deptNew), UserContextHolder.getUser());
        }

        // 是否需要发送激活通知
        if (beforeUnEnabled) {
            notifyEnableUser(Application.getUserStore().getUser(enUser.getId()));
        }

        // Kill session
        if (enableNew != null && !enableNew) {
            Application.getSessionStore().killSession(user);
        }
    }

    /**
     * @param user
     * @return
     */
    public boolean notifyEnableUser(User user) {
        // 未激活
        if (!user.isActive()) return false;

        // 登录过
        Object did = Application.createQueryNoFilter(
                "select logId from LoginLog where user = ?")
                .setParameter(1, user.getId())
                .unique();
        if (did != null) return false;

        // 站内信
        String content = Language.L("%s 你的账户已激活！现在你可以登陆并使用系统。如有任何登陆或使用问题，请与系统管理员联系。",
                user.getFullName());
        Application.getNotifications().send(MessageBuilder.createMessage(user.getId(), content));

        // 邮件
        if (SMSender.availableMail() && user.getEmail() != null) {
            String homeUrl = RebuildConfiguration.getHomeUrl();
            content = Language.L("%s 你的账户已激活！现在你可以登陆并使用系统。 [][] 登录地址 : [%s](%s) [][] 首次登陆，建议你立即修改密码！如有任何登陆或使用问题，请与系统管理员联系。",
                    user.getFullName(), homeUrl, homeUrl);
            String subject = Language.L("你的账户已激活");
            SMSender.sendMailAsync(user.getEmail(), subject, content);
        }
        return true;
    }

    /**
     * 更新附加角色
     *
     * @param user
     * @param roleAppends
     * @return
     */
    protected boolean updateRoleAppends(ID user, ID[] roleAppends) {
        Object[][] exists = Application.createQueryNoFilter(
                "select memberId,roleId from RoleMember where userId = ?")
                .setParameter(1, user)
                .array();
        if (exists.length == 0 && (roleAppends == null || roleAppends.length == 0)) {
            return false;
        }

        if (roleAppends == null || roleAppends.length == 0) {
            for (Object[] o : exists) {
                super.delete((ID) o[0]);
            }
            return true;
        }

        Map<ID, ID> role2members = new HashMap<>();
        for (Object[] o : exists) {
            role2members.put((ID) o[1], (ID) o[0]);
        }

        for (ID role : roleAppends) {
            if (role2members.remove(role) == null) {
                Record member = RecordBuilder.builder(EntityHelper.RoleMember)
                        .add("roleId", role)
                        .add("userId", user)
                        .build(SYSTEM_USER);
                super.create(member);
            }
        }

        for (ID old : role2members.values()) {
            super.delete(old);
        }
        return true;
    }

    /**
     * 新用户注册
     *
     * @param record
     * @return
     * @throws DataSpecificationException
     */
    public ID txSignUp(Record record) throws DataSpecificationException {
        if (!record.hasValue("password")) {
            record.setString("password", CodecUtils.randomCode(6) + "Rb!8");
        }
        if (!record.hasValue("isDisabled")) {
            record.setBoolean("isDisabled", Boolean.TRUE);
        }

        UserContextHolder.setUser(SYSTEM_USER);
        try {
            record = this.create(record, false);
        } finally {
            UserContextHolder.clearUser();
        }

        // 通知管理员
        ID newUserId = record.getPrimary();
        String viewUrl = AppUtils.getContextPath("/app/list-and-view?id=" + newUserId);
        String content = Language.L(
                "用户 @%s 提交了注册申请。请验证用户有效性后为其指定部门和角色，激活用户登录。如果这是一个无效的申请请忽略。",
                newUserId);
        content += String.format("[%s](%s)", Language.L("点击开始激活"), viewUrl);

        Message message = MessageBuilder.createMessage(ADMIN_USER, content, Message.TYPE_DEFAULT, newUserId);
        Application.getNotifications().send(message);

        return newUserId;
    }

    /**
     * 密码过期时间（剩余天数）
     *
     * @param user
     * @return
     */
    public static Integer getPasswdExpiredDayLeft(ID user) {
        int peDays = RebuildConfiguration.getInt(ConfigurationItem.PasswordExpiredDays);
        if (peDays > 0) {
            String key = ConfigurationItem.PasswordExpiredDays.name() + user;
            String lastChanged = StringUtils.defaultIfBlank(RebuildConfiguration.getCustomValue(key), "2021-06-30");
            int peLeft = -CalendarUtils.getDayLeft(CalendarUtils.parse(lastChanged));
            return peDays - peLeft;
        }
        return null;
    }
}
