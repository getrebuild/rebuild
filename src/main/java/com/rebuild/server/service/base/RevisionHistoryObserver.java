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

package com.rebuild.server.service.base;

import cn.devezhao.commons.CalendarUtils;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.alibaba.fastjson.JSON;
import com.rebuild.server.Application;
import com.rebuild.server.business.trigger.RobotTriggerObserver;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.OperatingContext;
import com.rebuild.server.service.OperatingObserver;
import com.rebuild.server.service.bizz.UserService;
import com.rebuild.utils.JSONUtils;

/**
 * 纪录变更历史
 * 
 * @author devezhao
 * @since 10/31/2018
 */
public class RevisionHistoryObserver extends OperatingObserver {
	
	@Override
	public void onCreate(OperatingContext context) {
        Record revision = newRevision(context, false);
        Application.getCommonService().create(revision);
	}
	
	@Override
	public void onUpdate(OperatingContext context) {
        Record revision = newRevision(context, true);
        Application.getCommonService().create(revision);
	}
	
	@Override
	public void onDelete(OperatingContext context) {
        Record revision = newRevision(context, false);
        Application.getCommonService().create(revision);
	}
	
	@Override
	public void onAssign(OperatingContext context) {
        Record revision = newRevision(context, true);
        Application.getCommonService().create(revision);
	}
	
	@Override
	public void onShare(OperatingContext context) {
        Record revision = newRevision(context, false);
        ID recordId = context.getAfterRecord().getID("recordId");
        revision.setID("recordId", recordId);
        revision.setString("belongEntity", MetadataHelper.getEntityName(recordId));
        Application.getCommonService().create(revision);
	}

	@Override
	public void onUnshare(OperatingContext context) {
        Record revision = newRevision(context, false);
        ID recordId = context.getBeforeRecord().getID("recordId");
        revision.setID("recordId", recordId);
        revision.setString("belongEntity", MetadataHelper.getEntityName(recordId));
        Application.getCommonService().create(revision);
	}

	/**
	 * @param context
	 * @param mergeChange
	 * @return
	 */
	private Record newRevision(OperatingContext context, boolean mergeChange) {
	    ID recordId = context.getAnyRecord().getPrimary();
	    Record record = EntityHelper.forNew(EntityHelper.RevisionHistory, UserService.SYSTEM_USER);
	    record.setString("belongEntity", MetadataHelper.getEntityName(recordId));
	    record.setID("recordId", recordId);
	    record.setInt("revisionType", context.getAction().getMask());
	    record.setID("revisionBy", context.getOperator());
	    record.setDate("revisionOn", CalendarUtils.now());

	    if (mergeChange) {
			Record before = context.getBeforeRecord();
			Record after = context.getAfterRecord();
			JSON revisionContent = new RecordMerger(before).merge(after);
			record.setString("revisionContent", revisionContent.toJSONString());
		} else {
			record.setString("revisionContent", JSONUtils.EMPTY_ARRAY_STR);
		}

		OperatingContext source = RobotTriggerObserver.getTriggerSource();
		if (source != null) {
			record.setID("channelWith", source.getAnyRecord().getPrimary());
		}

	    return record;
    }
}