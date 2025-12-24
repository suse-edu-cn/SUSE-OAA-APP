#include <jni.h>
#include <string>
#include "Evaluator.h"



extern "C" JNIEXPORT jstring JNICALL
Java_com_suseoaa_projectoaa_common_util_NativeHelper_startEvaluation(
	JNIEnv* env,
	jobject,
	jstring jCookie) {// 现在只需要传 Cookie 即可

	const char* cCookie = env->GetStringUTFChars(jCookie, 0);

	Evaluator evaluator;
	
	std::string resultLog = evaluator.runOneClickEvaluation(std::string(cCookie));

	env->ReleaseStringUTFChars(jCookie, cCookie);
	return env->NewStringUTF(resultLog.c_str());
}
 
