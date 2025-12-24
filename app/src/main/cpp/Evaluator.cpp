//httplib.h之前，需要定义这个宏以启用OpenSSL支持
#define CPPHTTPLIB_OPENSSL_SUPPORT

#include "Evaluator.h"
#include "httplib.h"


const char* HOST = "https://jwgl.suse.edu.cn";
const char* UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";

//构造参数的时候 强行拉取50条  qwq  //TODO：优化，先获取总数再设置
#define PARAM_SHOW_COUNT "50"

//评教分数
#define EVALUATION_SCORE 95.00

using  json = nlohmann::json;

//a bunch of shit
//std::string Evaluator::execute(const std::string& cookie, const std::string& apiUrl)
//{
//
//	//先get
//
//	httplib::Client cli("https://jwgl.suse.edu.cn");
//
//	//构造Header
//	httplib::Headers headers = {
//		{"Cookie", cookie},
//		{"User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"}
//	};
//
//	//构造Post参数
//	//context_type  application/x-www-form-urlencoded;charset=UTF-8
//	httplib::Params params;
//	params.emplace("_search","false");
//	params.emplace("nd",Tools::get_unix_ms_timestamp_str());
//	params.emplace("queryModel.showCount", "15");
//	params.emplace("queryModel.currentPage", "1");
//	params.emplace("queryModel.sortName", "kcmc,jzgmc");
//	params.emplace("queryModel.sortOrder", "asc");
//	params.emplace("time", "0");
//	//context_type JSON
//	/*
//	json requset_body;
//	requset_body["_search"] = "false";
//	requset_body["nd"] = Tools::get_unix_ms_timestamp_str();
//	requset_body["queryModel.showCount"] = "15";
//	requset_body["queryModel.currentPage"] = "1";
//	requset_body["queryModel.sortName"] = "kcmc,jzgmc";
//	requset_body["queryModel.sortOrder"] = "asc";
//	requset_body["time"] = "0";
//	*/
//	std::cout << Tools::get_unix_ms_timestamp_str() << char(10);
//
//
//	//发送POST请求
//	std::string path = "https://jwgl.suse.edu.cn/xspjgl/xspj_cxXspjIndex.html?doType=query&gnmkdm=N401605";
//	auto res = cli.Post(path, headers, params);
//	
//	if (res && res->status == 200) {
//		return "Success:"+res->body; //返回响应体作为评测结果
//	}
//	else {
//		return "Error:"+(res?std::to_string(res->status):"oi,Connection Failed");
//	}
//
//}

std::string Evaluator::runOneClickEvaluation(const std::string& cooke)
{
	std::string log = "oi,老登，正在开始一键评教...元神，启动！\n我将通过四步完成\n";
	//log += "传入的Cookie前20字符: " + (cooke.length() > 20 ? cooke.substr(0, 20) : cooke) + "...\n";

    log +="传入Cookie："+cooke+"\n";
    std::string tmp="route=182145f41e04a51a4d58f44daa39f024; JSESSIONID=1D4F3B72D0C8A0CCDBB614F2FBC8269A";
	log +="示例Cookie："+tmp+"\n";
    //1.获取所有课程
	auto courses = fetchCourseList(cooke);
	if (courses.empty()) {
		log += "获取课程表失败or没有课表orcooke过期\n";
		log += "请检查Cookie是否包含  route和JSESSIONID\n";
		return log;
	}

	log += "共计获取到 " + std::to_string(courses.size()) + " 门课程，开始评教...\n";
    std::cout<<log;
	int successCount = 0;
	//int skipCount = 0;
	//int failCount = 0;

	//2.遍历并提交


	for (const auto& course : courses)
	{
        std::cout<<"进入循环";
		

		//tjzt=="1"  ->已经提交了
		if (course.tjzt == "1"||course.tjzt=="提交") {
			//skipCount++;
			log += "[跳过] 课程：" + course.kcmc + "，教师：" + course.jgmc + "，状态：已评教\n";
			continue;
		}

		log += "[评教中] 课程：" + course.kcmc + "，教师：" + course.jgmc + " ... ";


		//4连提交
		if (submitFullFlow(cooke, course))
		{
			log += "四连提交成功！\n";
			successCount++;
			return log;
		}
		else 
		{
			log += "失败！\n";
		}
		

/*
		//执行提交，分数95.0
		bool success = submitSingleEvaluation(cooke, course, EVALUATION_SCORE);

		if (success)
		{
			successCount++;
			log += "成功！\n";
		}
		else {
			failCount++;
			log += "失败！\n";
		}
*/
		//休眠
		//std::this_thread::sleep_for(std::chrono::milliseconds(500));

	}

	log += "\n===oi成功了老登===\n";
	log += "成功提交：" + std::to_string(successCount) ;
	return log;

}

static httplib::Headers buildCommonHeaders(const std::string& cookie, bool form = true) {
    httplib::Headers headers = {
        {"Cookie", cookie},
        {"User-Agent", UA},
        {"Referer", "https://jwgl.suse.edu.cn/xspjgl/xspj_cxXspjIndex.html?gnmkdm=N401605"},
        {"Origin", "https://jwgl.suse.edu.cn"},
        {"X-Requested-With", "XMLHttpRequest"}
    };
    if (form) {
        headers.insert({"Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"});
        headers.insert({"Accept", "application/json, text/javascript, */*; q=0.01"});
    }
    return headers;
}

std::vector<CourseInfo> Evaluator::fetchCourseList(const std::string& cookie)
{
    std::vector<CourseInfo> list;
    httplib::Client cli(HOST);
    cli.set_follow_location(true);
    cli.set_connection_timeout(0, 3000000);
    cli.set_read_timeout(10, 0);

    auto headers = buildCommonHeaders(cookie);

    //2.构造参数
    httplib::Params params;
	params.emplace("_search", "false");
	params.emplace("nd", Tools::get_unix_ms_timestamp_str()); // 时间戳
	params.emplace("queryModel.showCount", PARAM_SHOW_COUNT); 
	params.emplace("queryModel.currentPage", "1");
	params.emplace("queryModel.sortName", "kcmc,jzgmc");
	params.emplace("queryModel.sortOrder", "asc");
	params.emplace("time", "0");

    // 3. 发送请求
	auto res = cli.Post("/xspjgl/xspj_cxXspjIndex.html?doType=query&gnmkdm=N401605", headers, params);

    if (res && res->status == 200) {
		try {
			auto json_data = json::parse(res->body);
			// 确保 items 存在且是数组
			if (json_data.contains("items") && json_data["items"].is_array()) {
				for (const auto& item : json_data["items"]) {
					CourseInfo info;
					// 使用 .value() 防止字段不存在导致崩溃
					info.kcmc = item.value("kcmc", "");//课程名称
					info.jgmc = item.value("jzgmc", ""); // 注意json里是 jzgmc (教职工名称)
					info.jxb_id = item.value("jxb_id", "");//教学班id
					info.jgh_id = item.value("jgh_id", "");//教师号id
					info.tjzt = item.value("tjzt", "-1"); // 获取状态
					info.kch_id = item.value("kch_id", "");//课程号id
					info.xsdm = item.value("xsdm", "01");//学时代码

					list.push_back(info);
				}
			} else {
				std::cout << "JSON解析成功但没有items或items不是数组: " << res->body.substr(0, 200) << std::endl;
			}
		}
		catch (std::exception& e) {
			std::cout << "JSON解析失败: " << e.what() << " Body: " << res->body.substr(0, 100) << std::endl;
		}
	} else {
        std::string status = res ? std::to_string(res->status) : "<null>";
        std::string body = res ? res->body.substr(0, 200) : "<no body>";
        std::cout << "fetchCourseList failed, status=" << status << ", body head=" << body << std::endl;
    }
    return list;
}

//为了构造第三个POST那个一坨FormData
httplib::Params buildBigPayload(const CourseInfo& c) 
{
	httplib::Params p;
	
	
	

	
	p.emplace("ztpjbl", "100");
	p.emplace("jszdpjbl", "0");
	p.emplace("xykzpjbl", "0");
	//p.emplace("pjzt", "1"); // 评教状态：1
	//p.emplace("tjzt", "1"); // 提交状态：1
	p.emplace("jxb_id", c.jxb_id);
	p.emplace("kch_id", c.kch_id);
	p.emplace("jgh_id", c.jgh_id);
	p.emplace("xsdm", c.xsdm);

	p.emplace("modelList[0].pjmbmcb_id", "3EE1315A25E10102E065000000000000001");
	p.emplace("modelList[0].pjdxdm", "01");
	p.emplace("modelList[0].fxzgf", "");
	p.emplace("modelList[0].py", "");
	p.emplace("modelList[0].xspfb_id", "");

	// 第一题 (满分)
	p.emplace("modelList[0].xspjList[0].childXspjList[0].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[0].pjzbxm_id", "3EE1315A25E40102E065000000000001");
	p.emplace("modelList[0].xspjList[0].childXspjList[0].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[0].zsmbmcb_id", "3EE1315A25E10102E065000000000001");

	// 第二题 (满分)
	p.emplace("modelList[0].xspjList[0].childXspjList[1].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[1].pjzbxm_id", "3EE1315A25E50102E065000000000001");
	p.emplace("modelList[0].xspjList[0].childXspjList[1].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[1].zsmbmcb_id", "3EE1315A25E10102E065000000000001");

	p.emplace("modelList[0].xspjList[0].childXspjList[2].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[2].pjzbxm_id", "3EE1315A25E60102E065000000000001");
	p.emplace("modelList[0].xspjList[0].childXspjList[2].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[2].zsmbmcb_id", "3EE1315A25E10102E065000000000001");

	p.emplace("modelList[0].xspjList[0].childXspjList[3].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[3].pjzbxm_id", "3EE1315A25E70102E065000000000001");
	p.emplace("modelList[0].xspjList[0].childXspjList[3].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[0].childXspjList[3].zsmbmcb_id", "3EE1315A25E10102E065000000000001");

	p.emplace("modelList[0].xspjList[0].pjzbxm_id", "3EE1315A25E20102E065000000000001");

	p.emplace("modelList[0].xspjList[1].childXspjList[0].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[0].pjzbxm_id", "3EE1315A25E80102E065000000000001");
	p.emplace("modelList[0].xspjList[1].childXspjList[0].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[0].zsmbmcb_id", "3EE1315A25E10102E065000000000001");
									 
	p.emplace("modelList[0].xspjList[1].childXspjList[1].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[1].pjzbxm_id", "3EE1315A25E90102E065000000000001");
	p.emplace("modelList[0].xspjList[1].childXspjList[1].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[1].zsmbmcb_id", "3EE1315A25E10102E065000000000001");
									 
	p.emplace("modelList[0].xspjList[1].childXspjList[2].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[2].pjzbxm_id", "3EE1315A25EA0102E065000000000001");
	p.emplace("modelList[0].xspjList[1].childXspjList[2].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[2].zsmbmcb_id", "3EE1315A25E10102E065000000000001");
									 
	p.emplace("modelList[0].xspjList[1].childXspjList[3].pfdjdmxmb_id", "7D419693F2DB750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[3].pjzbxm_id", "3EE1315A25EB0102E065000000000001");
	p.emplace("modelList[0].xspjList[1].childXspjList[3].pfdjdmb_id", "7D419693F2D9750EE0538AB0DDDB714C");
	p.emplace("modelList[0].xspjList[1].childXspjList[3].zsmbmcb_id", "3EE1315A25E10102E065000000000001");

	p.emplace("modelList[0].xspjList[1].pjzbxm_id", "3EE1315A25E30102E065000000000001");
	
	p.emplace("modelList[0].pjzt", "1");
	p.emplace("tjzt", "1");

	return p;
}

bool Evaluator::submitSingleEvaluation(const std::string& cookie, const CourseInfo& course, double score)
{

    httplib::Client cli(HOST);
    cli.set_follow_location(true);

    auto headers = buildCommonHeaders(cookie);

    httplib::Params params;
	// 构造 Form Data (bfzpf, jxb_id, jgh_id)
	char score_str[16];
	sprintf(score_str, "%.2f", score); // 保留两位小数 "95.00"

	params.emplace("bfzpf", score_str);
	params.emplace("jxb_id", course.jxb_id);
	params.emplace("jgh_id", course.jgh_id);

	// 发送请求
	
	auto res = cli.Post("/xspjgl/xspj_cxSftf.html?gnmkdm=N401605", headers, params);

    if (res && res->status == 200) {
		// 这里需要判断 body 内容。
		// 通常如果成功，body可能是 "success" 或者 json {"flag":"1"} 之类的
		// 你可以先简单判断状态码，或者打印 body 看看
		std::cout << "Submit Reponse: " << res->body << std::endl;
		return true;
	}
	return false;
}

bool Evaluator::submitFullFlow(const std::string& cookie, const CourseInfo& course)
{
    httplib::Client cli(HOST);
    cli.set_follow_location(true);

    auto headers = buildCommonHeaders(cookie);

    // --- 第 1 步：cxSftf (检查) ---
    httplib::Params p1;
	p1.emplace("bfzpf", "95.00");
	p1.emplace("jxb_id", course.jxb_id);
	p1.emplace("jgh_id", course.jgh_id);
	cli.Post("/xspjgl/xspj_cxSftf.html?gnmkdm=N401605", headers, p1);

	// --- 第 2 步：cxInsjjg (初始化) ---
	// 这个请求是 POST 但是 body 为空 (Content-Length: 0)
	cli.Post("/xspjgl/xspj_cxInsjjg.html?gnmkdm=N401605", headers, httplib::Params());

	// --- 第 3 步：cxInmffz (录入分值) ---
	httplib::Params p3;
	p3.emplace("xspfb_id", "");
	p3.emplace("jxb_id", course.jxb_id);
	p3.emplace("bfzpf", "95.00");
	cli.Post("/xspjgl/xspj_cxInmffz.html?gnmkdm=N401605", headers, p3);

	// --- 第 4 步：tjXspj (提交保存 - BOSS战) ---
	// 使用上面定义的辅助函数构造巨大参数
	httplib::Params p4 = buildBigPayload(course);

	// 你抓包的那个最长的 payload 对应的 key 其实是 buildBigPayload 里那些
	// 注意：你需要在 buildBigPayload 里把所有抓到的 modelList 参数都补全！

	auto res = cli.Post("/xspjgl/xspj_tjXspj.html?gnmkdm=N401605", headers, p4);

    if (res && res->status == 200) {
		// 通常成功会返回 "success" 或者 JSON，或者 body 为空但 status 200
		// 如果这里打印出来包含 "保存成功" 或者 json flag=1 就稳了
		std::cout << "Submit Result: " << res->body << std::endl;
		return true;
	} else {
        std::string status = res ? std::to_string(res->status) : "<null>";
        std::string body = res ? res->body.substr(0, 200) : "<no body>";
        std::cout << "Submit Result failed, status=" << status << ", body head=" << body << std::endl;
    }

	return false;
}
