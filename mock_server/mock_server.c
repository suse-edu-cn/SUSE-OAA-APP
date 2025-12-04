#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <time.h>
#include <stdarg.h>

#ifdef _WIN32
// Windows Headers
#include <winsock2.h>
#include <ws2tcpip.h>
#include <iphlpapi.h>
#include <fcntl.h>
#include <io.h>
#include <windows.h>
#define _O_U8TEXT 0x0004

#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib, "iphlpapi.lib")
#define CLOSE_SOCKET closesocket
#define SLEEP_MS(x) Sleep(x)
#define IS_VALID_SOCKET(s) ((s) != INVALID_SOCKET)
typedef SOCKET socket_t;
#else
// Linux/Mac Headers
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <ifaddrs.h>
#define CLOSE_SOCKET close
#define SLEEP_MS(x) usleep((x)*1000)
#define IS_VALID_SOCKET(s) ((s) >= 0)
typedef int socket_t;
#endif

// ==========================================
// ============ Logger Utils ================
// ==========================================

#define COL_RESET   "\033[0m"
#define COL_GREEN   "\033[32m"
#define COL_YELLOW  "\033[33m"
#define COL_CYAN    "\033[36m"
#define COL_RED     "\033[31m"
#define COL_MAGENTA "\033[35m"

void log_msg(const char* color, const char* tag, const char* fmt, ...) {
	time_t now = time(NULL);
	struct tm *t = localtime(&now);
	char time_str[64];
	strftime(time_str, sizeof(time_str), "%H:%M:%S", t);
	
	printf("%s[%s] [%s] ", color, time_str, tag);
	
	va_list args;
	va_start(args, fmt);
	vprintf(fmt, args);
	va_end(args);
	
	printf("%s\n", COL_RESET);
}

// ==========================================
// ============ Base64 Utils ================
// ==========================================

static char encoding_table[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
	'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
	'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
	'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
	'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
	'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
	'w', 'x', 'y', 'z', '0', '1', '2', '3',
	'4', '5', '6', '7', '8', '9', '+', '/'};
static int mod_table[] = {0, 2, 1};

char *base64_encode(const unsigned char *data, size_t input_length) {
	size_t output_length = 4 * ((input_length + 2) / 3);
	char *encoded_data = (char *)malloc(output_length + 1);
	if (encoded_data == NULL) return NULL;
	
	for (int i = 0, j = 0; i < input_length;) {
		uint32_t octet_a = i < input_length ? (unsigned char)data[i++] : 0;
		uint32_t octet_b = i < input_length ? (unsigned char)data[i++] : 0;
		uint32_t octet_c = i < input_length ? (unsigned char)data[i++] : 0;
		
		uint32_t triple = (octet_a << 0x10) + (octet_b << 0x08) + octet_c;
		
		encoded_data[j++] = encoding_table[(triple >> 3 * 6) & 0x3F];
		encoded_data[j++] = encoding_table[(triple >> 2 * 6) & 0x3F];
		encoded_data[j++] = encoding_table[(triple >> 1 * 6) & 0x3F];
		encoded_data[j++] = encoding_table[(triple >> 0 * 6) & 0x3F];
	}
	
	for (int i = 0; i < mod_table[input_length % 3]; i++)
		encoded_data[output_length - 1 - i] = '=';
	
	encoded_data[output_length] = '\0';
	return encoded_data;
}

// ==========================================
// ============ Network Helper ==============
// ==========================================

// 显示本机所有 IPv4 地址
void show_local_ips() {
	printf("---------------------------------------------\n");
	log_msg(COL_CYAN, "NET", "Available IP Addresses for Connection:");
	
#ifdef _WIN32
	PIP_ADAPTER_INFO pAdapterInfo;
	PIP_ADAPTER_INFO pAdapter = NULL;
	DWORD dwRetVal = 0;
	ULONG ulOutBufLen = sizeof(IP_ADAPTER_INFO);
	
	pAdapterInfo = (IP_ADAPTER_INFO *)malloc(sizeof(IP_ADAPTER_INFO));
	if (GetAdaptersInfo(pAdapterInfo, &ulOutBufLen) == ERROR_BUFFER_OVERFLOW) {
		free(pAdapterInfo);
		pAdapterInfo = (IP_ADAPTER_INFO *)malloc(ulOutBufLen);
	}
	
	if ((dwRetVal = GetAdaptersInfo(pAdapterInfo, &ulOutBufLen)) == NO_ERROR) {
		pAdapter = pAdapterInfo;
		while (pAdapter) {
			if (strcmp(pAdapter->IpAddressList.IpAddress.String, "0.0.0.0") != 0) {
				printf("    -> %s (%s)\n", pAdapter->IpAddressList.IpAddress.String, pAdapter->Description);
			}
			pAdapter = pAdapter->Next;
		}
	}
	if (pAdapterInfo) free(pAdapterInfo);
	
#else
	struct ifaddrs *ifAddrStruct = NULL;
	struct ifaddrs *ifa = NULL;
	void *tmpAddrPtr = NULL;
	
	getifaddrs(&ifAddrStruct);
	
	for (ifa = ifAddrStruct; ifa != NULL; ifa = ifa->ifa_next) {
		if (!ifa->ifa_addr) continue;
		if (ifa->ifa_addr->sa_family == AF_INET) { // IPv4
			tmpAddrPtr = &((struct sockaddr_in *)ifa->ifa_addr)->sin_addr;
			char addressBuffer[INET_ADDRSTRLEN];
			inet_ntop(AF_INET, tmpAddrPtr, addressBuffer, INET_ADDRSTRLEN);
			if (strcmp(addressBuffer, "127.0.0.1") != 0) {
				printf("    -> %s (%s)\n", addressBuffer, ifa->ifa_name);
			}
		}
	}
	if (ifAddrStruct != NULL) freeifaddrs(ifAddrStruct);
#endif
	printf("---------------------------------------------\n");
}

int send_all(socket_t s, const char *buf, int len) {
	int total = 0;        
	int bytesleft = len; 
	int n;
	while(total < len) {
		n = send(s, buf + total, bytesleft, 0);
		if (n == -1) { break; }
		total += n;
		bytesleft -= n;
	}
	return n == -1 ? -1 : 0; 
}

// ==========================================
// ============ Config & Constants ==========
// ==========================================
#define PORT 8080
#define MAX_BUFFER_SIZE (1024 * 1024 * 2) // 2MB Buffer

// --- Static JSON Responses ---

const char *JSON_SUCCESS_DEFAULT = "{\"code\":200,\"message\":\"操作成功\",\"data\":null}";
const char *JSON_EMPTY_SUCCESS = "{\"code\":200,\"message\":\"请求成功\",\"data\":null}"; 

// 用户模块
const char *JSON_LOGIN_SUCCESS = "{\"code\":200,\"message\":\"登录成功\",\"data\":{\"token\":\"mock_jwt_token_v2_xyz\"}}";
const char *JSON_REGISTER_SUCCESS = "{\"code\":200,\"data\":null,\"message\":\"注册成功\"}";
const char *JSON_USER_INFO = "{\"code\":\"200\",\"message\":\"success\",\"data\":{\"student_id\":24171040204,\"name\":\"浪凡\",\"username\":\"langfan\",\"role\":\"会员\",\"department\":\"软件工程系\"}}";

// 申请表模块
const char *JSON_APP_LIST = 
"{\"code\":200,\"message\":\"查询成功\",\"data\":["
"{\"name\":\"樊明智\",\"reason\":\"想提升代码能力\",\"choice1\":\"算法竞赛部\",\"choice2\":\"项目实践部\",\"experience\":\"蓝桥杯省一\",\"phone\":\"18780732003\",\"gender\":\"男\",\"major\":\"软件工程\",\"class\":\"245班\",\"birthday\":\"2006-02-03\",\"qq\":\"2824920336\",\"politic_stance\":\"党员\",\"adjustment\":0,\"studentid\":\"24171040202\"},"
"{\"name\":\"李华\",\"reason\":\"试试就逝世\",\"choice1\":\"组织宣传部\",\"choice2\":\"秘书处\",\"experience\":\"无\",\"phone\":\"13800138000\",\"gender\":\"男\",\"major\":\"计算机科学\",\"class\":\"1班\",\"birthday\":\"2005-01-01\",\"qq\":\"123456789\",\"politic_stance\":\"团员\",\"adjustment\":1,\"studentid\":\"24171040203\"}"
"]}";

// ==========================================
// ============ String Helper ===============
// ==========================================

int safe_append(char *buffer, int offset, int max_size, const char *format, ...) {
	if (offset >= max_size - 1) return offset;
	va_list args;
	va_start(args, format);
	int written = vsnprintf(buffer + offset, max_size - offset, format, args);
	va_end(args);
	if (written < 0 || written >= max_size - offset) {
		return max_size - 1; 
	}
	return offset + written;
}

// ==========================================
// ============ Generators Utils ============
// ==========================================

int rand_range(int min, int max) {
	if (max < min) return min;
	return min + rand() % (max - min + 1);
}

void format_date(char* buf, int offset_days) {
	time_t now = time(NULL);
	now += offset_days * 86400; 
	struct tm *t = localtime(&now);
	sprintf(buf, "%04d-%02d-%02d", t->tm_year + 1900, t->tm_mon + 1, t->tm_mday);
}

// ==========================================
// ============ Match Logic =================
// ==========================================

enum MatchStatus {
	STATUS_REGISTERING = 0,
	STATUS_UPCOMING = 1,
	STATUS_REG_ENDED = 2,
	STATUS_ONGOING = 3,
	STATUS_ENDED = 4
};

void get_dates_for_status(int status, char* reg_s, char* reg_e, char* match_s, char* match_e) {
	switch (status) {
		case STATUS_REGISTERING: 
		format_date(reg_s, -5); format_date(reg_e, 5);
		format_date(match_s, 10); format_date(match_e, 15);
		break;
	case STATUS_UPCOMING:
		format_date(reg_s, 5); format_date(reg_e, 10);
		format_date(match_s, 20); format_date(match_e, 25);
		break;
	case STATUS_REG_ENDED:
		format_date(reg_s, -10); format_date(reg_e, -2);
		format_date(match_s, 5); format_date(match_e, 10);
		break;
	case STATUS_ONGOING:
		format_date(reg_s, -20); format_date(reg_e, -10);
		format_date(match_s, -2); format_date(match_e, 5);
		break;
	case STATUS_ENDED:
		format_date(reg_s, -30); format_date(reg_e, -20);
		format_date(match_s, -15); format_date(match_e, -5);
		break;
	default:
		format_date(reg_s, 1); format_date(reg_e, 2);
		format_date(match_s, 3); format_date(match_e, 4);
	}
}

// ==========================================
// ============ Vocabulary ==================
// ==========================================

const char* T_PREFIX[] = { "第1届", "2025年度", "2024秋季", "全球", "全国", "校际", "院级", "新生", "沉浸式", "高强度", "极限", "创意", "微软杯", "谷歌杯", "华为杯" };
const char* T_ADJ[] = { "人工智能", "区块链", "云原生", "网络安全", "全栈开发", "嵌入式", "物联网", "大数据", "元宇宙", "量子计算", "低代码", "高性能", "分布式", "自动化" };
const char* T_NOUN[] = { "程序设计", "算法", "黑客攻防", "CTF夺旗", "UI设计", "建模", "机器人", "大模型微调", "移动应用", "游戏开发", "数据挖掘" };
const char* T_SUFFIX[] = { "大赛", "挑战杯", "黑客松", "锦标赛", "嘉年华", "邀请赛", "集训营", "公开赛", "马拉松", "对抗赛", "联赛" };
const char* LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

// ==========================================
// ============ Response Logic ==============
// ==========================================

void generate_match_list_response(char* response_buffer) {
	int offset = 0;
	offset = safe_append(response_buffer, offset, MAX_BUFFER_SIZE, "{\"code\":200,\"message\":\"获取比赛列表成功\",\"data\":[");
	
	int count = rand_range(10, 20); 
	log_msg(COL_MAGENTA, "GEN", "Generating %d matches...", count);
	
	char rs[16], re[16], ms[16], me[16];
	
	for (int i = 0; i < count; i++) {
		int status = rand_range(0, 4); 
		get_dates_for_status(status, rs, re, ms, me);
		
		char title[256];
		sprintf(title, "%s%s%s%s", T_PREFIX[rand_range(0, 14)], T_ADJ[rand_range(0, 13)], T_NOUN[rand_range(0, 10)], T_SUFFIX[rand_range(0, 10)]);
		
		offset = safe_append(response_buffer, offset, MAX_BUFFER_SIZE,
							 "{"
							 "\"id\":%d,"
							 "\"title\":\"%s\","
							 "\"description\":\"Status: %d | %s\","
							 "\"status\":%d,"
							 "\"reg_time\":[\"%s\",\"%s\"],"
							 "\"match_time\":[\"%s\",\"%s\"],"
							 "\"reg_start_at\":\"%s\"," 
							 "\"reg_end_at\":\"%s\","
							 "\"start_at\":\"%s\","
							 "\"end_at\":\"%s\""
							 "}",
							 i, title, status, LOREM_IPSUM, 
							 status, 
							 rs, re, ms, me, rs, re, ms, me
							 );
		
		if (i < count - 1) {
			offset = safe_append(response_buffer, offset, MAX_BUFFER_SIZE, ",");
		}
	}
	safe_append(response_buffer, offset, MAX_BUFFER_SIZE, "]}");
}

void generate_detail_response(char* response_buffer, int id) {
	// 1. 生成一段很长的 Markdown 文本
	// 注意：这里我们使用栈内存，如果文本非常大，建议使用 malloc
	char raw_content[8192]; 
	int pos = 0;
	
	pos += sprintf(raw_content + pos, "# 比赛详情 (ID: %d)\n\n", id);
	pos += sprintf(raw_content + pos, "## 1. 赛事背景\n> 本次比赛旨在选拔优秀的开发者，挑战极限。\n\n");
	pos += sprintf(raw_content + pos, "在当今数字化飞速发展的时代，技术革新日新月异。本次大赛聚合了行业顶尖资源，为广大学子提供一个展示自我的平台。\n\n");
	
	pos += sprintf(raw_content + pos, "## 2. 参赛规则\n- **组队方式**：每队 1-3 人\n- **语言限制**：C/C++, Java, Python, Go\n- **提交格式**：Git 仓库链接\n\n");
	
	pos += sprintf(raw_content + pos, "## 3. 详细流程\n1. **报名阶段**：线上填写表单\n2. **初赛**：算法在线笔试\n3. **复赛**：项目实战开发\n4. **决赛**：现场答辩与演示\n\n");
	
	pos += sprintf(raw_content + pos, "## 4. 示例代码\n```c\n#include <stdio.h>\nint main() {\n    printf(\"Hello World\");\n    return 0;\n}\n```\n\n");
	
	pos += sprintf(raw_content + pos, "## 5. 奖项设置\n| 奖项 | 数量 | 奖金 |\n|---|---|---|\n| 一等奖 | 1 | 10000 |\n| 二等奖 | 2 | 5000 |\n| 三等奖 | 3 | 2000 |\n\n");
	
	// 添加重复文本以增加长度
	for(int i=0; i<10; i++) {
		pos += sprintf(raw_content + pos, "这是一段重复的填充文本，用于测试长文本的显示效果。Lorem ipsum dolor sit amet. (%d)\n", i);
	}
	
	// 2. 将 Markdown 文本进行 Base64 编码
	char* b64_content = base64_encode((unsigned char*)raw_content, strlen(raw_content));
	if (!b64_content) {
		log_msg(COL_RED, "ERR", "Base64 encode failed");
		return;
	}
	
	// 3. 构造 JSON 响应，将 Base64 字符串插入
	int offset = 0;
	offset = safe_append(response_buffer, offset, MAX_BUFFER_SIZE, 
						 "{\"code\":200,\"message\":\"获取比赛详情成功\",\"data\":{"
						 "\"title\":\"模拟比赛详情 ID: %d\","
						 "\"author\":{\"id\":241000001,\"name\":\"管理员\"},"
						 "\"status\":4,"
						 "\"reg_time\":[\"2025-11-24\",\"2025-11-25\"],"
						 "\"match_time\":[\"2025-11-26\",\"2025-11-27\"],"
						 "\"content\":\"%s\""  // 这里插入 Base64 字符串
						 "}}", id, b64_content);
	
	free(b64_content); // 释放 Base64 内存
}

// ==========================================
// ============ Main Server =================
// ==========================================

int main() {
	srand((unsigned int)time(NULL));
#ifdef _WIN32
	WSADATA wsaData;
	WSAStartup(MAKEWORD(2, 2), &wsaData);
	if (_setmode(_fileno(stdout), _O_U8TEXT) == -1) {}
#endif
	
	show_local_ips();
	
	socket_t server_fd, new_socket;
	struct sockaddr_in address;
	int addrlen = sizeof(address);
	
	char *buffer = (char*)malloc(MAX_BUFFER_SIZE); 
	char *resp_body = (char*)malloc(MAX_BUFFER_SIZE); 
	char *header = (char*)malloc(4096); 
	
	if (!buffer || !resp_body || !header) {
		log_msg(COL_RED, "ERR", "Memory allocation failed!");
		return 1;
	}
	
	if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) return 1;
	
	int opt = 1;
	setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (char *)&opt, sizeof(opt));
	address.sin_family = AF_INET;
	address.sin_addr.s_addr = INADDR_ANY;
	address.sin_port = htons(PORT);
	
	if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
		log_msg(COL_RED, "ERR", "Bind failed! Port %d might be in use.", PORT);
		return 1;
	}
	listen(server_fd, 5); 
	
	log_msg(COL_GREEN, "SYS", "Ultimate Mock Server Running on Port %d", PORT);
	
	while (1) {
		new_socket = accept(server_fd, (struct sockaddr *)&address, 
							#ifdef _WIN32
							(int*)&addrlen
							#else
							(socklen_t*)&addrlen
							#endif
							);
		
		if (!IS_VALID_SOCKET(new_socket)) continue;
		
		char *client_ip = inet_ntoa(address.sin_addr);
		log_msg(COL_CYAN, "CONN", "Client connected: %s", client_ip);
		
		memset(buffer, 0, 4096); 
		memset(resp_body, 0, MAX_BUFFER_SIZE);
		memset(header, 0, 4096);
		
		int valread = recv(new_socket, buffer, 4096, 0);
		
		if (valread > 0) {
			char method[16] = {0}, path[256] = {0};
			sscanf(buffer, "%s %s", method, path);
			log_msg(COL_YELLOW, "REQ", "%s %s", method, path);
			
			// ============ 路由逻辑 ============
			
			if (strstr(path, "/match/getList")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Match List");
				generate_match_list_response(resp_body);
			} 
			else if (strstr(path, "/match/getDetail")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Match Detail (Base64 Content)");
				generate_detail_response(resp_body, 999);
			}
			else if (strstr(path, "/contest/create")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Contest Create");
				strcpy(resp_body, JSON_EMPTY_SUCCESS);
			}
			else if (strstr(path, "/user/Info")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: User Info");
				strcpy(resp_body, JSON_USER_INFO);
			}
			else if (strstr(path, "/user/login")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Login");
				strcpy(resp_body, JSON_LOGIN_SUCCESS);
			}
			else if (strstr(path, "/user/register")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Register");
				strcpy(resp_body, JSON_REGISTER_SUCCESS);
			}
			else if (strstr(path, "/user/updatePassword")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Update Password");
				strcpy(resp_body, JSON_EMPTY_SUCCESS); 
			}
			else if (strstr(path, "/application/get")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Application List");
				strcpy(resp_body, JSON_APP_LIST);
			}
			else if (strstr(path, "/application/create") || strstr(path, "/application/update")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: Application Create/Update");
				strcpy(resp_body, JSON_EMPTY_SUCCESS);
			}
			else if (strstr(path, "/user/update")) {
				log_msg(COL_MAGENTA, "ROUTE", "Matched: User Update");
				strcpy(resp_body, JSON_EMPTY_SUCCESS);
			}
			else {
				log_msg(COL_MAGENTA, "ROUTE", "Fallback: Default Success");
				strcpy(resp_body, JSON_SUCCESS_DEFAULT);
			}
			
			// ============ 发送响应 ============
			size_t body_len = strlen(resp_body);
			sprintf(header, 
					"HTTP/1.1 200 OK\r\n"
					"Content-Type: application/json; charset=utf-8\r\n"
					"Connection: close\r\n"  
					"Access-Control-Allow-Origin: *\r\n"
					"Content-Length: %zu\r\n" 
					"\r\n", body_len);
			
			send_all(new_socket, header, strlen(header));
			send_all(new_socket, resp_body, body_len);
			
			log_msg(COL_GREEN, "SEND", "Response sent (%zu bytes).", body_len);
		} else {
			log_msg(COL_RED, "WARN", "Empty request or receive error.");
		}
		
		SLEEP_MS(50); 
		
#ifdef _WIN32
		shutdown(new_socket, SD_BOTH);
#else
		shutdown(new_socket, SHUT_RDWR);
#endif
		CLOSE_SOCKET(new_socket);
	}
	
	free(buffer);
	free(resp_body);
	free(header);
#ifdef _WIN32
	WSACleanup();
#endif
	return 0;
}
