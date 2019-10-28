/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.bizz;

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
import com.rebuild.server.helper.task.TaskExecutors;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.DataSpecificationException;
import com.rebuild.server.service.SystemEntityService;
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
public class UserService extends SystemEntityService {
	
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
		final String passwd = record.getString("password");
		saveBefore(record);
		Record r = super.create(record);
		Application.getUserStore().refreshUser(record.getPrimary());
		notifyNewUser(r, passwd);
		return r;
	}
	
	@Override
	public Record update(Record record) {
		saveBefore(record);
		Record r = super.update(record);
		Application.getUserStore().refreshUser(record.getPrimary());
		return r;
	}
	
	@Override
	public int delete(ID record) {
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
		
		if (record.hasValue("email") && Application.getUserStore().exists(record.getString("email"))) {
			throw new DataSpecificationException("邮箱重复");
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
		if (Application.getUserStore().exists(loginName)) {
			throw new DataSpecificationException("登陆名重复");
		}
		if (!CommonsUtils.isPlainText(loginName) || BlackList.isBlack(loginName)) {
			throw new DataSpecificationException("无效登陆名");
		}
	}

    /**
     * 检查密码是否符合安全策略
     *
     * @param password
     * @throws DataSpecificationException
     */
    public void checkPassword(String password) throws DataSpecificationException {
        if (password.length() < 6) {
            throw new DataSpecificationException("密码不能小于6位");
        }

        int policy = (int) SysConfiguration.getLong(ConfigurableItem.PasswordPolicy);
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

        if (policy >= 2 && (countUpper == 0 || countLower == 0 || countDigit == 0)) {
            throw new DataSpecificationException("密码必须包含数字和大小写字母");
        }
        if (policy >= 3 && (countSpecial == 0 || password.length() < 8)) {
            throw new DataSpecificationException("密码不能小于8位，且必须包含特殊字符");
        }
    }

	private static final String MSG_NEWUSER = "<p>系统管理员已经为你开通了 %s 账号！以下为你的登录信息，请妥善保管。</p><div style='margin:10px 0'>登录账号 <b>%s</b><br>登录密码 <b>%s</b><br>登录地址 <a href='%s'>%s</a></div><p>首次登陆，建议你立即修改登陆密码。修改方式：登陆后点击右上角头像 - 个人设置 - 安全设置 - 更改密码</p>";
	/**
	 * @param newUser
	 * @param passwd
	 * @return
	 */
	private boolean notifyNewUser(Record newUser, String passwd) {
		if (SysConfiguration.getMailAccount() == null || !newUser.hasValue("email")) {
			return false;
		}

		String subject = "你的 " + SysConfiguration.get(ConfigurableItem.AppName) + " 账号已就绪";
		String content = String.format(MSG_NEWUSER,
				SysConfiguration.get(ConfigurableItem.AppName),
				newUser.getString("loginName"), passwd,
				SysConfiguration.getHomeUrl(),
				SysConfiguration.getHomeUrl());

		SMSender.sendMail(newUser.getString("email"), subject, content);
		return true;
	}
	
	/**
	 * 改变部门
	 * 
	 * @param user
	 * @param deptNew
	 * @see #updateEnableUser(ID, ID, ID, Boolean)
	 */
	public void updateDepartment(ID user, ID deptNew) {
		updateEnableUser(user, deptNew, null, null);
	}
	
	/**
	 * 改变角色
	 * 
	 * @param user
	 * @param roleNew
	 * @see #updateEnableUser(ID, ID, ID, Boolean)
	 */
	public void updateRole(ID user, ID roleNew) {
		updateEnableUser(user, null, roleNew, null);
	}

	/**
	 * 入参值为 null 表示不做修改
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
			TaskExecutors.submit(new ChangeOwningDeptTask(user, deptNew));
		}
	}
	
	/**
	 * 用户注册
	 * 
	 * @param record
	 */
	public void txSignUp(Record record) {
		if (!SysConfiguration.getBool(ConfigurableItem.OpenSignUp)) {
			throw new DataSpecificationException("管理员未开放公开注册");
		}
		
		record = this.create(record);
		
		ID newUserId = record.getPrimary();
		String content = String.format(
				"用户 @%s 提交了注册申请。请验证用户有效性后为其启用并指定部门和角色，以便用户登录使用。如果这是一个无效的注册申请请忽略。"
				+ "[点击此处激活](%s/admin/bizuser/users#!/View/User/%s)",
				newUserId, AppUtils.getContextPath(), newUserId);
		
		Message message = MessageBuilder.createMessage(ADMIN_USER, content);
		Application.getNotifications().send(message);
	}
}
