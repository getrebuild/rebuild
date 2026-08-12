/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.rbstore;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.core.Application;
import com.rebuild.core.aibot2.AibotCommonsConfigManager;
import com.rebuild.core.aibot2.AibotCommonsConfigService;
import com.rebuild.core.metadata.EntityHelper;
import com.rebuild.core.support.i18n.Language;
import com.rebuild.core.support.task.HeavyTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * 技能导入器。从 RBStore 获取技能列表并导入到 AibotCommonsConfig
 *
 * @author devezhao
 * @since 2026/7/26
 */
@Slf4j
public class SkillImporter extends HeavyTask<Integer> {

    private String[] skillNames;

    public void setSkillNames(String[] skillNames) {
        this.skillNames = skillNames;
    }

    @Override
    protected Integer exec() throws Exception {
        Assert.notNull(skillNames, "[skillNames] cannot be null");

        JSONArray schemas = (JSONArray) RBStore.fetchSkills(null);
        if (schemas == null || schemas.isEmpty()) {
            this.errorMessage = Language.L("暂无可用技能");
            return 0;
        }

        this.setTotal(skillNames.length);

        for (String name : skillNames) {
            try {
                JSONObject skillIndex = findSkill(schemas, name);
                if (skillIndex == null) {
                    log.warn("Skill not found in rbstore : {}", name);
                    this.addCompleted();
                    continue;
                }

                String skillName = skillIndex.getString("name");

                // load full skill data (with prompt) from individual file
                JSONObject skill = (JSONObject) RBStore.fetchSkills(skillIndex.getString("file"));

                // create AibotCommonsConfig record
                Record record = EntityHelper.forNew(EntityHelper.AibotCommonsConfig);
                record.setString("type", AibotCommonsConfigManager.TYPE_SKILL);
                record.setString("name", skillName);

                JSONObject config = new JSONObject();
                config.put("name", skillName);
                config.put("description", skill.getString("description"));
                config.put("prompt", skill.getString("prompt"));
                record.setString("config", config.toJSONString());

                Application.getBean(AibotCommonsConfigService.class).create(record);
                log.info("Skill imported : {}", skillName);
                this.addSucceeded();

            } catch (Exception ex) {
                log.error("Cannot import skill : {}", name, ex);
                this.errorMessage = ex.getLocalizedMessage();
            }
            this.addCompleted();
        }

        return getSucceeded();
    }

    /**
     * @param schemas
     * @param name
     * @return
     */
    private JSONObject findSkill(JSONArray schemas, String name) {
        for (Object o : schemas) {
            JSONObject item = (JSONObject) o;
            if (name.equalsIgnoreCase(item.getString("name"))) {
                return item;
            }
        }
        return null;
    }
}
