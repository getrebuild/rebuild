/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.support;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.core.Application;
import com.rebuild.core.BootApplication;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import org.dom4j.Element;

/**
 * 根据 METADATA 生成表的创建语句
 *
 * @author Zhao Fangfang
 * @since 0.2, 2014-4-10
 */
public class SchemaGenerator {

    private static PersistManagerFactory PMF;

    public static void main(String[] args) {
        System.setProperty("spring.main.web-application-type", "none");  // No Web
        System.setProperty("rbdev", "true");  // dev/debug mode
        BootApplication.main(new String[0]);

        PMF = Application.getPersistManagerFactory();

        // !!! COMMENT DynamicMetadataFactory#appendConfig4Db
//        generate();
        generate(EntityHelper.FrontjsCode);

        System.exit(0);
    }

    /**
     * 生成全部实体
     */
    static void generate() {
        for (Entity entity : PMF.getMetadataFactory().getEntities()) {
            if (EasyMetaFactory.valueOf(entity).isBuiltin()) generate(entity.getEntityCode());
        }
    }

    /**
     * 生成指定实体
     *
     * @param entityCode
     */
    static void generate(int entityCode) {
        Entity entity = PMF.getMetadataFactory().getEntity(entityCode);
        Element root = ((ConfigurationMetadataFactory) PMF.getMetadataFactory()).getConfigDocument().getRootElement();
        Table table = new Table(
                entity,
                PMF.getDialect(),
                root.selectSingleNode("//entity[@name='" + entity.getName() + "']").selectNodes("index"));

        String[] ddl = table.generateDDL(false, false, false);

        StringBuffer sb = new StringBuffer();
        sb.append("-- ************ Entity [").append(entity.getName()).append("] DDL ************\n");
        for (String d : ddl) {
            sb.append(d).append("\n");
        }
        System.out.println(sb);
    }
}
