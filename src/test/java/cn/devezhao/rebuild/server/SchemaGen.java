package cn.devezhao.rebuild.server;

import org.dom4j.Element;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.PersistManagerFactoryImpl;
import cn.devezhao.persist4j.metadata.impl.ConfigurationMetadataFactory;
import cn.devezhao.persist4j.util.support.Table;
import cn.devezhao.rebuild.server.metadata.EntityHelper;

/**
 * 根据 METADATA 生成表的创建语句
 * 
 * @author Zhao Fangfang
 * @version $Id: SchemaGen.java 3407 2017-05-05 10:09:40Z devezhao $
 * @since 0.2, 2014-4-10
 */
public class SchemaGen {
	
	private static ApplicationContext CTX;
	private static PersistManagerFactory PMF;

	public static void main(String[] args) {
		CTX = new ClassPathXmlApplicationContext(new String[] { "application-ctx.xml" });
		PMF = CTX.getBean(PersistManagerFactoryImpl.class);
		
//		genAll();
		gen(EntityHelper.RolePrivileges);
		gen(EntityHelper.RoleMember);
		
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
