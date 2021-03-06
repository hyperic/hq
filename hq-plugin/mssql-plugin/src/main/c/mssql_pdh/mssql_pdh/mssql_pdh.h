// mssql_pdh.h
#include <pdh.h>
#include <pdhmsg.h>

static char *get_error_message(PDH_STATUS status);

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_hyperic_hq_plugin_mssql_PDH */

#ifndef _Included_org_hyperic_hq_plugin_mssql_PDH
#define _Included_org_hyperic_hq_plugin_mssql_PDH
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhOpenQuery
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhOpenQuery
  (JNIEnv *, jclass);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhCloseQuery
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhCloseQuery
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhAddCounter
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhAddCounter
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhRemoveCounter
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhRemoveCounter
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    PdhCollectQueryData
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_PdhCollectQueryData
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    PdhGetFormattedCounterValue
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_PdhGetFormattedCounterValue
  (JNIEnv *, jclass, jlong);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhGetInstances
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhGetInstances
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_hyperic_hq_plugin_mssql_PDH
 * Method:    pdhGetRawCounterValue
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhGetRawCounterValue
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class org_hyperic_hq_plugin_mssql_PDH_InstanceIndex */

#ifndef _Included_org_hyperic_hq_plugin_mssql_PDH_InstanceIndex
#define _Included_org_hyperic_hq_plugin_mssql_PDH_InstanceIndex
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
	