/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.support;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.dialect.Dialect;
import cn.devezhao.persist4j.dialect.MySQL5Dialect;
import cn.devezhao.persist4j.metadata.MetadataFactory;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;
import com.rebuild.core.metadata.easymeta.EasyMetaFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.dom4j.Element;

import java.util.Scanner;

/**
 * 根据 METADATA 生成表的创建语句
 *
 * @author Zhao Fangfang
 * @since 0.2, 2014-4-10
 */
public class SchemaGenerator {

    private static Dialect dialect;
    private static MetadataFactory metadataFactory;

    public static void main(String[] args) {
        dialect = new MySQL5Dialect();
        metadataFactory = new ConfigurationMetadataFactory("metadata-conf.xml", dialect);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter the entity code (enter 0 for all) :");
        if (scanner.hasNext()) {
            String input = scanner.next();
            if ("0".equals(input)) {
                generate();
            } else if (NumberUtils.isNumber(input)) {
                generate(NumberUtils.toInt(input));
            } else {
                IOUtils.closeQuietly(scanner);
            }
        }

        System.exit(0);
    }

    /**
     * 生成全部实体
     */
    static void generate() {
        for (Entity entity : metadataFactory.getEntities()) {
            if (EasyMetaFactory.valueOf(entity).isBuiltin()) generate(entity.getEntityCode());
        }
    }

    /**
     * 生成指定实体
     *
     * @param entityCode
     */
    static void generate(int entityCode) {
        Entity entity = metadataFactory.getEntity(entityCode);
        Element root = ((ConfigurationMetadataFactory) metadataFactory).getConfigDocument().getRootElement();
        Table table = new Table(
                entity,
                dialect,
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
