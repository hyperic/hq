INSERT INTO EAM_AGENT_TYPE (ID,NAME) VALUES ('1','covalent-eam')
INSERT INTO EAM_AGENT_TYPE (ID,NAME) VALUES ('2','hyperic-hq-remoting')
INSERT INTO EAM_PRINCIPAL (PASSWORD,PRINCIPAL,id) VALUES ('XfLzwfNQujo/CxxaYX3OCg==','hqadmin','1')
INSERT INTO EAM_SUBJECT (DEPARTMENT,DSN,FACTIVE,FIRST_NAME,FSYSTEM,HTML_EMAIL,ID,LAST_NAME,NAME) VALUES ('Administration','covalentAuthzInternalDsn',1,'System',1,0,'0','User','admin')
INSERT INTO EAM_SUBJECT (DSN,EMAIL_ADDRESS,FACTIVE,FIRST_NAME,FSYSTEM,HTML_EMAIL,ID,LAST_NAME,NAME) VALUES ('CAM','${server.admin.email}',1,'HQ',1,0,'1','Administrator','hqadmin')
INSERT INTO EAM_ROLE (FSYSTEM,ID,NAME) VALUES (1,'0','Super User Role')
INSERT INTO EAM_SUBJECT_ROLE_MAP (ROLE_ID,SUBJECT_ID) VALUES ('0','1')
INSERT INTO EAM_ROLE (FSYSTEM,ID,NAME) VALUES (1,'1','RESOURCE_CREATOR_ROLE')
INSERT INTO EAM_SUBJECT_ROLE_MAP (ROLE_ID,SUBJECT_ID) VALUES ('1','1')
INSERT INTO EAM_CRISPO (ID) VALUES ('0')
INSERT INTO EAM_CRISPO (ID) VALUES ('2')
INSERT INTO EAM_CRISPO (ID) VALUES ('3')
INSERT INTO EAM_SUBJECT (DSN,EMAIL_ADDRESS,FACTIVE,FIRST_NAME,FSYSTEM,HTML_EMAIL,ID,LAST_NAME,NAME,PREF_CRISPO_ID) VALUES ('CAM','${server.admin.email}',0,'Guest',0,0,'2','User','guest','3')
INSERT INTO EAM_CRISPO_OPT (CRISPO_ID,ID,PROPKEY,VAL) VALUES ('3','2','.user.dashboard.default.id','2')
INSERT INTO EAM_ROLE (FSYSTEM,ID,NAME) VALUES (0,'2','Guest Role')
INSERT INTO EAM_DASH_CONFIG (CONFIG_TYPE,CRISPO_ID,ID,NAME,ROLE_ID) VALUES ('ROLE','0','0','Super User Role','0')
INSERT INTO EAM_DASH_CONFIG (CONFIG_TYPE,CRISPO_ID,ID,NAME,ROLE_ID) VALUES ('ROLE','2','2','Guest Role','2')
INSERT INTO EAM_SUBJECT_ROLE_MAP (ROLE_ID,SUBJECT_ID) VALUES ('2','2')
INSERT INTO EAM_ALERT_DEFINITION  (ACTIVE,CONTROL_FILTERED,CTIME,DELETED,ENABLED,FREQUENCY_TYPE,ID,MTIME,NAME,NOTIFY_FILTERED,PRIORITY,VERSION_COL,WILL_RECOVER) VALUES (0,0,'0',0,0,'0','0','0','Resource Type Alert',0,'0','0',0)
INSERT INTO EAM_ALERT_DEF_STATE  (ALERT_DEFINITION_ID,LAST_FIRED) VALUES ('0','0')
INSERT INTO EAM_ESCALATION (ALLOW_PAUSE,CTIME,DESCRIPTION,FREPEAT,ID,MAX_WAIT_TIME,MTIME,NAME,NOTIFY_ALL,VERSION_COL) VALUES (0,'0','This is an Escalation Scheme created by HQ that performs no actions',0,'100','300000','0','Default Escalation',0,'0')
INSERT INTO EAM_MEASUREMENT_CAT (ID,NAME) VALUES ('1','AVAILABILITY')
INSERT INTO EAM_MEASUREMENT_CAT (ID,NAME) VALUES ('2','PERFORMANCE')
INSERT INTO EAM_MEASUREMENT_CAT (ID,NAME) VALUES ('3','THROUGHPUT')
INSERT INTO EAM_MEASUREMENT_CAT (ID,NAME) VALUES ('4','UTILIZATION')
INSERT INTO EAM_NUMBERS (I) VALUES ('0')
INSERT INTO EAM_NUMBERS (I) VALUES ('1')
INSERT INTO EAM_NUMBERS (I) VALUES ('2')
INSERT INTO EAM_NUMBERS (I) VALUES ('3')
INSERT INTO EAM_NUMBERS (I) VALUES ('4')
INSERT INTO EAM_NUMBERS (I) VALUES ('5')
INSERT INTO EAM_NUMBERS (I) VALUES ('6')
INSERT INTO EAM_NUMBERS (I) VALUES ('7')
INSERT INTO EAM_NUMBERS (I) VALUES ('8')
INSERT INTO EAM_NUMBERS (I) VALUES ('9')
INSERT INTO EAM_NUMBERS (I) VALUES ('10')
INSERT INTO EAM_NUMBERS (I) VALUES ('11')
INSERT INTO EAM_NUMBERS (I) VALUES ('12')
INSERT INTO EAM_NUMBERS (I) VALUES ('13')
INSERT INTO EAM_NUMBERS (I) VALUES ('14')
INSERT INTO EAM_NUMBERS (I) VALUES ('15')
INSERT INTO EAM_NUMBERS (I) VALUES ('16')
INSERT INTO EAM_NUMBERS (I) VALUES ('17')
INSERT INTO EAM_NUMBERS (I) VALUES ('18')
INSERT INTO EAM_NUMBERS (I) VALUES ('19')
INSERT INTO EAM_NUMBERS (I) VALUES ('20')
INSERT INTO EAM_NUMBERS (I) VALUES ('21')
INSERT INTO EAM_NUMBERS (I) VALUES ('22')
INSERT INTO EAM_NUMBERS (I) VALUES ('23')
INSERT INTO EAM_NUMBERS (I) VALUES ('24')
INSERT INTO EAM_NUMBERS (I) VALUES ('25')
INSERT INTO EAM_NUMBERS (I) VALUES ('26')
INSERT INTO EAM_NUMBERS (I) VALUES ('27')
INSERT INTO EAM_NUMBERS (I) VALUES ('28')
INSERT INTO EAM_NUMBERS (I) VALUES ('29')
INSERT INTO EAM_NUMBERS (I) VALUES ('30')
INSERT INTO EAM_NUMBERS (I) VALUES ('31')
INSERT INTO EAM_NUMBERS (I) VALUES ('32')
INSERT INTO EAM_NUMBERS (I) VALUES ('33')
INSERT INTO EAM_NUMBERS (I) VALUES ('34')
INSERT INTO EAM_NUMBERS (I) VALUES ('35')
INSERT INTO EAM_NUMBERS (I) VALUES ('36')
INSERT INTO EAM_NUMBERS (I) VALUES ('37')
INSERT INTO EAM_NUMBERS (I) VALUES ('38')
INSERT INTO EAM_NUMBERS (I) VALUES ('39')
INSERT INTO EAM_NUMBERS (I) VALUES ('40')
INSERT INTO EAM_NUMBERS (I) VALUES ('41')
INSERT INTO EAM_NUMBERS (I) VALUES ('42')
INSERT INTO EAM_NUMBERS (I) VALUES ('43')
INSERT INTO EAM_NUMBERS (I) VALUES ('44')
INSERT INTO EAM_NUMBERS (I) VALUES ('45')
INSERT INTO EAM_NUMBERS (I) VALUES ('46')
INSERT INTO EAM_NUMBERS (I) VALUES ('47')
INSERT INTO EAM_NUMBERS (I) VALUES ('48')
INSERT INTO EAM_NUMBERS (I) VALUES ('49')
INSERT INTO EAM_NUMBERS (I) VALUES ('50')
INSERT INTO EAM_NUMBERS (I) VALUES ('51')
INSERT INTO EAM_NUMBERS (I) VALUES ('52')
INSERT INTO EAM_NUMBERS (I) VALUES ('53')
INSERT INTO EAM_NUMBERS (I) VALUES ('54')
INSERT INTO EAM_NUMBERS (I) VALUES ('55')
INSERT INTO EAM_NUMBERS (I) VALUES ('56')
INSERT INTO EAM_NUMBERS (I) VALUES ('57')
INSERT INTO EAM_NUMBERS (I) VALUES ('58')
INSERT INTO EAM_NUMBERS (I) VALUES ('59')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('REPLACE_ME',1,'1','CAM_SERVER_VERSION','${hq.version}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('REPLACE_ME',1,'2','CAM_SCHEMA_VERSION','@@@CAM_SCHEMA_VERSION@@@')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('JDBC',0,'3','CAM_JAAS_PROVIDER','@@@JAASPROVIDER@@@')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('${server.webapp.baseurl}',0,'4','CAM_BASE_URL','${server.webapp.baseurl}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('${server.mail.host}',0,'7','CAM_SMTP_HOST','${server.mail.host}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('${server.mail.sender}',0,'8','CAM_EMAIL_SENDER','${server.mail.sender}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('web',0,'9','CAM_HELP_USER','web')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('user',0,'10','CAM_HELP_PASSWORD','user')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('com.sun.jndi.ldap.LdapCtxFactory',0,'11','CAM_LDAP_NAMING_FACTORY_INITIAL','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('ldap://localhost/',0,'12','CAM_LDAP_NAMING_PROVIDER_URL','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('cn',0,'13','CAM_LDAP_LOGIN_PROPERTY','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('o=Hyperic,c=US',0,'14','CAM_LDAP_BASE_DN','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'15','CAM_LDAP_BIND_DN','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'16','CAM_LDAP_BIND_PW','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'17','CAM_LDAP_PROTOCOL','@@@LDAPPROTOCOL@@@')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'18','CAM_LDAP_FILTER','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'19','CAM_MULTICAST_ADDRESS','${server.multicast.addr}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'20','CAM_MULTICAST_PORT','${server.multicast.port}')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('false',0,'21','CAM_SYSLOG_ACTIONS_ENABLED','false')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'23','CAM_GUIDE_ENABLED','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'24','CAM_RT_COLLECT_IP_ADDRS','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('172800000',0,'25','CAM_DATA_PURGE_RAW','172800000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('1209600000',0,'26','CAM_DATA_PURGE_1H','1209600000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('2678400000',0,'27','CAM_DATA_PURGE_6H','2678400000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('31536000000',0,'28','CAM_DATA_PURGE_1D','31536000000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('259200000',0,'29','CAM_BASELINE_FREQUENCY','259200000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('604800000',0,'30','CAM_BASELINE_DATASET','604800000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('40',0,'31','CAM_BASELINE_MINSET','40')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('3600000',0,'32','CAM_DATA_MAINTENANCE','3600000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'33','DATA_STORE_ALL','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('2678400000',0,'34','RT_DATA_PURGE','2678400000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'35','DATA_REINDEX_NIGHTLY','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('2678400000',0,'36','ALERT_PURGE','2678400000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'37','SNMP_VERSION','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'38','SNMP_AUTH_PROTOCOL','MD5')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'39','SNMP_AUTH_PASSPHRASE','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'40','SNMP_PRIV_PASSPHRASE','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'41','SNMP_COMMUNITY','public')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'42','SNMP_ENGINE_ID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'43','SNMP_CONTEXT_NAME','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'44','SNMP_SECURITY_NAME','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'45','SNMP_TRAP_OID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'46','SNMP_ENTERPRISE_OID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'47','SNMP_GENERIC_ID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'48','SNMP_SPECIFIC_ID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'49','SNMP_AGENT_ADDRESS','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'50','SNMP_PRIVACY_PROTOCOL','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('2678400000',0,'51','EVENT_LOG_PURGE','2678400000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'52','KERBEROS_REALM','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'53','KERBEROS_KDC','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'54','KERBEROS_DEBUG','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'55','HQ-GUID','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'56','BATCH_AGGREGATE_WORKERS','10')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'57','BATCH_AGGREGATE_BATCHSIZE','1000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'58','BATCH_AGGREGATE_QUEUE','500000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('1000',0,'59','REPORT_STATS_SIZE','1000')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'60','AGENT_BUNDLE_REPOSITORY_DIR','hq-agent-bundles')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'61','ARC_SERVER_URL','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'62','HQ_ALERTS_ENABLED','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'63','HQ_ALERT_NOTIFICATIONS_ENABLED','true')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('0',0,'64','HQ_ALERT_THRESHOLD','0')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('',0,'65','HQ_ALERT_THRESHOLD_EMAILS','')
INSERT INTO EAM_CONFIG_PROPS (DEFAULT_PROPVALUE,FREAD_ONLY,ID,PROPKEY,PROPVALUE) VALUES ('true',0,'66','HQ_HIERARCHICAL_ALERTING_ENABLED','true')
INSERT INTO QRTZ_LOCKS (LOCK_NAME) VALUES ('TRIGGER_ACCESS')
INSERT INTO QRTZ_LOCKS (LOCK_NAME) VALUES ('JOB_ACCESS')
INSERT INTO QRTZ_LOCKS (LOCK_NAME) VALUES ('CALENDAR_ACCESS')
INSERT INTO QRTZ_LOCKS (LOCK_NAME) VALUES ('STATE_ACCESS')
INSERT INTO QRTZ_LOCKS (LOCK_NAME) VALUES ('MISFIRE_ACCESS')