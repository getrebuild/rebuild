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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.Application;

import cn.devezhao.bizz.privileges.Privileges;
import cn.devezhao.bizz.privileges.impl.BizzPermission;
import cn.devezhao.bizz.security.EntityPrivileges;
import cn.devezhao.bizz.security.member.BusinessUnit;
import cn.devezhao.bizz.security.member.NoMemberFoundException;
import cn.devezhao.bizz.security.member.Role;
import cn.devezhao.persist4j.Entity;
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
	public Department getDept(ID deptId) throws NoMemberFoundException {
		Department b = DEPTs.get(deptId);
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
		Department oldDept = DEPTs.get(deptId);
		
		Object[] o = aPMFactory.createQuery("select name,isDisabled,parentDept from Department where deptId = ?")
				.setParameter(1, deptId)
				.unique();
		Department newDept = new Department(deptId, (String) o[0], (Boolean) o[1]);
		store(newDept);
		
		ID parent = (ID) o[2];
		if (oldDept != null) {
			if (oldDept.getParent() == null && parent != null) {  // 新加入了部门
				getDept(parent).addChild(newDept);
			} else if (oldDept.getParent() != null && parent == null) {  // 离开了部门
				getDept(parent).removeMember(oldDept);
			} else if (oldDept.getParent() != null && parent != null && !oldDept.getIdentity().equals(parent)) {
				getDept(deptId).removeMember(oldDept);
				getDept(parent).addChild(newDept);
			}
			
		} else if (parent != null) {
			getDept(parent).addChild(newDept);
		}
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
		
		array = aPMFactory.createQuery("select roleId,name,isDisabled from Role").array();
		for (Object[] o : array) {
			ID roleId = (ID) o[0];
			Role role = new Role(roleId, (String) o[1], (Boolean) o[2]);
			
			Object[][] definition = aPMFactory.createQuery(
					"select entity,definition,zeroKey from RolePrivileges where roleId = ?")
					.setParameter(1, roleId)
					.array();
			for (Object[] e : definition) {
				String entity = (String) e[0];
				if ("N".equals(entity)) {
					Privileges p = new ZeroPrivileges((String) e[2], (String) e[1]);
					role.addPrivileges(p);
				} else {
					Entity entityMeta = aPMFactory.getMetadataFactory().getEntity(entity);
					Privileges p = new EntityPrivileges(
							entityMeta.getEntityCode(), converEntityPrivilegesDefinition((String) e[1]));
					role.addPrivileges(p);
				}
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
		ROLEs.put((ID) role.getIdentity(), role);
	}
	
	/**
	 * @param dept
	 */
	private void store(Department dept) {
		DEPTs.put((ID) dept.getIdentity(), dept);
	}
	
	// 统一化 Key
	static private String normalIdentifier(String ident) {
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
		int C = defJson.getInteger("C");
		int D = defJson.getInteger("D");
		int U = defJson.getInteger("U");
		int R = defJson.getInteger("R");
		int A = defJson.getInteger("A");
		int S = defJson.getInteger("S");
	
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
	
	public static void main(String[] args) {
		String c = converEntityPrivilegesDefinition("{\"A\":0,\"R\":1,\"C\":4,\"S\":0,\"D\":0,\"U\":0}");
		System.out.println(c);
	}
}
