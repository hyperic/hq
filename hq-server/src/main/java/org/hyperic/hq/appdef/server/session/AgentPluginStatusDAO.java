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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.product.Plugin;
import org.hyperic.hq.product.server.session.PluginDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AgentPluginStatusDAO extends HibernateDAO<AgentPluginStatus> {

    private static final String LIMIT_S_TO_CURRENT_AGENTS = "join EAM_AGENT agent on s.agent_id = agent.id " +         
    		"where agent.version >= :serverVersion ";
    private static final String SYNCHABLE_AGENT_IDS_QUERY_STRING = 	"select distinct agent_id from EAM_AGENT_PLUGIN_STATUS s "+
    		LIMIT_S_TO_CURRENT_AGENTS + "and exists (select 1 from EAM_PLATFORM p where p.agent_id = s.agent_id)";
    private static final String CUR_UNSYNCHABLE_AGENT_IDS_QUERY_STRING = 
                "select distinct id from EAM_AGENT where version >= :serverVersion and " + 
                " id not in (" + SYNCHABLE_AGENT_IDS_QUERY_STRING + ") ";
 
    
    private final AgentDAO agentDAO;
    private final PluginDAO pluginDAO;
    private final ServerConfigManager serverConfigManager;

    @Autowired
    public AgentPluginStatusDAO(SessionFactory factory, AgentDAO agentDAO, PluginDAO pluginDAO,
            ServerConfigManager serverConfigManager) {
        super(AgentPluginStatus.class, factory);
        this.agentDAO = agentDAO;
        this.pluginDAO = pluginDAO;
        this.serverConfigManager = serverConfigManager;
    }
    
    @PostConstruct
    public void initCache() {
        new HibernateTemplate(sessionFactory, true).execute(new HibernateCallback<Object>() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.createQuery("from AgentPluginStatus").list();
                return null;
            }
        });
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
            rtn.put(status.getFileName(), status);
        }
        return rtn;
    }

    public Map<Plugin, Collection<AgentPluginStatus>> getOutOfSyncAgentsByPlugin() {
        final Map<Plugin, Collection<AgentPluginStatus>> rtn =
            new HashMap<Plugin, Collection<AgentPluginStatus>>();
        final List<Integer> list = getOutOfSyncPlugins(null);
        for (final Integer id : list) {
            final AgentPluginStatus st = get(id);
            final String pluginName = st.getPluginName();
            final Plugin plugin = pluginDAO.findByName(pluginName);
            Collection<AgentPluginStatus> tmp;
            if (null == (tmp = rtn.get(plugin))) {
                tmp = new ArrayList<AgentPluginStatus>();
                rtn.put(plugin, tmp);
            }
            tmp.add(st);
        }
        return rtn;
    }
       
    Map<Agent, Collection<AgentPluginStatus>> getOutOfSyncPluginsByAgent() {
        final Map<Agent, Collection<AgentPluginStatus>> rtn =
            new HashMap<Agent, Collection<AgentPluginStatus>>();
        final List<Integer> list = getOutOfSyncPlugins(null);
        for (final Integer id : list) {
            final AgentPluginStatus st = get(id);
            final int agentId = st.getAgent().getId();
            final Agent agent = agentDAO.get(agentId);
            Collection<AgentPluginStatus> tmp;
            if (null == (tmp = rtn.get(agent))) {
                tmp = new ArrayList<AgentPluginStatus>();
                rtn.put(agent, tmp);
            }
            tmp.add(st);
        }
        return rtn;
    }

    public List<String> getOutOfSyncPluginNamesByAgentId(int agentId) {
        final List<Integer> ids = getOutOfSyncPlugins(agentId);
        final List<String> rtn = new ArrayList<String>(ids.size());
        for (final Integer id : ids) {
            final AgentPluginStatus st = get(id);
            final String pluginName = st.getPluginName();
            rtn.add(pluginName);
        }
        return rtn;
    }

    
    /**
     * Get our of sync plugins for agents, whose version is not older than that of
     * the server.
     * @param agentId may be null
     * @return {@link List} of {@link Integer} which represents the AgentPluginStatusId
     */
    @SuppressWarnings("unchecked")
    private List<Integer> getOutOfSyncPlugins(Integer agentId) {
        String serverMajorVersion = serverConfigManager.getServerMajorVersion();
        final String agentSql = agentId == null ? "" : " s.agent_id = :agentId AND ";
        final String sql = new StringBuilder(256)            
            .append("select distinct s.id ")
            .append("from EAM_AGENT_PLUGIN_STATUS s ")
            .append(LIMIT_S_TO_CURRENT_AGENTS)
            .append(" AND (")
            .append(agentSql)
            .append("not exists ( ")
            .append("    select 1 ")
            .append("    from EAM_PLUGIN p ")
            .append("    join EAM_AGENT_PLUGIN_STATUS st on p.md5 = st.md5 ")
            .append("    where st.agent_id = s.agent_id and s.md5 = st.md5 ")
            .append("    and p.deleted = '0'")
            .append(") ")
            .append("OR s.last_sync_status != :syncSuccess)")
            .toString();
        final SQLQuery query = getSession().createSQLQuery(sql);
        if (agentId != null) {
            query.setParameter("agentId", agentId);
        }
        query.setParameter("serverVersion", serverMajorVersion);
        return query.addScalar("id", Hibernate.INTEGER)
                    .setParameter("syncSuccess", AgentPluginStatusEnum.SYNC_SUCCESS.toString())
                    .list();
    }    
    
    /**
     * @return {@link Collection} of {@link Object[]} where [0] = agentId and [1] = pluginName
     */
    @SuppressWarnings("unchecked")
    Collection<Object[]> getPluginsNotOnAllAgents() {
        final String sql = new StringBuilder(256)
            .append("SELECT distinct a.id,p.name from EAM_PLUGIN p, EAM_AGENT a ")
			.append("JOIN EAM_PLATFORM pl on pl.agent_id = a.id ")
			.append("WHERE not exists ( ")
			.append("    SELECT 1 FROM EAM_AGENT_PLUGIN_STATUS s ")
			.append("    WHERE a.id = s.agent_id and s.plugin_name = p.name ")
			.append(") and p.deleted = '0'")
			.toString();
        return getSession().createSQLQuery(sql)
                           .addScalar("id", Hibernate.INTEGER)
                           .addScalar("name", Hibernate.STRING)
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
			.append(") and p.deleted = '0'")
            .toString();
        return getSession().createSQLQuery(sql)
                           .addScalar("id", Hibernate.INTEGER)
                           .setParameter("agentId", agentId)
                           .list();
    }

    public Map<String, AgentPluginStatus> getStatusByAgentId(Integer agentId) {
        final String hql = "from AgentPluginStatus where agent.id = :agentId";
        @SuppressWarnings("unchecked")
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

    public Map<Integer, Map<String, AgentPluginStatus>> getStatusByAgentIds(Collection<Integer> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        final String hql = "from AgentPluginStatus where agent.id in (:agentIds)";
        @SuppressWarnings("unchecked")
        final Collection<AgentPluginStatus> list =
            getSession().createQuery(hql)
                        .setParameterList("agentIds", agentIds, new IntegerType())
                        .list();
        final Map<Integer, Map<String, AgentPluginStatus>> rtn =
            new HashMap<Integer, Map<String, AgentPluginStatus>>(list.size());
        for (final AgentPluginStatus status : list) {
            final Integer agentId = status.getAgent().getId();
            Map<String, AgentPluginStatus> map = rtn.get(status.getAgent().getId());
            if (map == null) {
                map = new HashMap<String, AgentPluginStatus>();
                rtn.put(agentId, map);
            }
            map.put(status.getPluginName(), status);
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Collection<AgentPluginStatus> getPluginStatusByFileName(String fileName,
                                                      Collection<AgentPluginStatusEnum> statuses) {
        final String hql = new StringBuilder(128)
            .append("from AgentPluginStatus s ")
            .append("where s.fileName = :fileName and s.lastSyncStatus in (:statuses) ")
            .append("and exists (select 1 from Platform p where p.agent.id = s.agent.id)")
            .toString();
        Collection<String> vals = new ArrayList<String>(statuses.size());
        for (final AgentPluginStatusEnum s : statuses) {
            vals.add(s.toString());
        }
        return getSession().createQuery(hql)
                           .setParameter("fileName", fileName)
                           .setParameterList("statuses", vals, new StringType())
                           .list();
    }

    
    
    @SuppressWarnings("unchecked")
    public Set<Integer> getAutoUpdatingAgentIDs() {
        String serverMajorVersion = serverConfigManager.getServerMajorVersion();
        final SQLQuery query = getSession().createSQLQuery(SYNCHABLE_AGENT_IDS_QUERY_STRING);
        query.setParameter("serverVersion", serverMajorVersion);
        final List<Integer> ids = query.addScalar("agent_id", Hibernate.INTEGER).list();
        final Set<Integer> idsSet = new HashSet<Integer>(ids);
        return idsSet;
    }
    

    @SuppressWarnings("unchecked")
   public Collection<Agent> getAutoUpdatingAgents() {
        final Set<Integer> idsSet = getAutoUpdatingAgentIDs();
        final List<Agent> rtn = new ArrayList<Agent>(idsSet.size());
        for (final Integer agentId : idsSet) {
            rtn.add(agentDAO.findById(agentId));
        }
        return rtn;
    }
   
   
   @SuppressWarnings("unchecked")
   public List<Agent> getCurrentNonSyncAgents() {
       String serverMajorVersion = serverConfigManager.getServerMajorVersion();
       final SQLQuery query = getSession().createSQLQuery(CUR_UNSYNCHABLE_AGENT_IDS_QUERY_STRING);
       query.setParameter("serverVersion", serverMajorVersion);
       final List<Integer> ids = query.addScalar("id", Hibernate.INTEGER).list();
       final Set<Integer> idsSet = new HashSet<Integer>(ids);

       final List<Agent> rtn = new ArrayList<Agent>(idsSet.size());
       for (final Integer agentId : idsSet) {
           rtn.add(agentDAO.findById(agentId));
       }
       return rtn;
	}   
   
   
   
   public Long getNumAutoUpdatingAgents() {
	   Set<Integer> synchedAgentIDs = getAutoUpdatingAgentIDs();
       return ((long)synchedAgentIDs.size());
   }


/*
    @SuppressWarnings("unchecked")
    public Collection<Agent> getAutoUpdatingAgents() {
        String serverMajorVersion = serverConfigManager.getServerMajorVersion();
        final String hql = new StringBuilder(150)
            .append("select distinct agent_id from EAM_AGENT_PLUGIN_STATUS s ")
            .append(LIMIT_S_TO_CURRENT_AGENTS)
            .append("and exists (select 1 from EAM_PLATFORM p where p.agent_id = s.agent_id)")
            .toString();
        final SQLQuery query = getSession().createSQLQuery(hql);
        query.setParameter("serverVersion", serverMajorVersion);
        final List<Integer> ids = query.addScalar("agent_id", Hibernate.INTEGER).list();
        final List<Agent> rtn = new ArrayList<Agent>(ids.size());
        for (final Integer agentId : ids) {
            rtn.add(agentDAO.findById(agentId));
        }
        return rtn;
    }
*/

    public void removeAgentPluginStatuses(Integer agentId, Collection<String> pluginFileNames) {
        final String hql =
            "select id from AgentPluginStatus where agent.id = :agentId and fileName in (:filenames)";
        @SuppressWarnings("unchecked")
        final List<Integer> list =
            getSession().createQuery(hql)
                        .setParameter("agentId", agentId, new IntegerType())
                        .setParameterList("filenames", pluginFileNames)
                        .list();
        for (final Integer sapsId : list) {
            AgentPluginStatus status = get(sapsId);
            if (status == null) {
                continue;
            }
            remove(status);
        }
    }

    public Map<Agent, Collection<AgentPluginStatus>> getPluginsToRemoveFromAgents() {
        final String hql = new StringBuilder(64)
            .append("select s.id FROM EAM_AGENT_PLUGIN_STATUS s ")
            .append("where not exists (")
            .append("select 1 from EAM_PLUGIN p where p.name = s.plugin_name and p.deleted = '0')")
            .toString();
        @SuppressWarnings("unchecked")
        final List<Integer> list =
            getSession().createSQLQuery(hql)
                        .addScalar("id", Hibernate.INTEGER)
                        .list();
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Agent, Collection<AgentPluginStatus>> rtn =
            new HashMap<Agent, Collection<AgentPluginStatus>>(list.size());
        for (final Integer sapsId : list) {
            final AgentPluginStatus status = get(sapsId);
            if (status == null) {
                continue;
            }
            final Agent agent = status.getAgent();
            if (agent == null) {
                continue;
            }
            Collection<AgentPluginStatus> tmp = rtn.get(agent);
            if (tmp == null) {
                tmp = new ArrayList<AgentPluginStatus>();
                rtn.put(agent, tmp);
            }
            tmp.add(status);
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Collection<AgentPluginStatus> getStatusByAgentAndFileNames(Integer agentId,
                                                                      Collection<String> fileNames) {
        final String hql =
            "from AgentPluginStatus where agent.id = :agentId AND fileName in (:fileNames)";
        return getSession().createQuery(hql)
                           .setParameterList("fileNames", fileNames)
                           .setInteger("agentId", agentId)
                           .list();
    }

    public Map<Agent, AgentPluginStatus> getPluginStatusByFileName(String fileName) {
        final String hql = "select id from AgentPluginStatus where fileName = :fileName";
        @SuppressWarnings("unchecked")
        final List<Integer> list =
            getSession().createQuery(hql).setParameter("fileName", fileName).list();
        final Map<Agent, AgentPluginStatus> rtn = new HashMap<Agent, AgentPluginStatus>(list.size());
        for (final Integer sapsId : list) {
            final AgentPluginStatus status = get(sapsId);
            if (status == null) {
                continue;
            }
            final Agent agent = status.getAgent();
            if (agent == null) {
                continue;
            }
            rtn.put(agent, status);
        }
        return rtn;
    }

    public Map<String, Long> getFileNameCounts(Collection<String> pluginFileNames) {
        if (pluginFileNames != null && pluginFileNames.isEmpty()) {
            return Collections.emptyMap();
        }
        String and = "";
        if (pluginFileNames != null) {
            and = "and s.fileName in (:filenames) ";
        }
        final String hql = new StringBuilder(256)
            .append("select s.fileName, count(*) from AgentPluginStatus s ")
            .append("where exists (")
                .append("select 1 from Agent a ")
                .append("join a.platforms p ")
                .append("where a.id = s.agent.id) ")
            .append(and)
            .append("group by s.fileName")
            .toString();
        final Query query = getSession().createQuery(hql);
        if (pluginFileNames != null) {
            query.setParameterList("filenames", pluginFileNames);
        }
        @SuppressWarnings("unchecked")
        final List<Object[]> list = query.list();
        final Map<String, Long> rtn = new HashMap<String, Long>(list.size());
        for (final Object[] obj : list) {
            rtn.put((String) obj[0], ((Number) obj[1]).longValue());
        }
        return rtn;
    }

    public Map<String, Long> getFileNameCounts() {
        return getFileNameCounts(null);
    }

    @SuppressWarnings("unchecked")
    public Collection<Plugin> getOrphanedPlugins() {
        final String hql = new StringBuilder(200)
            .append("from Plugin p where deleted = '1' and not exists (")
            .append("    select 1 from AgentPluginStatus s")
          	.append("    join s.agent a")
          	.append("    join a.platforms pl")
          	.append("    where s.fileName = p.path")
          	.append(")")
            .toString();
        return getSession().createQuery(hql).list();
    }

    @SuppressWarnings("unchecked")
    public List<AgentPluginStatus> getPluginStatusByAgent(AgentPluginStatusEnum ... keys) {
        final List<String> vals = new ArrayList<String>(keys.length);
        for (AgentPluginStatusEnum key : keys) {
            vals.add(key.toString());
        }
        final String hql = new StringBuilder(128)
            .append("from AgentPluginStatus s ")
            .append("where exists (select 1 from Platform p where p.agent.id = s.agent.id) ")
            .append("and s.lastSyncStatus in (:statuses)")
            .toString();
        return getSession()
            .createQuery(hql)
            .setParameterList("statuses", vals, new StringType())
            .list();
    }
    
    @SuppressWarnings("unchecked")
    public List<Integer> getAllIds() {
        final String hql = "select id from AgentPluginStatus";
        return getSession().createQuery(hql).list();
    }

}
