import groovy.sql.Sql
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

def ctx = new InitialContext();
def ds = (DataSource) ctx.lookup("java:/HypericDS");
def conn = ds.getConnection();

def sql = new Sql(conn);

def output = "";

sql.eachRow("select l.mode, transaction, l.granted, now() - query_start as time, current_query from pg_locks  l, pg_stat_activity a where l.pid=a.procpid and now() - query_start > '00:00:01'"){

  output += "${it.time} ${it.current_query}"

}

return output;
