package org.hyperic.hq.common.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hyperic.hibernate.JmxExposingLocalSessionFactoryBean;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

public class HqLazyConnectionDataSourceProxy extends LazyConnectionDataSourceProxy {
    
    private Boolean useLazyConnection;
    
    public HqLazyConnectionDataSourceProxy() {
        // XXX would be nicer if we could get the dialect in the constructor and initialize
        // useLazyConnection here rather than in getConnection()
        // issue is that the sessionFactory is not created and relies on the dataSource
        // when it is initialized
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        // main thread launches the app and runs this method, so no need to synchronize
        if (useLazyConnection == null) {
            JmxExposingLocalSessionFactoryBean sessionFactory =
                Bootstrap.getBean(JmxExposingLocalSessionFactoryBean.class);
            HQDialect dialect =
                (HQDialect) Dialect.getDialect(sessionFactory.getConfiguration().getProperties());
            useLazyConnection = dialect.useLazyConnection();
        }
        if (useLazyConnection) {
            return super.getConnection();
        }
        return getTargetDataSource().getConnection();
    }

}
