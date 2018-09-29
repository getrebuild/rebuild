/*
rebuild - Building your system freely.
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

package com.rebuild.server.bizz.privileges;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class UserStore {
	
	private static final Log LOG = LogFactory.getLog(UserStore.class);

	final private Map<ID, User> USERs = new ConcurrentHashMap<>();
	final private Map<ID, Role> ROLEs = new ConcurrentHashMap<>();
	final private Map<ID, BusinessUnit> DEPTs = new ConcurrentHashMap<>();
	
	final private Map<String, ID> USERs_NAME2ID = new ConcurrentHashMap<>();
	final private Map<String, ID> USERs_MAIL2ID = new ConcurrentHashMap<>();
	
	final private PersistManagerFactory PM_FACTORY;
	
	protected UserStore(PersistManagerFactory persistManagerFactory) {
		super();
		this.PM_FACTORY = persistManagerFactory;
	}
	
	/**
	 * @param username
	 * @return
	 */
	public boolean existsName(String username) {
		return USERs_NAME2ID.containsKey(normalIdentifier(username));
	}
	
	/**
	 * @param email
	 * @return
	 */
	public boolean existsEmail(String email) {
		return USERs_MAIL2ID.containsKey(normalIdentifier(email));
	}
	
	/**
	 * @param emailOrName
	 * @return
	 */
	public boolean exists(String emailOrName) {
		return existsName(emailOrName) || existsEmail(emailOrName);
	}
	
	/**
	 * @param userId
	 * @return
	 */
	public boolean exists(ID userId) {
		return USERs.containsKey(userId);
	}
	
	/**
	 * @param username
	 * @return
	 * @throws NoMemberFoundException
	 */
	public User getUserByName(String username) throws NoMemberFoundException {
		ID userId = USERs_NAME2ID.get(normalIdentifier(username));
		if (userId == null) {
			throw new NoMemberFoundException("No User found: " + username);
		}
		return getUser(userId);
	}
	
	/**
	 * @param email
	 * @return
	 * @throws NoMemberFoundException
	 */
	public User getUserByEmail(String email) throws NoMemberFoundException {
		ID userId = USERs_MAIL2ID.get(normalIdentifier(email));
		if (userId == null) {
			throw new NoMemberFoundException("No User found: " + email);
		}
		return getUser(userId);
	}
	
	/**
	 * @param emailOrName
	 * @return
	 * @throws NoMemberFoundException
	 */
	public User getUser(String emailOrName) throws NoMemberFoundException {
		if (existsEmail(emailOrName)) {
			return getUserByEmail(emailOrName);
		} else {
			return getUserByName(emailOrName);
		}
	}
	
	/**
	 * @param userId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public User getUser(ID userId) throws NoMemberFoundException {
		User u = USERs.get(userId);
		if (u == null) {
			throw new NoMemberFoundException("No User found: " + userId);
		}
		return u;
	}
	
	/**
	 * @return
	 */
	public User[] getAllUsers() {
		return USERs.values().toArray(new User[USERs.size()]);
	}
	
	/**
	 * @param roleId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public Role getRole(ID roleId) throws NoMemberFoundException {
		Role r = ROLEs.get(roleId);
		if (r == null) {
			throw new NoMemberFoundException("No Role found: " + roleId);
		}
		return r;
	}
	
	/**
	 * @param deptId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public BusinessUnit getDept(ID deptId) throws NoMemberFoundException {
		BusinessUnit b = DEPTs.get(deptId);
		if (b == null) {
			throw new NoMemberFoundException("No Department found: " + deptId);
		}
		return b;
	}
	
	/**
	 * 刷新用户缓存
	 * 
	 * @param userId
	 * @return
	 */
	public User refreshUser(ID userId) {
		final boolean ifExists = exists(userId);
		
		// OLD , CLEAN , HOLD
		
		User oldUser = !ifExists ? null : getUser(userId);
		Role oldRole = oldUser == null ? null : oldUser.getOwningRole();
		if (oldRole != null) {
			oldRole.removeMember(oldUser);
		}
		BusinessUnit oldDept = oldUser == null ? null : oldUser.getOwningBizUnit();
		if (oldDept != null) {
			oldDept.removeMember(oldUser);
		}
		
		Object[] u = Application.createNoFilterQuery(
				"select " + USER_FS + " from User where userId = ?")
				.setParameter(1, userId)
				.unique();
		User newUser = new User(
				userId, (String) u[1], (String) u[2], (String) u[3], (String) u[4], (Boolean) u[5]);
		
		String oldEmail = ifExists ? normalIdentifier(oldUser.getEmail()) : null;
		if (oldEmail != null) {
			USERs_MAIL2ID.remove(oldEmail);
		}
		
		store(newUser);
		
		// ROLE
		
		ID newRoleId = (ID) u[7];
		if (oldRole == null || !oldRole.getIdentity().equals(newRoleId)) {
			getRole(newRoleId).addMember(newUser);
		} else {
			oldRole.addMember(newUser);
		}
		
		// DEPT
		
		ID newDeptId = (ID) u[6];
		if (oldDept == null || !oldDept.getIdentity().equals(newDeptId)) {
			getRole(newDeptId).addMember(newUser);
		} else {
			oldDept.addMember(newUser);
		}
		
		return getUser(userId);
	}
	
	/**
	 * 刷新角色缓存
	 * 
	 * @param roleId
	 */
	public void refreshRole(ID roleId) {
	}
	
	/**
	 * 刷新部门缓存
	 * 
	 * @param deptId
	 */
	public void refreshDept(ID deptId) {
	}
	
	private static final String USER_FS = "userId,loginName,email,fullName,avatarUrl,isDisabled,deptId,roleId";
	/**
	 * 初始化
	 * 
	 * @throws Exception
	 */
	synchronized 
	public void init() throws Exception {
		// User
		
		final Map<ID, Set<ID>> roleOfUserMap = new HashMap<>();
		final Map<ID, Set<ID>> deptOfUserMap = new HashMap<>();
		
		Object[][] array = PM_FACTORY.createQuery("select " + USER_FS + " from User").array();
		for (Object[] o : array) {
			ID userId = (ID) o[0];
			User user = new User(
					userId, (String) o[1], (String) o[2], (String) o[3], (String) o[4], (Boolean) o[5]);
			
			ID roleId = (ID) o[7];
			if (roleId != null) {
				Set<ID> roleOfUser = roleOfUserMap.get(roleId);
				if (roleOfUser == null) {
					roleOfUser = new HashSet<ID>();
					roleOfUserMap.put(roleId, roleOfUser);
				}
				roleOfUser.add(userId);
			}
			ID deptId = (ID) o[6];
			if (deptId != null) {
				Set<ID> deptOfUser = deptOfUserMap.get(deptId);
				if (deptOfUser == null) {
					deptOfUser = new HashSet<ID>();
					deptOfUserMap.put(deptId, deptOfUser);
				}
				deptOfUser.add(userId);
			}
			
			store(user);
		}
		LOG.info("Loaded [ " + USERs.size() + " ] users.");
		
		// ROLE
		
		array = PM_FACTORY.createQuery("select roleId,name,isDisabled from Role").array();
		for (Object[] o : array) {
			ID roleId = (ID) o[0];
			Role role = new Role(roleId, (String) o[1], (Boolean) o[2]);
			
			Object[][] definition = PM_FACTORY.createQuery(
					"select entity,definition from RolePrivileges where roleId = ?")
					.setParameter(1, roleId)
					.array();
			for (Object[] e : definition) {
				Privileges priv = new EntityPrivileges((Integer) e[0], (String) e[1]);
				role.addPrivileges(priv);
			}
			
			Set<ID> roleOfUser = roleOfUserMap.get(roleId);
			if (roleOfUser != null) {
				for (ID userId : roleOfUser) {
					role.addMember(getUser(userId));
				}
			}
			
			store(role);
		}
		LOG.info("Loaded [ " + ROLEs.size() + " ] roles.");
		
		// DEPT
		
		array = PM_FACTORY.createQuery("select deptId,name,isDisabled,parentDept from Department").array();
		Map<ID, Set<ID>> parentTemp = new HashMap<>();
		for (Object[] o : array) {
			ID deptId = (ID) o[0];
			BusinessUnit dept = new BusinessUnit(deptId, (String) o[1], (Boolean) o[2]);
			
			Set<ID> deptOfUser = deptOfUserMap.get(deptId);
			if (deptOfUser != null) {
				for (ID userId : deptOfUser) {
					dept.addMember(getUser(userId));
				}
			}
			
			ID parent = (ID) o[3];
			if (parent != null) {
				Set<ID> child = parentTemp.get(parent);
				if (child == null) {
					child = new HashSet<>();
					parentTemp.put(parent, child);
				}
				child.add(deptId);
			}
			
			store(dept);
		}
		
		// 组织关系
		for (Map.Entry<ID, Set<ID>> e : parentTemp.entrySet()) {
			BusinessUnit parent = getDept(e.getKey());
			for (ID child : e.getValue()) {
				parent.addChild(getDept(child));
			}
		}
		
		LOG.info("Loaded [ " + DEPTs.size() + " ] departments.");
	}
	
	/**
	 * @param user
	 */
	protected void store(User user) {
		USERs.put((ID) user.getIdentity(), user);
		USERs_NAME2ID.put(normalIdentifier(user.getName()), (ID) user.getIdentity());
		if (user.getEmail() != null) {
			USERs_MAIL2ID.put(normalIdentifier(user.getEmail()), (ID) user.getIdentity());
		}
	}
	
	/**
	 * @param role
	 */
	protected void store(Role role) {
		ROLEs.put((ID) role.getIdentity(), role);
	}
	
	/**
	 * @param dept
	 */
	protected void store(BusinessUnit dept) {
		DEPTs.put((ID) dept.getIdentity(), dept);
	}
	
	/*-
	 * 统一化
	 */
	static String normalIdentifier(String ident) {
		return StringUtils.defaultIfEmpty(ident, "").toLowerCase();
	}
}
