import org.hibernate.criterion.Restrictions

def persistedClass = org.hyperic.hq.appdef.server.session.Platform
def sess  = org.hyperic.hibernate.Util.sessionFactory.currentSession

sess.createCriteria(persistedClass).
  setMaxResults(10).
  list()

