// This is the main DLL file.

#include "stdafx.h"

#include "mssql_pdh.h"

void plugin_throw_exception(JNIEnv *env, char *msg)
{
    jclass exceptionClass = env->FindClass("org/hyperic/hq/product/PluginException");
    env->ThrowNew(exceptionClass, msg);
}

JNIEXPORT jlong JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhOpenQuery(JNIEnv *env, jclass jc) {
    HQUERY     h_query;
    PDH_STATUS status;

    status = PdhOpenQuery(NULL, 0, &h_query);
    if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
        return 0;
    }
    return (jlong)h_query;
}

JNIEXPORT jlong JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhAddCounter(JNIEnv *env, jclass, jlong query, jstring path) {
    HCOUNTER   h_counter;
    HQUERY     h_query = (HQUERY)query;
    PDH_STATUS status;
    LPCTSTR    counter_path = (LPCTSTR)env->GetStringChars(path, NULL);

    status = PdhAddCounter(h_query, counter_path, 0, &h_counter);

    env->ReleaseStringChars(path, (const jchar *)counter_path);

	if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
        return 0;
    }

    return (jlong)h_counter;
}

JNIEXPORT void JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhRemoveCounter(JNIEnv *env, jclass, jlong counter) {
    HCOUNTER   h_counter = (HCOUNTER)counter;
    PDH_STATUS status;
    
    status = PdhRemoveCounter(h_counter);

    if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
    }
}


JNIEXPORT void JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_PdhCollectQueryData(JNIEnv * env, jclass, jlong query) {
   HQUERY h_query = (HQUERY)query;

    PDH_STATUS status = PdhCollectQueryData(h_query);
   
    if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
    }
}

JNIEXPORT jdouble JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_PdhGetFormattedCounterValue(JNIEnv *env, jclass, jlong counter) {
    HCOUNTER  h_counter = (HCOUNTER)counter;
    PDH_FMT_COUNTERVALUE fmt_value;

	PDH_STATUS status = PdhGetFormattedCounterValue(h_counter, PDH_FMT_DOUBLE, (LPDWORD)NULL, &fmt_value);

    if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
        return 0;
    }

    return fmt_value.doubleValue;
}

JNIEXPORT jobjectArray JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhGetInstances(JNIEnv *env, jclass, jstring cp) {
    PDH_STATUS   status              = ERROR_SUCCESS;
    DWORD        counter_list_size   = 0;
    DWORD        instance_list_size  = 8096;
    LPTSTR       instance_list_buf   = (LPTSTR)malloc ((instance_list_size * sizeof (TCHAR)));
    LPTSTR       cur_object          = NULL;
    LPCTSTR      counter_path        = (LPCTSTR)env->GetStringChars(cp, 0);
    jobjectArray array = NULL;

    status = PdhEnumObjectItems(NULL, NULL, counter_path, NULL,
                                &counter_list_size, instance_list_buf,
                                &instance_list_size, PERF_DETAIL_WIZARD,
                                FALSE);
    
    if (status == PDH_MORE_DATA && instance_list_size > 0) {
        // Allocate the buffers and try the call again.
        if (instance_list_buf != NULL) 
            free(instance_list_buf);
        
        instance_list_buf = (LPTSTR)malloc((instance_list_size * 
                                            sizeof (TCHAR)));
        counter_list_size = 0;
        status  = PdhEnumObjectItems (NULL, NULL, counter_path,
                                      NULL, &counter_list_size,
                                      instance_list_buf,
                                      &instance_list_size,
                                      PERF_DETAIL_WIZARD, FALSE);
    }

    env->ReleaseStringChars(cp, (const jchar *)counter_path);

    // Still may get PDH_ERROR_MORE data after the first reallocation,
    // but that is OK for just browsing the instances
    if (status == ERROR_SUCCESS || status == PDH_MORE_DATA) {
        int i, count;
        
        for (cur_object = instance_list_buf, count = 0;
             *cur_object != 0;
             cur_object += lstrlen(cur_object) + 1, count++);
            
        array = env->NewObjectArray(count, env->FindClass("java/lang/String"), env->NewStringUTF(""));
        if (env->ExceptionCheck()) {
            free(instance_list_buf);
            return NULL;
        }

        /* Walk the return instance list, creating an array */
        for (cur_object = instance_list_buf, i = 0;
             *cur_object != 0;
             i++) 
        {
            int len = lstrlen(cur_object);
            jstring s = env->NewString((const jchar *)cur_object, len);
            env->SetObjectArrayElement(array, i, s);
            if (env->ExceptionCheck()) {
                free(instance_list_buf);
                return NULL;
            }
            cur_object += len + 1;
        }
    } else {
        if (instance_list_buf != NULL) 
            free(instance_list_buf);
        
        // An error occured
        plugin_throw_exception(env, get_error_message(status));
        return NULL;
    }

    if (instance_list_buf != NULL) 
        free(instance_list_buf);

    return array;
}

JNIEXPORT jdouble JNICALL Java_org_hyperic_hq_plugin_mssql_PDH_pdhGetRawCounterValue(JNIEnv *env, jclass, jlong counter) {
    HCOUNTER  h_counter = (HCOUNTER)counter;
    PDH_RAW_COUNTER raw_value;
	DWORD type;

	PDH_STATUS status = PdhGetRawCounterValue(h_counter, &type, &raw_value);

    if (status != ERROR_SUCCESS) {
        plugin_throw_exception(env, get_error_message(status));
        return 0;
    }

	return (jdouble)raw_value.FirstValue;
}

static char *get_error_message(PDH_STATUS status) {
    switch (status) {
    case PDH_CSTATUS_NO_MACHINE:
        return "The computer is unavailable";
    case PDH_CSTATUS_NO_OBJECT:
        return "The specified object could not be found on the computer";
    case PDH_INVALID_ARGUMENT:
        return "A required argument is invalid";
    case PDH_MEMORY_ALLOCATION_FAILURE:
        return "A required temporary buffer could not be allocated";
    case PDH_INVALID_HANDLE:
        return "The query handle is not valid";
    case PDH_NO_DATA:
        return "The query does not currently have any counters";
    case PDH_CSTATUS_BAD_COUNTERNAME:
        return "The counter name path string could not be parsed or "
            "interpreted";
    case PDH_CSTATUS_NO_COUNTER:
        return "The specified counter was not found";
    case PDH_CSTATUS_NO_COUNTERNAME:
        return "An empty counter name path string was passed in";
    case PDH_FUNCTION_NOT_FOUND:
        return "The calculation function for this counter could not "
            "be determined";
    case PDH_ACCESS_DENIED:
        return "Access denied";
	case PDH_INVALID_DATA:
		return "The specified counter does not contain valid data or a successful status code.";
    default:
        return "Unknown error";
    }
}