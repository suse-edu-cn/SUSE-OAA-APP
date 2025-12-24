#pragma once
#include <string>
#include <vector>

#include <chrono>
#include <cstdint>
#include <sstream> 
#include "json.hpp"


struct CourseInfo {
	std::string kcmc;//课程名称
	std::string jgmc;//老师名称
	std::string jxb_id;//教学班id
	std::string jgh_id;//教师号id

	std::string kch_id;//课程号id
	std::string xsdm;//学时代码

	std::string tjzt;//提交状态

};

class Evaluator {
public:
	//传入Cookie和API地址，返回评测结果
	// a bunch of shit
	//std::string execute(const std::string& cookie, const std::string& apiUrl);

	//一键教学评估
	std::string runOneClickEvaluation(const std::string& cooke);

private:
	//第一步：获取课程列表
	std::vector<CourseInfo>fetchCourseList(const std::string& cookie);

	//第二步：提交单个评教
	bool submitSingleEvaluation(const std::string& cookie, const CourseInfo& course,double score);

	bool submitFullFlow(const std::string& cookie, const CourseInfo& course);
};



class Tools {
public:
	static std::string get_unix_ms_timestamp_str() {
		// 1. 获取13位毫秒时间戳（数值类型）
		auto now = std::chrono::system_clock::now();
		auto ms_ts = std::chrono::time_point_cast<std::chrono::milliseconds>(now)
			.time_since_epoch()
			.count();

		// 2. 转换为字符串（关键：适配Form Data的字符串格式）
		std::ostringstream oss;
		oss << ms_ts;  // 数值转字符串（无精度丢失，无多余字符）
		return oss.str();
	}
};