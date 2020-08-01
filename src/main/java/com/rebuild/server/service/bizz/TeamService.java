/*
rebuild - Building your business-systems freely.
Copyright (C) 2018-2019 devezhao <zhaofang123@gmail.com>

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

package com.rebuild.server.service.bizz;

import cn.devezhao.bizz.security.member.Team;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;
import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.BaseServiceImpl;
import com.rebuild.server.service.bizz.privileges.AdminGuard;

import java.util.Collection;

/**
 * 团队
 *
 * @author devezhao
 * @since 2019/11/13
 */
public class TeamService extends BaseServiceImpl implements AdminGuard {

    protected TeamService(PersistManagerFactory aPMFactory) {
        super(aPMFactory);
    }

    @Override
    public int getEntityCode() {
        return EntityHelper.Team;
    }

    @Override
    public Record create(Record record) {
        record = super.create(record);
        Application.getUserStore().refreshTeam(record.getPrimary());
        return record;
    }

    @Override
    public Record update(Record record) {
        record = super.update(record);
        Application.getUserStore().refreshTeam(record.getPrimary());
        return record;
    }

    @Override
    public int delete(ID teamId) {
        int del = super.delete(teamId);
        Application.getUserStore().removeTeam(teamId);
        return del;
    }

    /**
     * 添加成员
     *
     * @param teamId
     * @param members
     * @return
     */
    public int createMembers(ID teamId, Collection<ID> members) {
        int added = 0;
        Team team = Application.getUserStore().getTeam(teamId);
        for (ID user : members) {
            if (team.isMember(user)) {
                continue;
            }
            Record record = EntityHelper.forNew(EntityHelper.TeamMember, Application.getCurrentUser());
            record.setID("teamId", teamId);
            record.setID("userId", user);
            super.create(record);
            added++;
        }

        if (added > 0) {
            Application.getUserStore().refreshTeam(teamId);
        }
        return added;
    }

    /**
     * 移除成员
     *
     * @param teamId
     * @param members
     * @return
     */
    public int deleteMembers(ID teamId, Collection<ID> members) {
        int deleted = 0;
        for (ID m : members) {
            Object[] exists = Application.createQueryNoFilter(
                    "select memberId from TeamMember where teamId = ? and userId = ?")
                    .setParameter(1, teamId)
                    .setParameter(2, m)
                    .unique();
            if (exists != null) {
                super.delete((ID) exists[0]);
                deleted++;
            }
        }

        if (deleted > 0) {
            Application.getUserStore().refreshTeam(teamId);
        }
        return deleted;
    }
}
