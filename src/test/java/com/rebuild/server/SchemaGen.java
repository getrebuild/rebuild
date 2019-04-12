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

package com.rebuild.server;

import org.dom4j.Element;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rebuild.server.metadata.EntityHelper;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.PersistManagerFactoryImpl;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;

/**
 * 根据 METADATA 生成表的创建语句
 * 
 * @author Zhao Fangfang
 * @since 0.2, 2014-4-10
 */
public class SchemaGen {
	
	private static ApplicationContext CTX;
	private static PersistManagerFactory PMF;

	public static void main(String[] args) {
		CTX = new ClassPathXmlApplicationContext(new String[] { "application-ctx.xml", });
		PMF = CTX.getBean(PersistManagerFactoryImpl.class);
		
//		genAll();
		gen(EntityHelper.Classification);
		gen(EntityHelper.ClassificationData);
		
		System.exit(0);
	}
	
	static void genAll() {
		for (Entity entity : PMF.getMetadataFactory().getEntities()) {
			gen(entity.getEntityCode());
		}
	}
	
	static void gen(int e) {
		Entity entity = PMF.getMetadataFactory().getEntity(e);
		Element root = ((ConfigurationMetadataFactory) PMF.getMetadataFactory()).getConfigDocument().getRootElement();
		Table table = new Table(
				entity,
				PMF.getDialect(),
				root.selectSingleNode("//entity[@name='" + entity.getName() + "']").selectNodes("index"));
		
		String[] ddl = table.generateDDL(true, false);
		
		StringBuffer sb = new StringBuffer();
		sb.append("-- ************ Entity [" + entity.getName() + "] DDL ************\n");
		for (String d : ddl) {
			if (d.startsWith("drop ")) {
				d = "" + d;
			}
			sb.append(d).append("\n");
		}
		System.out.println(sb);
	}
}
