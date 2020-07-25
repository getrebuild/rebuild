/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
package com.rebuild.server;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.PersistManagerFactoryImpl;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.server.metadata.EntityHelper;
import org.dom4j.Element;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 根据 METADATA 生成表的创建语句
 * 
 * @author Zhao Fangfang
 * @since 0.2, 2014-4-10
 */
public class SchemaGen {

	private static ApplicationContext CTX;
	private static PersistManagerFactory PMF;

	private static boolean DROP_EXISTS = !true;

	public static void main(String[] args) {
		CTX = new ClassPathXmlApplicationContext(new String[] { "application-ctx.xml", });
		PMF = CTX.getBean(PersistManagerFactoryImpl.class);
		
//		genAll();
		gen(EntityHelper.ProjectConfig);
		gen(EntityHelper.ProjectPlanConfig);
		gen(EntityHelper.ProjectTask);
		gen(EntityHelper.ProjectTaskRelation);
		gen(EntityHelper.ProjectTaskComment);
		gen(EntityHelper.ProjectTaskTag);
		gen(EntityHelper.ProjectTaskTagRelation);

		System.exit(0);
	}

    /**
     * 生成全部实体
     */
    static void genAll() {
		for (Entity entity : PMF.getMetadataFactory().getEntities()) {
			gen(entity.getEntityCode());
		}
	}

    /**
     * 生成指定实体
     *
     * @param e
     */
	static void gen(int e) {
		Entity entity = PMF.getMetadataFactory().getEntity(e);
		Element root = ((ConfigurationMetadataFactory) PMF.getMetadataFactory()).getConfigDocument().getRootElement();
		Table table = new Table(
				entity,
				PMF.getDialect(),
				root.selectSingleNode("//entity[@name='" + entity.getName() + "']").selectNodes("index"));

		String[] ddl = table.generateDDL(DROP_EXISTS, false, false);
		
		StringBuffer sb = new StringBuffer();
		sb.append("-- ************ Entity [" + entity.getName() + "] DDL ************\n");
		for (String d : ddl) {
			sb.append(d).append("\n");
		}
		System.out.println(sb);
	}
}
