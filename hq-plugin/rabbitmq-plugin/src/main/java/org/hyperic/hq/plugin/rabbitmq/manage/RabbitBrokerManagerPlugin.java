/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.manage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.rabbitmq.core.AMQPStatus;
import org.hyperic.hq.plugin.rabbitmq.core.RabbitGateway;
import org.hyperic.hq.plugin.rabbitmq.product.RabbitProductPlugin;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

/**
 * RabbitBrokerManagerPlugin
 *
 * @author Helena Edelson
 */
public class RabbitBrokerManagerPlugin extends ControlPlugin {

    private static final Log logger = LogFactory.getLog(RabbitBrokerManagerPlugin.class);

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
    }

    public void doAction(String action, String[] args) throws PluginException {
        setResult(ControlPlugin.RESULT_FAILURE);
        logger.debug("Received " + args.length + " args");
        for (String s : args) {
            s = s.trim();
        }
        
        RabbitGateway rabbitGateway = RabbitProductPlugin.getRabbitGateway();
        if (rabbitGateway != null) {

            try {

                if (action.equals("createQueue")) {
                    if (args.length == 1) {
                        AMQPStatus status = rabbitGateway.createQueue(args[0]);

                        if (status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to create new Queue: " + args[1]);
                        }

                    } else {
                        throw new PluginException("To create a new Queue please pass in a new Queue name.");
                    }
                    
                } else if (action.equals("deleteQueue")) {

                    if (args.length == 1) {
                        AMQPStatus status = rabbitGateway.deleteQueue(args[1]);

                        if (status.compareTo(AMQPStatus.NO_CONTENT) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to delete Queue: " + args[1]);
                        }

                    } else {
                        throw new PluginException("To delete a Queue please pass in an existing Queue name.");
                    }

                } else if (action.equals("purgeQueue")) {
                    if (args.length == 1) {
                        AMQPStatus status = rabbitGateway.purgeQueue(args[1]);

                        if (status.compareTo(AMQPStatus.NO_CONTENT) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to purge Queue: " + args[1]);
                        }

                    } else {
                        throw new PluginException("To purge a Queue please pass in a Queue name.");
                    }

                } else if (action.equals("createExchange")) {
 
                    if (args.length == 2) {
                        String exchangeName = args[1];
                        String exchangeType = args[2];
                        AMQPStatus status = rabbitGateway.createExchange(exchangeName, exchangeType);

                        if (status.compareTo(AMQPStatus.NO_CONTENT) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to create Exchange: " + args[1]);
                        }

                    } else {
                        throw new PluginException("To create an Exchange please pass in a new Exchange name.");
                    }

                } else if (action.equals("deleteExchangeIfUnused")) {

                     if (args.length == 2) {
                        String exchangeName = args[1];
                        AMQPStatus status = rabbitGateway.deleteExchange(exchangeName, true);

                        if (status.compareTo(AMQPStatus.NO_CONTENT) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to create Exchange: " + args[1]);
                        }

                    } else {
                        throw new PluginException("To delete Exchange: please pass in an existing Exchange name.");
                    }
                }
                else if (action.equals("createUser")) {
                    if (args.length == 2) {
                        String username = args[1];
                        String password = args[2];
                        AMQPStatus status = rabbitGateway.createUser(username, password);

                        if (status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to create User: " + args[1]);
                        }
                    } else {
                        throw new PluginException("To create a User: please pass in a new user name and password.");
                    }
                }
                else if (action.equals("updateUserPassword")) {
                    if (args.length == 2) {
                        String username = args[1];
                        String password = args[2];
                        AMQPStatus status = rabbitGateway.updateUserPassword(username, password);

                        if (status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to update User: " + args[1]);
                        }
                    } else {
                        throw new PluginException("To update a User: please pass in an existing user name and new password.");
                    }
                }
                else if (action.equals("deleteUser")) {
                    if (args.length == 1) {
                        AMQPStatus status = rabbitGateway.deleteUser(args[1]);
                        if (status.compareTo(AMQPStatus.NO_CONTENT) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                        } else {
                            handleResult(status, "Failed to delete User: " + args[1]);
                        }
                    } else {
                        throw new PluginException("To delete a User: please pass in an existing user name.");
                    }
                }
                else if (action.equals("startBrokerApplication")) {
                    AMQPStatus status = rabbitGateway.startBrokerApplication();
                    if (status.compareTo(AMQPStatus.SUCCESS) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                    } else {
                            handleResult(status, "Failed to start broker app.");
                    }
                }
                else if (action.equals("stopBrokerApplication")) {
                    AMQPStatus status = rabbitGateway.stopBrokerApplication();
                    if (status.compareTo(AMQPStatus.SUCCESS) == 0) {
                            setResult(ControlPlugin.RESULT_SUCCESS);
                    } else {
                            handleResult(status, "Failed to stop broker app.");
                    }
                }
                /** test for RabbitMQ_HOME set - a requirement */
                /* else if (action.equals("stopNode")) { 
                    AMQPStatus status = rabbitGateway.stopRabbitNode();
                }
                else if (action.equals("startNode")) {
                    AMQPStatus status = rabbitGateway.startRabbitNode();
                }*/
                else {
                    throw new PluginException("Unsupported action: " + action);
                }


            }
            catch (PluginException e) {
                setMessage(e.getMessage());
                throw e;
            }
            catch (Exception e) {
                setMessage(e.getMessage());
                throw new PluginException("", e);
            }
            finally {
                /** cleanup */
            }
        }
    }

    private void handleResult(AMQPStatus status, String message) {
        setMessage(new StringBuilder(message).append(": ").append(status).toString());
    }


}
