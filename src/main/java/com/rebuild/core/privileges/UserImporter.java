/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.privileges;

import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.commons.excel.Cell;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.core.Application;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyEmail;
import com.rebuild.core.metadata.easymeta.EasyPhone;
import com.rebuild.core.privileges.bizz.Department;
import com.rebuild.core.privileges.bizz.User;
import com.rebuild.core.support.ConfigurationItem;
import com.rebuild.core.support.RebuildConfiguration;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.i18n.LanguageBundle;
import com.rebuild.core.support.integration.SMSender;
import com.rebuild.core.support.task.HeavyTask;
import com.rebuild.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;

/**
 * 用户导入
 *
 * @author devezhao
 * @since 2020/11/5
 */
@Slf4j
public class UserImporter extends HeavyTask<Integer> {

    final private File useFile;
    final private boolean emailNotify;

    /**
     * @param useFile
     * @param emailNotify
     */
    public UserImporter(File useFile, boolean emailNotify) {
        this.useFile = useFile;
        this.emailNotify = emailNotify;
    }

    @Override
    protected Integer exec() {
        final List<Cell[]> data = ExcelUtils.readExcel(useFile, -1, false);
        this.setTotal(data.size());

        for (Cell[] row : data) {
            String loginName = cellAsString(row, 0);
            String password = cellAsString(row, 1);
            if (StringUtils.isBlank(loginName) || StringUtils.isBlank(password)) {
                log.warn("[loginName] or [password] cannot be empty");
                continue;
            }

            if (Application.getUserStore().existsName(loginName)) {
                log.warn("[loginName] cannot be repeated");
                continue;
            }

            String email = cellAsString(row, 7);
            if (StringUtils.isNotBlank(email) && Application.getUserStore().existsEmail(email)) {
                log.warn("[email] cannot be repeated");
                continue;
            }

            String deptName = cellAsString(row, 2);
            String roleName = cellAsString(row, 3);
            String fullName = cellAsString(row, 4);
            String jobTitle = cellAsString(row, 5);
            String workphone = cellAsString(row, 6);

            Record newUser = EntityHelper.forNew(EntityHelper.User, getUser());
            newUser.setString("loginName", loginName);
            newUser.setString("password", password);

            ID deptId = findDepartment(deptName);
            if (deptId != null) newUser.setID("deptId", deptId);

            ID roleId = findRole(roleName);
            if (roleId != null) newUser.setID("roleId", roleId);

            if (EasyPhone.isPhone(workphone)) newUser.setString("workphone", workphone);
            if (EasyEmail.isEmail(email)) newUser.setString("email", email);

            if (StringUtils.isNotBlank(fullName)) newUser.setString("fullName", fullName);
            if (StringUtils.isNotBlank(jobTitle)) newUser.setString("jobTitle", jobTitle);

            try {
                newUser = Application.getBean(UserService.class).create(newUser);
                this.addSucceeded();

                if (emailNotify) {
                    sendEmailNotify(Application.getUserStore().getUser(newUser.getPrimary()), password);
                }

            } catch (Exception ex) {
                log.error("Cannot create new user : " + loginName, ex);
            } finally {
                this.addCompleted();
            }
        }

        return this.getSucceeded();
    }

    private ID findRole(String roleName) {
        for (Role role : Application.getUserStore().getAllRoles()) {
            if (role.getName().equalsIgnoreCase(roleName)
                    || role.getIdentity().toString().equals(roleName)) {
                return (ID) role.getIdentity();
            }
        }
        return null;
    }

    private ID findDepartment(String deptName) {
        for (Department dept : Application.getUserStore().getAllDepartments()) {
            if (dept.getName().equalsIgnoreCase(deptName)
                    || dept.getIdentity().toString().equalsIgnoreCase(deptName)) {
                return (ID) dept.getIdentity();
            }
        }
        return null;
    }

    private String cellAsString(Cell[] row, int index) {
        if (row.length >= index + 1) {
            Cell cell = row[index];
            if (cell == null || cell.asString() == null) return null;
            else return cell.asString().trim();
        }
        return null;
    }

    private void sendEmailNotify(User to, String passwordPlain) {
        if (!SMSender.availableMail()) return;
        if (to.getEmail() == null) return;

        String appName = RebuildConfiguration.get(ConfigurationItem.AppName);
        String homeUrl = RebuildConfiguration.getHomeUrl();

        LanguageBundle bundle = Language.getSysDefaultBundle();
        String subject = bundle.L("你的账号已就绪");
        String content = bundle.L(
                "系统管理员已经为你开通了 %s 账号！以下为你的登录信息，请妥善保管。 [] 登录账号 : **%s** [] 登录密码 : **%s** [] 登录地址 : [%s](%s) [][] 首次登陆，建议你立即修改登陆密码。修改方式 : 登陆后点击右上角头像 - 个人设置 - 安全设置 - 更改密码",
                appName, to.getName(), passwordPlain, homeUrl, homeUrl);

        SMSender.sendMailAsync(to.getEmail(), subject, content);
    }
}
