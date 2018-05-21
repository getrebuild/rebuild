package cn.devezhao.rebuild.web.commons;

import cn.devezhao.persist4j.engine.ID;

/**
 * 当前请求用户（线程量）
 * 
 * @author zhaofang123@gmail.com
 * @since 11/20/2017
 */
public class CurrentCaller {

	private static final ThreadLocal<ID> CALLER = new ThreadLocal<>();
	
	protected void set(ID user) {
		CALLER.set(user);
	}
	
	protected void clean() {
		CALLER.remove();
	}

	/**
	 * @return
	 */
	public ID get() {
		return CALLER.get();
	}
	
	/**
	 * @return
	 * @throws BadRequestException
	 */
	public ID getNotNull() throws BadRequestException {
		ID user = get();
		if (user == null) {
			throw new BadRequestException(403, "无效请求用户");
		}
		return user;
	}
}
