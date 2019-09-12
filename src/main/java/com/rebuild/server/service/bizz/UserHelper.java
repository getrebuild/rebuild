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

import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.Member;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.rebuild.server.Application;
import com.rebuild.server.helper.SysConfiguration;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.bizz.privileges.Department;
import com.rebuild.server.service.bizz.privileges.User;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户帮助类
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class UserHelper {

	private static final Log LOG = LogFactory.getLog(UserHelper.class);
	
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
			LOG.error("No User found : " + userId);
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
		return UserService.ADMIN_USER.equals(userId);
	}
	
	/**
	 * 是否激活
	 * 
	 * @param bizzId ID of User/Role/Department
	 * @return
	 */
	public static boolean isActive(ID bizzId) {
		try {
			if (bizzId.getEntityCode() == EntityHelper.User) {
				return Application.getUserStore().getUser(bizzId).isActive();
			} else if (bizzId.getEntityCode() == EntityHelper.Department) {
				return !Application.getUserStore().getDepartment(bizzId).isDisabled();
			} else if (bizzId.getEntityCode() == EntityHelper.Role) {
				return !Application.getUserStore().getRole(bizzId).isDisabled();
			}
		} catch (NoMemberFoundException ex) {
			LOG.error("No bizz found : " + bizzId);
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
			LOG.error("No User found : " + userId);
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
	 * @param bizzId ID of User/Role/Department
	 * @return
	 */
	public static String getName(ID bizzId) {
		try {
			if (bizzId.getEntityCode() == EntityHelper.User) {
				return Application.getUserStore().getUser(bizzId).getFullName();
			} else if (bizzId.getEntityCode() == EntityHelper.Department) {
				return Application.getUserStore().getDepartment(bizzId).getName();
			} else if (bizzId.getEntityCode() == EntityHelper.Role) {
				return Application.getUserStore().getRole(bizzId).getName();
			} 
		} catch (NoMemberFoundException ex) {
			LOG.error("No bizz found : " + bizzId);
		}
		return null;
	}
	
	/**
	 * 获取部门或角色下的成员
	 * 
	 * @param groupId ID of Role/Department
	 * @return
	 */
	public static Member[] getMembers(ID groupId) {
		Set<Principal> ms = null;
		try {
			if (groupId.getEntityCode() == EntityHelper.Department) {
				ms = Application.getUserStore().getDepartment(groupId).getMembers();
			} else if (groupId.getEntityCode() == EntityHelper.Role) {
				ms = Application.getUserStore().getRole(groupId).getMembers();
			}
		} catch (NoMemberFoundException ex) {
			LOG.error("No group found : " + groupId);
		}
		
		if (ms == null || ms.isEmpty()) {
			return new Member[0];
		}
		return ms.toArray(new Member[0]);
	}
	
	/**
	 * 解析用户列表
	 * 
	 * @param userDefs
	 * @param record
	 * @return
	 */
	public static Set<ID> parseUsers(JSONArray userDefs, ID record) {
		if (userDefs == null) {
			return Collections.emptySet();
		}
		
		Set<String> users = new HashSet<>();
		for (Object u : userDefs) {
			users.add((String) u);
		}
		return parseUsers(users, record);
	}
	
	/**
	 * 解析用户列表
	 * 
	 * @param userDefs
	 * @param record
	 * @return
	 */
	public static Set<ID> parseUsers(Collection<String> userDefs, ID record) {
		Entity entity = record == null ? null : MetadataHelper.getEntity(record.getEntityCode());
		
		Set<ID> bizzs = new HashSet<>();
		Set<String> fromFields = new HashSet<>();
		for (String def : userDefs) {
			if (ID.isId(def)) {
				bizzs.add(ID.valueOf(def));
			} else if (entity != null && MetadataHelper.getLastJoinField(entity, def) != null) {
				fromFields.add(def);
			}
		}
		
		if (!fromFields.isEmpty()) {
			String sql = String.format("select %s from %s where %s = ?", 
					StringUtils.join(fromFields.iterator(), ","), entity.getName(), entity.getPrimaryField().getName());
			Object[] bizzValues = Application.createQueryNoFilter(sql).setParameter(1, record).unique();
			for (Object bizz : bizzValues) {
				if (bizz != null) {
					bizzs.add((ID) bizz);
				}
			}
		}
		
		Set<ID> users = new HashSet<>();
		for (ID bizz : bizzs) {
			if (bizz.getEntityCode() == EntityHelper.User) {
				users.add(bizz);
			} else if (bizz.getEntityCode() == EntityHelper.Department || bizz.getEntityCode() == EntityHelper.Role) {
				Member ms[] = UserHelper.getMembers(bizz);
				for (Member m : ms) {
					users.add((ID) m.getIdentity());
				}
			}
		}
		return users;
	}

	private static final Color[] AB_COLORS = new Color[] {
			new Color(66, 133,244),
			new Color(52, 168,83),
			new Color(251, 188,5),
			new Color(234, 67,53)
	};
	/**
	 * 生成用户头像
	 *
	 * @param name
	 * @param reload
	 * @return
	 * @throws IOException
	 */
	public static File generateAvatar(String name, boolean reload) throws IOException {
		File avatarFile = SysConfiguration.getFileOfData("avatar-" + name + ".jpg");
		if (avatarFile.exists()) {
			if (reload) {
				avatarFile.delete();
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

		g2d.setColor(AB_COLORS[RandomUtils.nextInt(AB_COLORS.length)]);
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		final Font font = createFont(81f);
		g2d.setFont(font);
		g2d.setColor(Color.WHITE);

		FontMetrics fontMetrics = g2d.getFontMetrics(font);
		int x = fontMetrics.stringWidth(name);
		g2d.drawString(name, (200 - x) / 2, 128);

		try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
			ImageIO.write(bi, "png", fos);
			fos.flush();
		}

		return avatarFile;
	}

	/**
	 * @param fs
	 * @return
	 */
	private static Font createFont(float fs) {
		File fontFile = SysConfiguration.getFileOfData("SourceHanSansK-Regular.ttf");
		if (fontFile.exists()) {
			try {
				Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
				font = font.deriveFont(fs);
				return font;
			} catch (Exception ex) {
				LOG.warn("Couldn't create Font: SourceHanSansK-Regular.ttf", ex);
			}
		}
		// Use default
		return new Font("SimHei", Font.BOLD, (int) fs);
	}
}
