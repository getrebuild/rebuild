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

package com.rebuild.server.service.bizz.privileges;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author zhaofang123@gmail.com
 * @since 09/16/2018
 */
public class UserStore {
	
	private static final Log LOG = LogFactory.getLog(UserStore.class);

	final private Map<ID, User> USERs = new ConcurrentHashMap<>();
	final private Map<ID, Role> ROLEs = new ConcurrentHashMap<>();
	final private Map<ID, Department> DEPTs = new ConcurrentHashMap<>();
	
	final private Map<String, ID> USERs_NAME2ID = new ConcurrentHashMap<>();
	final private Map<String, ID> USERs_MAIL2ID = new ConcurrentHashMap<>();
	
	final private PersistManagerFactory aPMFactory;
	
	protected UserStore(PersistManagerFactory aPMFactory) {
		super();
		this.aPMFactory = aPMFactory;
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
		return USERs.values().toArray(new User[0]);
	}
	
	/**
	 * @param deptId
	 * @return
	 * @throws NoMemberFoundException
	 */
	public Department getDepartment(ID deptId) throws NoMemberFoundException {
		Department b = DEPTs.get(deptId);
		if (b == null) {
			throw new NoMemberFoundException("No Department found: " + deptId);
		}
		return b;
	}
	
	/**
	 * @return
	 */
	public Department[] getAllDepartments() {
		return DEPTs.values().toArray(new Department[0]);
	}
	
	/**
	 * 获取一级部门列表
	 * 
	 * @return
	 */
	public Department[] getTopDepartments() {
		List<Department> top = new ArrayList<>();
		for (Department dept : DEPTs.values()) {
			if (dept.getParent() == null) {
				top.add(dept);
			}
		}
		return top.toArray(new Department[0]);
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
	 * @return
	 */
	public Role[] getAllRoles() {
		return ROLEs.values().toArray(new Role[0]);
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
		
		final User oldUser = !ifExists ? null : getUser(userId);
		final Role oldRole = oldUser == null ? null : oldUser.getOwningRole();
		if (oldRole != null) {
			oldRole.removeMember(oldUser);
		}
		final Department oldDept = oldUser == null ? null : oldUser.getOwningDept();
		if (oldDept != null) {
			oldDept.removeMember(oldUser);
		}
		
		Object[] u = Application.createQueryNoFilter(
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
		if (!(newRoleId == null && oldRole == null)) {
			if (oldRole == null || !oldRole.getIdentity().equals(newRoleId)) {
				getRole(newRoleId).addMember(newUser);
			} else {
				oldRole.addMember(newUser);
			}
		}
		
		// DEPT
		
		ID newDeptId = (ID) u[6];
		if (!(newDeptId == null && oldDept == null)) {
			if (oldDept == null || !oldDept.getIdentity().equals(newDeptId)) {
				getDepartment(newDeptId).addMember(newUser);
			} else {
				oldDept.addMember(newUser);
			}
		}
		
		return getUser(userId);
	}

	/**
	 * @param userId
	 */
	public void removeUser(ID userId) {
		User oldUser = getUser(userId);

		// 移除成员
		if (oldUser.getOwningDept() != null) {
			oldUser.getOwningDept().removeMember(oldUser);
		}
		if (oldUser.getOwningRole() != null) {
			oldUser.getOwningRole().removeMember(oldUser);
		}

		// 移除缓存
		for (Map.Entry<String, ID> e : USERs_NAME2ID.entrySet()) {
			if (e.getValue().equals(userId)) {
				USERs_NAME2ID.remove(e.getKey());
				break;
			}
		}
		for (Map.Entry<String, ID> e : USERs_MAIL2ID.entrySet()) {
			if (e.getValue().equals(userId)) {
				USERs_MAIL2ID.remove(e.getKey());
				break;
			}
		}
		USERs.remove(userId);
	}
	
	/**
	 * 刷新角色缓存
	 * 
	 * @param roleId
	 * @param reloadPrivileges
	 */
	public void refreshRole(ID roleId, boolean reloadPrivileges) {
		final Role oldRole = ROLEs.get(roleId);
		
		Object[] o = aPMFactory.createQuery(
				"select roleId,name,isDisabled from Role where roleId = ?")
				.setParameter(1, roleId)
				.unique();
		Role role = new Role(roleId, (String) o[1], (Boolean) o[2]);
		
		if (!reloadPrivileges) {
			if (oldRole != null) {
				for (Privileges priv : oldRole.getAllPrivileges()) {
					role.addPrivileges(priv);
				}
			}
			store(role);
			return;
		}
		
		loadPrivileges(role);
		store(role);
	}
	
	/**
	 * @param roleId
	 * @param transferTo
	 */
	public void removeRole(ID roleId, ID transferTo) {
		Role role = getRole(roleId);
		Principal[] users = role.getMembers().toArray(new Principal[0]);
		
		if (transferTo != null) {
 			Role transferToRole = getRole(transferTo);
 			for (Principal user : users) {
 				transferToRole.addMember(user);
 	 		}
 		}
		
		for (Principal u : users) {
			role.removeMember(u);
		}
 		ROLEs.remove(roleId);
	}
	
	/**
	 * 刷新部门缓存
	 * 
	 * @param deptId
	 */
	public void refreshDepartment(ID deptId) {
		Object[] o = aPMFactory.createQuery(
				"select name,isDisabled,parentDept from Department where deptId = ?")
				.setParameter(1, deptId)
				.unique();
		Department newDept = new Department(deptId, (String) o[0], (Boolean) o[1]);
		ID parent = (ID) o[2];
		
		Department oldDept = DEPTs.get(deptId);
		// 重新组织父子级部门关系
		if (oldDept != null) {
			BusinessUnit oldParent = oldDept.getParent();
			if (oldParent != null) {
				oldParent.removeChild(oldDept);
				
				if (oldParent.getIdentity().equals(parent)) {
					oldParent.addChild(newDept);
				} else if (parent != null) {
					getDepartment(parent).addChild(newDept);
				}
			}
			
			for (BusinessUnit child : oldDept.getChildren()) {
				oldDept.removeChild(child);
				newDept.addChild(child);
			}
			
			store(newDept);
			
		} else {
			store(newDept);
			if (parent != null) {
				getDepartment(parent).addChild(newDept);
			}
		}
	}
	
	/**
	 * @param deptId
	 * @param transferTo
	 */
	public void removeDepartment(ID deptId, ID transferTo) {
		Department dept = getDepartment(deptId);
		Principal[] users = dept.getMembers().toArray(new Principal[0]);
		
		if (transferTo != null) {
 			Department transferToDept = getDepartment(transferTo);
 			for (Principal user : users) {
 				transferToDept.addMember(user);
 	 		}
 		}
		
		if (dept.getParent() != null) {
			dept.getParent().removeChild(dept);
		}
		
		for (Principal u : users) {
			dept.removeMember(u);
		}
		DEPTs.remove(deptId);
	}
	
	private static final String USER_FS = "userId,loginName,email,fullName,avatarUrl,isDisabled,deptId,roleId";
	/**
	 * 初始化
	 * 
	 * @throws Exception
	 */
	synchronized 
	protected void init() throws Exception {
		// User
		
		final Map<ID, Set<ID>> roleOfUserMap = new HashMap<>();
		final Map<ID, Set<ID>> deptOfUserMap = new HashMap<>();
		
		Object[][] array = aPMFactory.createQuery("select " + USER_FS + " from User").array();
		for (Object[] o : array) {
			ID userId = (ID) o[0];
			User user = new User(
					userId, (String) o[1], (String) o[2], (String) o[3], (String) o[4], (Boolean) o[5]);
			
			ID roleId = (ID) o[7];
			if (roleId != null) {
				Set<ID> roleOfUser = roleOfUserMap.computeIfAbsent(roleId, k -> new HashSet<>());
				roleOfUser.add(userId);
			}
			ID deptId = (ID) o[6];
			if (deptId != null) {
				Set<ID> deptOfUser = deptOfUserMap.computeIfAbsent(deptId, k -> new HashSet<>());
				deptOfUser.add(userId);
			}
			
			store(user);
		}
		LOG.info("Loaded [ " + USERs.size() + " ] users.");
		
		// ROLE
		
		array = aPMFactory.createQuery("select roleId,name,isDisabled from Role").array();
		for (Object[] o : array) {
			ID roleId = (ID) o[0];
			Role role = new Role(roleId, (String) o[1], (Boolean) o[2]);
			loadPrivileges(role);
			
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
		
		array = aPMFactory.createQuery("select deptId,name,isDisabled,parentDept from Department").array();
		Map<ID, Set<ID>> parentTemp = new HashMap<>();
		for (Object[] o : array) {
			ID deptId = (ID) o[0];
			Department dept = new Department(deptId, (String) o[1], (Boolean) o[2]);
			
			Set<ID> deptOfUser = deptOfUserMap.get(deptId);
			if (deptOfUser != null) {
				for (ID userId : deptOfUser) {
					dept.addMember(getUser(userId));
				}
			}
			
			ID parent = (ID) o[3];
			if (parent != null) {
				Set<ID> child = parentTemp.computeIfAbsent(parent, k -> new HashSet<>());
				child.add(deptId);
			}
			
			store(dept);
		}
		
		// 组织关系
		for (Map.Entry<ID, Set<ID>> e : parentTemp.entrySet()) {
			BusinessUnit parent = getDepartment(e.getKey());
			for (ID child : e.getValue()) {
				parent.addChild(getDepartment(child));
			}
		}
		
		LOG.info("Loaded [ " + DEPTs.size() + " ] departments.");
	}
	
	/**
	 * @param user
	 */
	private void store(User user) {
		USERs.put((ID) user.getIdentity(), user);
		USERs_NAME2ID.put(normalIdentifier(user.getName()), (ID) user.getIdentity());
		if (user.getEmail() != null) {
			USERs_MAIL2ID.put(normalIdentifier(user.getEmail()), (ID) user.getIdentity());
		}
	}
	
	/**
	 * @param role
	 */
	private void store(Role role) {
		Role old = ROLEs.get(role.getIdentity());
		if (old != null) {
			for (Principal user : old.getMembers()) {
				role.addMember(user);
			}
		}
		ROLEs.put((ID) role.getIdentity(), role);
	}
	
	/**
	 * @param dept
	 */
	private void store(Department dept) {
		Department old = DEPTs.get(dept.getIdentity());
		if (old != null) {
			for (Principal user : old.getMembers()) {
				dept.addMember(user);
			}
		}
		DEPTs.put((ID) dept.getIdentity(), dept);
	}
	
	/**
	 * @param role
	 */
	private void loadPrivileges(Role role) {
		Object[][] definition = aPMFactory.createQuery(
				"select entity,definition,zeroKey from RolePrivileges where roleId = ?")
				.setParameter(1, role.getIdentity())
				.array();
		for (Object[] d : definition) {
			int entity = (int) d[0];
			if (entity == 0) {
				Privileges p = new ZeroPrivileges((String) d[2], (String) d[1]);
				role.addPrivileges(p);
			} else {
				Privileges p = new EntityPrivileges(
						entity, converEntityPrivilegesDefinition((String) d[1]));
				role.addPrivileges(p);
			}
		}
	}
	
	// 统一化 Key
	private String normalIdentifier(String ident) {
		return StringUtils.defaultIfEmpty(ident, "").toLowerCase();
	}
	
	/**
	 * 转换成 bizz 能识别的权限定义
	 * 
	 * @param definition
	 * @return
	 * @see EntityPrivileges
	 * @see BizzPermission
	 */
	static private String converEntityPrivilegesDefinition(String definition) {
		JSONObject defJson = JSON.parseObject(definition);
		int C = defJson.getIntValue("C");
		int D = defJson.getIntValue("D");
		int U = defJson.getIntValue("U");
		int R = defJson.getIntValue("R");
		int A = defJson.getIntValue("A");
		int S = defJson.getIntValue("S");
	
		int deepP = 0;
		int deepL = 0;
		int deepD = 0;
		int deepG = 0;
		
		// {"A":0,"R":1,"C":4,"S":0,"D":0,"U":0} >> 1:9,2:1,3:1,4:1
		
		if (C >= 4) {
			deepP += BizzPermission.CREATE.getMask();
			deepL += BizzPermission.CREATE.getMask();
			deepD += BizzPermission.CREATE.getMask();
			deepG += BizzPermission.CREATE.getMask();
		}
		
		if (D >= 1) deepP += BizzPermission.DELETE.getMask();
		if (D >= 2) deepL += BizzPermission.DELETE.getMask();
		if (D >= 3) deepD += BizzPermission.DELETE.getMask();
		if (D >= 4) deepG += BizzPermission.DELETE.getMask();
		
		if (U >= 1) deepP += BizzPermission.UPDATE.getMask();
		if (U >= 2) deepL += BizzPermission.UPDATE.getMask();
		if (U >= 3) deepD += BizzPermission.UPDATE.getMask();
		if (U >= 4) deepG += BizzPermission.UPDATE.getMask();
		
		if (R >= 1) deepP += BizzPermission.READ.getMask();
		if (R >= 2) deepL += BizzPermission.READ.getMask();
		if (R >= 3) deepD += BizzPermission.READ.getMask();
		if (R >= 4) deepG += BizzPermission.READ.getMask();
		
		if (A >= 1) deepP += BizzPermission.ASSIGN.getMask();
		if (A >= 2) deepL += BizzPermission.ASSIGN.getMask();
		if (A >= 3) deepD += BizzPermission.ASSIGN.getMask();
		if (A >= 4) deepG += BizzPermission.ASSIGN.getMask();
		
		if (S >= 1) deepP += BizzPermission.SHARE.getMask();
		if (S >= 2) deepL += BizzPermission.SHARE.getMask();
		if (S >= 3) deepD += BizzPermission.SHARE.getMask();
		if (S >= 4) deepG += BizzPermission.SHARE.getMask();
		
		return "1:" + deepP + ",2:" + deepL + ",3:" + deepD + ",4:" + deepG;
	}
}
