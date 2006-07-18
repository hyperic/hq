import java.util.*;
import javax.naming.*;
import javax.management.*;

import weblogic.jndi.Environment;
import weblogic.management.*;

/**
 * help understand weblogic ObjectName patterns,
 * prints out mbeans in a tree view.
 */
public class WeblogicTree {

    public static void main(String[] args) throws Exception {

        if (args.length > 0) {
            if (args.length < 3) {
                System.out.println("usage: hostname username password");
                return;
            }
        }
        else {
            args = new String[]{"localhost", "weblogic", "weblogic"};
        }

        String url = "t3://" + args[0] + ":7001";
        String username = args[1];
        String password = args[2];

        Environment env = new Environment();

        env.setProviderUrl(url);
        env.setSecurityPrincipal(username);
        env.setSecurityCredentials(password);

        Context ctx = env.getInitialContext();

        MBeanHome home;

        try {
            home = (MBeanHome)ctx.lookup(MBeanHome.ADMIN_JNDI_NAME);
        } finally {
            ctx.close();
        }

        HashMap nodes = new HashMap();

        RemoteMBeanServer mServer = home.getMBeanServer();

        Set beans = home.getAllMBeans();

        for (Iterator it = beans.iterator();
             it.hasNext();) {

            WebLogicMBean bean = (WebLogicMBean)it.next();
            ArrayList tree = new ArrayList();
            WebLogicMBean parent = bean;

            while ((parent = parent.getParent()) != null) {
                tree.add(parent);
            }

            nodes.put(bean, tree);
        }

        for (Iterator it = nodes.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            WebLogicMBean child = (WebLogicMBean)entry.getKey();
            ArrayList tree = (ArrayList)entry.getValue();

            String indent = "";

            for (int i=tree.size()-1; i>=0; i--) {
                WebLogicMBean bean = (WebLogicMBean)tree.get(i);
                System.out.println(indent +
                                   bean.getType() + " = " + bean.getName());
                indent += "   ";
            }

            System.out.println(indent +
                               child.getType() + " = " + child.getName() +
                               "\n\n");
        }
    }
}
