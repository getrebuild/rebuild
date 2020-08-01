/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.server.service.bizz;

import cn.devezhao.bizz.privileges.Permission;
import cn.devezhao.bizz.privileges.PrivilegesException;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.member.User;
import cn.devezhao.commons.EncryptUtils;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.helper.BlackList;
import com.rebuild.server.helper.ConfigurableItem;
import com.rebuild.server.helper.SMSender;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.helper.language.Languages;
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.BaseServiceImpl;
import com.rebuild.server.service.notification.Message;
import com.rebuild.server.service.notification.MessageBuilder;
import com.rebuild.utils.AppUtils;
import com.rebuild.utils.CommonsUtils;

/**
 * for User
 * 
 * @author zhaofang123@gmail.com
 * @since 07/25/2018
 */
public class UserService extends BaseServiceImpl {
	
	// 系统用户
	public static final ID SYSTEM_USER = ID.valueOf("001-0000000000000000");
	// 管理员
	public static final ID ADMIN_USER = ID.valueOf("001-0000000000000001");
	
	public UserService(PersistManagerFactory aPMFactory) {
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
		return r;
	}
	
	@Override
	public int delete(ID record) {
    	checkAdminGuard(BizzPermission.DELETE, null);

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
			throw new DataSpecificationException(Languages.lang("Repeated", "Email"));
		}
		
		if (record.getPrimary() == null && !record.hasValue("fullName")) {
			record.setString("fullName", record.getString("loginName").toUpperCase());
		}

		if (record.hasValue("fullName")) {
			try {
				UserHelper.generateAvatar(record.getString("fullName"), true);
			} catch (Exception ex) {
				LOG.error(ex);
			}
		}
	}
	
	/**
	 * @param loginName
	 * @throws DataSpecificationException
	 */
	private void checkLoginName(String loginName) throws DataSpecificationException {
		if (Application.getUserStore().existsUser(loginName)) {
			throw new DataSpecificationException("登陆名重复");
		}
		if (!CommonsUtils.isPlainText(loginName) || BlackList.isBlack(loginName)) {
			throw new DataSpecificationException("无效登陆名");
		}
	}

	/**
	 * @param action
	 * @param user
	 * @see com.rebuild.server.service.bizz.privileges.AdminGuard
	 */
	private void checkAdminGuard(Permission action, ID user) {
		ID currentUser = Application.getCurrentUser();
		if (UserHelper.isAdmin(currentUser)) return;

		if (action == BizzPermission.CREATE || action == BizzPermission.DELETE) {
			throw new PrivilegesException("无操作权限 (E1)");
		}

		// 用户可自己改自己
		if (action == BizzPermission.UPDATE && currentUser.equals(user)) {
			return;
		}
		throw new PrivilegesException("无操作权限 (E1)");
	}

    /**
     * 检查密码是否符合安全策略
     *
     * @param password
     * @throws DataSpecificationException
     */
    protected void checkPassword(String password) throws DataSpecificationException {
        if (password.length() < 6) {
            throw new DataSpecificationException(Languages.lang("PasswordLevel1Tip"));
        }

        int policy = SysConfiguration.getInt(ConfigurableItem.PasswordPolicy);
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
            throw new DataSpecificationException(Languages.lang("PasswordLevel2Tip"));
        }
        if (policy >= 3 && (countSpecial == 0 || password.length() < 8)) {
            throw new DataSpecificationException(Languages.lang("PasswordLevel3Tip"));
        }
    }

	/**
	 * @param newUser
	 * @param passwd
	 * @return
	 */
	private boolean notifyNewUser(Record newUser, String passwd) {
		if (SysConfiguration.getMailAccount() == null || !newUser.hasValue("email")) {
			return false;
		}

        String appName = SysConfiguration.get(ConfigurableItem.AppName);
		String homeUrl = SysConfiguration.getHomeUrl();

        String subject = Languages.defaultBundle().formatLang("YourAccountReady",
                appName);
        String content = Languages.defaultBundle().formatLang("NewUserAddedNotify",
                appName, newUser.getString("loginName"), passwd, homeUrl, homeUrl);

		SMSender.sendMail(newUser.getString("email"), subject, content);
		return true;
	}
	
	/**
	 * xxxNew 值为 null 表示不做修改
	 * 
	 * @param user
	 * @param deptNew
	 * @param roleNew
	 * @param enableNew
	 */
	public void updateEnableUser(ID user, ID deptNew, ID roleNew, Boolean enableNew) {
		User u = Application.getUserStore().getUser(user);
		ID deptOld = null;
		// 检查是否需要更新部门
		if (deptNew != null) {
			deptOld = u.getOwningBizUnit() == null ? null : (ID) u.getOwningBizUnit().getIdentity();
			if (deptNew.equals(deptOld)) {
				deptNew = null;
				deptOld = null;
			}
		}
		
		// 检查是否需要更新角色
		if (u.getOwningRole() != null && u.getOwningRole().getIdentity().equals(roleNew)) {
			roleNew = null;
		}
		
		Record record = EntityHelper.forUpdate(user, Application.getCurrentUser());
		if (deptNew != null) {
			record.setID("deptId", deptNew);
		}
		if (roleNew != null) {
			record.setID("roleId", roleNew);
		}
		if (enableNew != null) {
			record.setBoolean("isDisabled", !enableNew);
		}
		super.update(record);
		Application.getUserStore().refreshUser(user);
		
		// 改变记录的所属部门
		if (deptOld != null) {
			TaskExecutors.submit(new ChangeOwningDeptTask(user, deptNew), Application.getCurrentUser());
		}
	}

    /**
     * 新用户注册
     *
     * @param record
     * @return
     * @throws DataSpecificationException
     */
	public ID txSignUp(Record record) throws DataSpecificationException {
		record = this.create(record, false);

		ID newUserId = record.getPrimary();
		String viewUrl = AppUtils.getContextPath() + "/app/list-and-view?id=" + newUserId;
		String content = Languages.defaultBundle().formatLang("NewUserSignupNotify", newUserId, viewUrl);

		Message message = MessageBuilder.createMessage(ADMIN_USER, content, newUserId);
		Application.getNotifications().send(message);
		return newUserId;
	}
}
