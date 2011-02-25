/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AgentPluginStatusDAO extends HibernateDAO<AgentPluginStatus> {

    private AgentDAO agentDAO;
    private PluginDAO pluginDAO;

    @Autowired
    public AgentPluginStatusDAO(SessionFactory factory, AgentDAO agentDAO, PluginDAO pluginDAO) {
        super(AgentPluginStatus.class, factory);
        this.agentDAO = agentDAO;
        this.pluginDAO = pluginDAO;
    }
    
    public void saveOrUpdate(AgentPluginStatus agentPluginStatus) {
        save(agentPluginStatus);
    }
    
    /**
     * @return {@link Map} of {@link String} of the jar-name to {@link AgentPluginStatus}
     */
    @SuppressWarnings("unchecked")
    public Map<String, AgentPluginStatus> getPluginStatusByAgent(Agent agent) {
        final List<AgentPluginStatus> list =
            getSession().createQuery("from AgentPluginStatus where agent = :agent")
                        .setParameter("agent", agent)
                        .list();
        final Map<String, AgentPluginStatus> rtn = new HashMap<String, AgentPluginStatus>(list.size());
        for (final AgentPluginStatus status : list) {
            rtn.put(status.getJarName(), status);
        }
        return rtn;
    }

    Map<Plugin, Collection<Agent>> getOutOfSyncAgentsByPlugin() {
        final Map<Plugin, Collection<Agent>> rtn = new HashMap<Plugin, Collection<Agent>>();
        final List<Object[]> list = getOutOfSyncPlugins(null);
        for (final Object[] obj : list) {
            final int agentId = (Integer) obj[0];
            final String pluginName = (String) obj[1];
            final Agent agent = agentDAO.get(agentId);
            final Plugin plugin = pluginDAO.findByName(pluginName);
            Collection<Agent> tmp;
            if (null == (tmp = rtn.get(plugin))) {
                tmp = new ArrayList<Agent>();
                rtn.put(plugin, tmp);
            }
            tmp.add(agent);
        }
        return rtn;
    }

    Map<Agent, Collection<Plugin>> getOutOfSyncPluginsByAgent() {
        final Map<Agent, Collection<Plugin>> rtn = new HashMap<Agent, Collection<Plugin>>();
        final List<Object[]> list = getOutOfSyncPlugins(null);
        for (final Object[] obj : list) {
            final int agentId = (Integer) obj[0];
            final String pluginName = (String) obj[1];
            final Agent agent = agentDAO.get(agentId);
            final Plugin plugin = pluginDAO.findByName(pluginName);
            Collection<Plugin> tmp;
            if (null == (tmp = rtn.get(agent))) {
                tmp = new ArrayList<Plugin>();
                rtn.put(agent, tmp);
            }
            tmp.add(plugin);
        }
        return rtn;
    }
    
    List<String> getOutOfSyncPluginNamesByAgentId(int agentId) {
        final List<Object[]> objs = getOutOfSyncPlugins(agentId);
        final List<String> rtn = new ArrayList<String>(objs.size());
        for (final Object[] obj : objs) {
            final String pluginName = (String) obj[1];
            rtn.add(pluginName);
        }
        return rtn;
    }

    /**
     * @param agentId may be null
     * @return {@link List} of Object[] where
     * [0] = agentId (Integer) and
     * [1] = pluginName (String)
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> getOutOfSyncPlugins(Integer agentId) {
        final String agentSql = agentId == null ? "" : " s.agent_id = :agentId AND ";
        final String sql = new StringBuilder(256)
            .append("select distinct s.agent_id, s.plugin_name ")
            .append("from EAM_AGENT_PLUGIN_STATUS s ")
            .append("where ")
            .append(agentSql)
            .append("not exists ( ")
            .append("    select 1 ")
            .append("    from EAM_PLUGIN p ")
            .append("    join EAM_AGENT_PLUGIN_STATUS st on p.md5 = st.md5 ")
            .append("    where st.agent_id = s.agent_id and s.md5 = st.md5 ")
            .append(")")
            .toString();
        final SQLQuery query = getSession().createSQLQuery(sql);
        if (agentId != null) {
            query.setParameter("agentId", agentId);
        }
        return query.addScalar("agent_id", Hibernate.INTEGER)
                    .addScalar("plugin_name", Hibernate.STRING)
                    .list();
    }

    @SuppressWarnings("unchecked")
    Collection<Integer> getPluginsNotOnAgent(int agentId) {
        final String sql = new StringBuilder(128)
			.append("select p.id ")
            .append("from EAM_PLUGIN p ")
			.append("where not exists (")
			.append("    select 1 from EAM_AGENT_PLUGIN_STATUS ")
			.append("    where agent_id = :agentId and plugin_name = p.name")
			.append(")")
            .toString();
        return getSession().createSQLQuery(sql)
                           .addScalar("id", Hibernate.INTEGER)
                           .setParameter("agentId", agentId)
                           .list();
    }
    
    long getNumAutoUpdatingAgents() {
        final String hql = "select count(distinct Agent) from AgentPluginStatus";
        final Long i = (Long) getSession().createQuery(hql).uniqueResult();
        return (i == null) ? 0 : i;
    }

    @SuppressWarnings("unchecked")
    public Map<String, AgentPluginStatus> getStatusByAgentId(Integer agentId) {
        final String hql = "from AgentPluginStatus where agent.id = :agentId";
        final Collection<AgentPluginStatus> list =
            getSession().createQuery(hql)
                        .setParameter("agentId", agentId)
                        .list();
        final Map<String, AgentPluginStatus> rtn =
            new HashMap<String, AgentPluginStatus>(list.size());
        for (final AgentPluginStatus status : list) {
            rtn.put(status.getPluginName(), status);
        }
        return rtn;
    }

}
