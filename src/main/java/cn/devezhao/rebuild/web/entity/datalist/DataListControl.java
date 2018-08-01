/*
 Copyright (C) 2013 QIDAPP.com. All rights reserved.
 QIDAPP.com PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package cn.devezhao.rebuild.web.entity.datalist;

/**
 * @author Zhao Fangfang
 * @version $Id: GridDataControl.java 1 2014-11-26 17:20:23Z zhaoff@qidapp.com $
 * @since 1.0, 2013-6-20
 */
public interface DataListControl {
	
	/**
	 * 默认过滤条件
	 * 
	 * @return
	 */
	String getDefaultFilter();
	
	/**
	 * 结果集
	 * 
	 * @return
	 */
	String getResult();
}
