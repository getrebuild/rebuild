/*
rebuild - Building your business-systems freely.
Copyright (C) 2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.business.trigger.impl;

import cn.devezhao.persist4j.Entity;
import com.rebuild.server.business.trigger.ActionContext;
import com.rebuild.server.business.trigger.ActionType;
import com.rebuild.server.business.trigger.TriggerAction;
import com.rebuild.server.business.trigger.TriggerException;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;

/**
 * @author devezhao
 * @since 2019/8/23
 */
public class AutoAssign implements TriggerAction {

    final protected ActionContext context;

    public AutoAssign(ActionContext context) {
        this.context = context;
    }

    @Override
    public ActionType getType() {
        return ActionType.AUTOASSIGN;
    }

    @Override
    public boolean isUsableSourceEntity(int entityCode) {
        Entity entity = MetadataHelper.getEntity(entityCode);
        // 明细不可用
        return entity.getMasterEntity() == null;
    }

    @Override
    public void execute(OperatingContext operatingContext) throws TriggerException {
    }

    @Override
    public void prepare(OperatingContext operatingContext) throws TriggerException {
        // Nothings
    }
}
