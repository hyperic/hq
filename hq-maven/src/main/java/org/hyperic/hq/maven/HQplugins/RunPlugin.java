package org.hyperic.hq.maven.HQplugins;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import com.jcraft.jsch.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * @goal run
 */
public class RunPlugin extends AbstractMojo implements UserInfo, UIKeyboardInteractive {

    Log log = getLog();
    private String remotePass;
    /**
     * @parameter expression="${project.basedir}"
     */
    private File dir;
    /**
     * @parameter expression="${project.version}"
     */
    private String version;
    /**
     * @parameter expression="${args}"
     */
    protected String config;

    public void execute() throws MojoExecutionException {
        JSch.setLogger(new MyLogger());
        log.info("dir=" + dir);
        log.info("version=" + version);
        String plugin = dir.getName().split("-plugin")[0];
        File hq = new File(System.getProperty("user.home"), ".hq");
        File configFile;
        if (config != null) {
            configFile = new File(config);
        } else {
            configFile = new File(hq, dir.getName() + ".properties");
        }
        log.info("Config file:" + configFile);
        File jar = new File(dir, "/target/" + plugin + "-plugin-" + version + ".jar");

        SuperProps props = new SuperProps();
        try {
            props.load(new FileInputStream(configFile));
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new MojoExecutionException("error", ex);
        }
        String javaBin = props.getProperty("java.bin");
        String sudo = props.getProperty("sudo", null);
        String agentBundle = props.getProperty("agent.bundle");
        String server = props.getProperty("server.install", null);
        String logL = props.getProperty("log", "info");
        String method = props.getProperty("method", null);
        String action = props.getProperty("action", null);
        String type = props.getProperty("type", null);
        String pluginArgsS = props.getProperty("plugin.args", "");
        List<String> pluginArgs = Arrays.asList(pluginArgsS.replaceAll("([^\\s]*=)", "-D$1").split(" "));
        boolean debug = props.getProperty("debug", "false").equalsIgnoreCase("true");
        boolean pause = props.getProperty("pause", "false").equalsIgnoreCase("true");
        String agentV = new File(agentBundle).getName().substring("agent-".length());

        String pdkJar = "hq-pdk-" + agentV + ".jar";
        boolean oldStyle = props.getProperty("oldStyle", "false").equalsIgnoreCase("true");
        if (oldStyle) {
            pdkJar = "hq-product.jar";
        }

        List<String> cmd = new ArrayList();
        cmd.add(javaBin);
        if (debug) {
            cmd.add("-Xdebug");
            cmd.add("-Xrunjdwp:transport=dt_socket,address=8998,server=y,suspend=y");
        }
        cmd.add("-ea");
        cmd.add("-jar");
        cmd.add(agentBundle + "/pdk/lib/" + pdkJar);
        cmd.add("-p");
        cmd.add(plugin);
        cmd.add("-m");
        cmd.add(method);
        if (action != null) {
            cmd.add("-a");
            cmd.add(action);
        }
        if (type != null) {
            cmd.add("-t");
            cmd.add("\"" + type + "\"");
        }

        if ((pluginArgs.size() > 0) && (pluginArgs.get(0).trim().length() > 0)) {
            cmd.addAll(pluginArgs);
        }
        cmd.add("-Dpause-on-error=" + pause);
        cmd.add("-Dplugins.include=" + plugin);
        cmd.add("-Dlog=" + logL);

//        if (sudo != null) {
//            cmd = "sudo -u " + sudo + " " + cmd;
//        }

        boolean remote = props.getProperty("remote", "false").equalsIgnoreCase("true");
        boolean copy = props.getProperty("copy", "false").equalsIgnoreCase("true");

        File jarAgentDest = new File(new File(agentBundle), "/pdk/plugins/" + plugin + "-plugin.jar");
        File jarServerDest = null;
        if (server != null) {
            jarServerDest = new File(new File(server), "/hq-engine/hq-server/webapps/ROOT/WEB-INF/hq-plugins/" + plugin + "-plugin.jar");
        }
        log.info("Plugin          => '" + plugin + "'");
        log.info("version         => '" + version + "'");
        log.info("jar             => '" + jar + "' (" + jar.exists() + ")");
        log.info("jarAgentDest    => '" + jarAgentDest + "'");
        log.info("jarServerDest   => '" + jarServerDest + "'");
        log.info("copy            => '" + copy + "'");
        log.info("debug           => '" + debug + "'");
        log.info("pause           => '" + pause + "'");
        log.info("cmd             => '" + join(cmd) + "'");

        boolean copyPdk = props.getProperty("copy-pdk", "false").equalsIgnoreCase("true");
        File pdkOrg = null;
        File pdkDest = null;
        if (copyPdk && !remote) {
            try {
                File pdkLib = new File(new File(agentBundle), "/pdk/lib/");
                String names[] = pdkLib.list(new FilenameFilter() {

                    public boolean accept(File file, String name) {
                        return name.startsWith("hq-pdk") && name.endsWith(".jar");
                    }
                });
                if (names.length != 1) {
                    throw new MojoExecutionException("hq-pdk*.jar not found");
                }

                pdkOrg = new File(dir, "../../hq-pdk/target/hq-pdk-" + version + ".jar");
                pdkDest = new File(pdkLib, names[0]);
                log.info("pdkOrg          => '" + pdkOrg.getCanonicalPath() + "' (" + pdkOrg.exists() + ")");
                log.info("pdkDest         => '" + pdkDest.getCanonicalPath() + "' (" + pdkDest.exists() + ")");
                copy(pdkOrg, pdkDest);
            } catch (IOException ex) {
                throw new MojoExecutionException("error", ex);
            }
        }

        log.info("remote          => '" + remote + "'");
        if (remote) {
            String remoteUser = props.getProperty("remote.user");
            remotePass = props.getProperty("remote.pass");
            String remoteHost = props.getProperty("remote.host");
            log.info("host            => '" + remoteHost + "'");
            log.info("user            => '" + remoteUser + "'");

            if (copy) {
                scpToRemote(remoteUser, remoteHost, jar.getAbsolutePath(), jarAgentDest.getAbsolutePath());
                if (server != null) {
                    scpToRemote(remoteUser, remoteHost, jar.getAbsolutePath(), jarServerDest.getAbsolutePath());
                }
            }
            if (method != null) {
                executeRemote(remoteUser, remoteHost, join(cmd) + " 2>&1");
            }
        } else {
            if (copy) {
                copy(jar, jarAgentDest);
                if (server != null) {
                    copy(jar, jarServerDest);
                }
            }
            if (method != null) {
                executeLocal(cmd, agentBundle);
            }
        }
    }

    private void executeLocal(List<String> argl, String agentBundle) throws MojoExecutionException {
        try {
            String[] args = (String[]) argl.toArray(new String[argl.size()]);
            File workingDir = new File(agentBundle, "../..");
            log.info("user            => '" + workingDir + "'");
            Process cmd = Runtime.getRuntime().exec(args, null, workingDir);
            StreamConnector stdOut = new StreamConnector(cmd.getErrorStream(), System.out);
            StreamConnector errOut = new StreamConnector(cmd.getInputStream(), System.out);
            stdOut.start();
            errOut.start();
            cmd.waitFor();
        } catch (Exception ex) {
            throw new MojoExecutionException("error", ex);
        }
    }

    private void executeRemote(String user, String host, String cmd) throws MojoExecutionException {
        Session session = null;
        Channel channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setConfig("PreferredAuthentications", "password");
            session.setUserInfo(this);
            session.setTimeout(10 * 1000);
            session.connect();
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(cmd);
            channel.setInputStream(System.in);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (!channel.isClosed()) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    System.out.print(new String(tmp, 0, i));
                }
                Thread.sleep(0);
            }
            System.out.println("exit-status: " + channel.getExitStatus());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new MojoExecutionException("error", ex);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    void copy(File src, File dst) throws MojoExecutionException {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
                System.out.print(".");
            }
            System.out.println(".");
        } catch (Exception ex) {
            throw new MojoExecutionException("error", ex);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw new MojoExecutionException("error", ex);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    throw new MojoExecutionException("error", ex);
                }
            }
        }
    }

    public void scpToRemote(String user, String host, String lfile, String rfile) {
        FileInputStream fis = null;
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);

            session.setConfig("PreferredAuthentications", "password");
            session.setUserInfo(this);
            session.connect();


            // exec 'scp -t rfile' remotely
            String command = "scp -p -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();

            channel.connect();

            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize = (new File(lfile)).length();
            command = "C0644 " + filesize + " ";
            if (lfile.lastIndexOf('/') > 0) {
                command += lfile.substring(lfile.lastIndexOf('/') + 1);
            } else {
                command += lfile;
            }
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }

            // send a content of lfile
            fis = new FileInputStream(lfile);
            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) {
                    break;
                }
                out.write(buf, 0, len); //out.flush();
                System.out.print(".");
            }
            System.out.println(".");
            fis.close();
            fis = null;
            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }
            out.close();

            channel.disconnect();
            session.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ee) {
            }
        }
    }

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1

        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }

    static String inputStreamAsString(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    public String getPassphrase() {
        System.out.println("-->getPassphrase");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPassword() {
        System.out.println("-->getPassword");
        return remotePass;
    }

    public boolean promptPassword(String string) {
        System.out.println("-->promptPassword");
        return true;
    }

    public boolean promptPassphrase(String string) {
        System.out.println("-->promptPassphrase");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean promptYesNo(String string) {
        System.out.println("-->promptYesNo");
        return true;
    }

    public void showMessage(String string) {
        System.out.println("-->showMessage");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            sb.append(l.get(i)).append(" ");
        }
        return sb.toString().trim();
    }

    public String[] promptKeyboardInteractive(String string, String string1, String string2, String[] strings, boolean[] blns) {
        return null;
    }

    public static class MyLogger implements com.jcraft.jsch.Logger {

        static Map<Integer, String> name = new java.util.HashMap<Integer, String>();

        static {
            name.put(new Integer(DEBUG), "DEBUG: ");
            name.put(new Integer(INFO), "INFO: ");
            name.put(new Integer(WARN), "WARN: ");
            name.put(new Integer(ERROR), "ERROR: ");
            name.put(new Integer(FATAL), "FATAL: ");
        }

        public boolean isEnabled(int level) {
            return true;
        }

        public void log(int level, String message) {
//            System.err.print(name.get(new Integer(level)));
//            System.err.println(message);
        }
    }
}