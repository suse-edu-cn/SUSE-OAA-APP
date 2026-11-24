package com.suseoaa.projectoaa.shared.data.remote

import com.suseoaa.projectoaa.shared.data.remote.api.OaaAppApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaOAuthApi
import com.suseoaa.projectoaa.shared.data.remote.api.OaaRecruitmentApi
import com.suseoaa.projectoaa.shared.data.remote.network.OaaHttpClient
import com.suseoaa.projectoaa.shared.data.repository.AppManagementRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.OAuthRepositoryImpl
import com.suseoaa.projectoaa.shared.data.repository.RecruitmentRepositoryImpl
import com.suseoaa.projectoaa.shared.domain.error.AppError
import com.suseoaa.projectoaa.shared.domain.error.AppException
import com.suseoaa.projectoaa.shared.domain.model.oauth.OAuthAuthorizeRequest
import com.suseoaa.projectoaa.shared.domain.model.person.CurrentUser
import com.suseoaa.projectoaa.shared.domain.model.person.PersonData
import com.suseoaa.projectoaa.shared.domain.model.person.UserListItem
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldOption
import com.suseoaa.projectoaa.shared.domain.model.recruitment.ExportFieldsConfig
import com.suseoaa.projectoaa.shared.domain.model.recruitment.Interviewer
import com.suseoaa.projectoaa.shared.domain.permission.OaaPermission
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OaaNewBackendContractTest {

    @Test
    fun baseUrlPointsToNewProductionEndpoint() {
        assertEquals("https://suseoaa.com/v2", ApiConfig.OAA_BASE)
        assertEquals("https://suseoaa.com", ApiConfig.OAA_WEB_ORIGIN)
    }

    @Test
    fun personAndUserListParseInterviewerAndDeletingFlagsAndDrivePermissions() {
        val me = testJson.decodeFromString<PersonData>(
            """{
                "user_id": 33,
                "student_id": "23341010305",
                "username": "VincentHZR",
                "name": "Vincent",
                "department": "项目实践部",
                "role": "干事",
                "role_level": 20,
                "is_graduated": false,
                "is_interviewer": true,
                "is_deleting": false
            }"""
        )
        assertEquals(true, me.isInterviewer)
        assertFalse(me.isDeleting)

        val currentInterviewer = CurrentUser(me, level = 20, departmentId = 5, roleId = 6)
        assertTrue(OaaPermission.canReviewApplications(currentInterviewer))
        assertFalse(OaaPermission.canManageApps(currentInterviewer))

        val graduatedInterviewer = currentInterviewer.copy(person = me.copy(isGraduated = true))
        assertFalse(OaaPermission.canReviewApplications(graduatedInterviewer))

        val admin = currentInterviewer.copy(level = 80)
        assertTrue(OaaPermission.canManageApps(admin))

        val item = testJson.decodeFromString<UserListItem>(
            """{"user_id": 5, "is_interviewer": false, "is_deleting": true}"""
        )
        assertEquals(false, item.isInterviewer)
        assertTrue(item.isDeleting)
    }

    @Test
    fun interviewerListParsesDepartmentName() = runTest {
        val stack = OaaTestStack {
            json(
                """{
                    "code": 200,
                    "message": "success",
                    "data": [{
                        "id": 8,
                        "year": 2026,
                        "term_type": "招新",
                        "name": "姚宜君",
                        "role": "部长",
                        "department_name": "组织宣传部",
                        "remark": "组织宣传部部长"
                    }]
                }"""
            )
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)
        val list = repo.getInterviewers(7).getOrThrow()
        val interviewer = list.single()
        assertEquals(8, interviewer.resolvedId)
        assertEquals(2026, interviewer.year)
        assertEquals("招新", interviewer.termType)
        assertEquals("组织宣传部", interviewer.resolvedDepartment)
        assertEquals("姚宜君", interviewer.displayName)
    }

    @Test
    fun applicationListForwardsHasResultFalseAndNameFilter() = runTest {
        val stack = OaaTestStack {
            json("""{"code":200,"message":"success","data":[]}""")
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        repo.getApplications(termId = 7, departmentId = 5, hasResult = false, name = " 张三 ").getOrThrow()

        val params = stack.requestsTo("/application/list").single().url.parameters
        assertEquals("7", params["term_id"])
        assertEquals("5", params["department_id"])
        assertEquals("false", params["has_result"])
        assertEquals("张三", params["name"])
    }

    @Test
    fun exportFieldsBitmaskAndBinaryXlsxDownloadAndErrorHandling() = runTest {
        val config = ExportFieldsConfig(
            totalFields = 22,
            allBits = "1".repeat(22),
            fields = listOf(ExportFieldOption(0, "业务周期"), ExportFieldOption(21, "更新时间"))
        )
        assertEquals("1000000000000000000001", config.buildBits(setOf(0, 21)))

        val xlsxBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00)
        var returnForbidden = false
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/interviewer/result/fields" -> json(
                    """{"code":200,"message":"success","data":{"total_fields":22,"all_bits":"1111111111111111111111","fields":[{"index":0,"label":"业务周期"},{"index":1,"label":"业务类型"}]}}"""
                )
                "/v2/interviewer/result/export" -> if (returnForbidden) {
                    json("""{"code":403,"message":"当前账号无权限导出","data":null}""", HttpStatusCode.Forbidden)
                } else {
                    respond(
                        content = xlsxBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                            HttpHeaders.ContentDisposition to listOf(
                                """attachment; filename="fallback.xlsx"; filename*=UTF-8''2026%20%E5%B9%B2%E4%BA%8B%E6%8B%9B%E6%96%B0_%E9%9D%A2%E8%AF%95%E7%BB%93%E6%9E%9C.xlsx"""
                            )
                        )
                    )
                }
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = RecruitmentRepositoryImpl(OaaRecruitmentApi(stack.requester), testJson)

        val fetchedConfig = repo.getExportFields().getOrThrow()
        assertEquals(22, fetchedConfig.totalFields)
        assertEquals("业务周期", fetchedConfig.fields.first().label)

        val file = repo.exportInterviewResults(
            termId = 7,
            departmentId = 5,
            hasResult = true,
            fieldsBits = "1100000000000000000000"
        ).getOrThrow()
        assertEquals("2026 干事招新_面试结果.xlsx", file.fileName)
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.mimeType)
        assertContentEquals(xlsxBytes, file.bytes)

        val exportParams = stack.requestsTo("/interviewer/result/export").single().url.parameters
        assertEquals("7", exportParams["term_id"])
        assertEquals("5", exportParams["department_id"])
        assertEquals("true", exportParams["has_result"])
        assertEquals("1100000000000000000000", exportParams["fields"])

        returnForbidden = true
        val failure = repo.exportInterviewResults(termId = 7).exceptionOrNull()
        assertEquals("当前账号无权限导出", failure?.message)
    }

    @Test
    fun appManagementLifecycleHandlesAllEndpointsAndCreateVariants() = runTest {
        var createAsNull = false
        val stack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/app/list" -> json(
                    """{"code":200,"message":"success","data":[{
                        "client_id":"suse_oj",
                        "mask_secret":"oaa_sec_e844********************bc0d",
                        "name":"oj 平台",
                        "redirect_uris":"https://oj.suseoaa.com/oauth/suseoaa/callback",
                        "scope":"openid profile email",
                        "is_active":true
                    }]}"""
                )
                "/v2/app/detail" -> json(
                    """{"code":200,"message":"success","data":{
                        "client_id":"suse_oj",
                        "mask_secret":"oaa_sec_e844********************bc0d",
                        "name":"oj 平台",
                        "redirect_uris":"https://oj.suseoaa.com/oauth/suseoaa/callback",
                        "scope":"openid profile email",
                        "is_active":true
                    }}"""
                )
                "/v2/app/create" -> if (createAsNull) {
                    json("""{"code":200,"message":"success","data":null}""")
                } else {
                    json("""{"code":200,"message":"success","data":{"client_id":"new_app","client_secret":"oaa_sec_plain_secret"}}""")
                }
                "/v2/app/update", "/v2/app/delete" -> json("""{"code":200,"message":"success","data":null}""")
                else -> error("unexpected ${request.url}")
            }
        }
        val repo = AppManagementRepositoryImpl(OaaAppApi(stack.requester), testJson)

        val apps = repo.listApps().getOrThrow()
        assertEquals("suse_oj", apps.single().clientId)
        assertEquals("oaa_sec_e844********************bc0d", apps.single().displaySecret)

        val detail = repo.getAppDetail("suse_oj").getOrThrow()
        assertEquals("suse_oj", stack.requestsTo("/app/detail").single().url.parameters["client_id"])
        assertEquals("oj 平台", detail.name)

        val createdWithSecret = repo.createApp(
            clientId = "new_app",
            name = "测试应用",
            redirectUris = "https://example.invalid/callback",
            scope = ""
        ).getOrThrow()
        assertEquals("oaa_sec_plain_secret", createdWithSecret.clientSecret)
        val createBody = testJson.parseToJsonElement(stack.requestsTo("/app/create").first().bodyText()).jsonObject
        assertEquals("", createBody["scope"]?.jsonPrimitive?.content)

        createAsNull = true
        val createdFallback = repo.createApp(
            clientId = "new_app_2",
            name = "测试应用2",
            redirectUris = "https://example.invalid/cb2",
            scope = "openid"
        ).getOrThrow()
        assertEquals("new_app_2", createdFallback.clientId)
        assertEquals("", createdFallback.clientSecret)

        repo.updateApp(
            clientId = "new_app",
            name = "测试应用-已停用",
            redirectUris = "https://example.invalid/callback",
            scope = "",
            isActive = false
        ).getOrThrow()
        val updateBody = testJson.parseToJsonElement(stack.requestsTo("/app/update").single().bodyText()).jsonObject
        assertEquals(false, updateBody["is_active"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("", updateBody["scope"]?.jsonPrimitive?.content)

        repo.deleteApp("new_app").getOrThrow()
        val deleteBody = testJson.parseToJsonElement(stack.requestsTo("/app/delete").single().bodyText()).jsonObject
        assertEquals("new_app", deleteBody["client_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun oauthPublicAndAuthorizeUseCookieAndRefreshOnAuthRedirect() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val stack = OaaTestStack { request ->
            requests += request
            when (request.url.encodedPath) {
                "/v2/oauth/client/public" -> json(
                    """{"code":200,"message":"success","data":{
                        "client_id":"suse_oj",
                        "client_name":"oj 平台",
                        "scopes":[{"key":"openid","title":"基础身份标识","description":"验证身份"}]
                    }}"""
                )
                "/v2/auth/refresh" -> json("""{"code":200,"message":"success","data":{"token":"fresh_token","refresh_token":"r2"}}""")
                "/v2/oauth/authorize" -> {
                    val cookie = request.headers[HttpHeaders.Cookie]
                    if (cookie == "token=fresh_token") {
                        respond(
                            content = "",
                            status = HttpStatusCode.Found,
                            headers = headersOf(
                                HttpHeaders.Location,
                                "https://oj.suseoaa.com/oauth/suseoaa/callback?code=oaa_code_123&state=xyz"
                            )
                        )
                    } else {
                        respond(
                            content = "",
                            status = HttpStatusCode.Found,
                            headers = headersOf(
                                HttpHeaders.Location,
                                "https://suseoaa.com/auth?redirect=%2Fv2%2Foauth%2Fauthorize"
                            )
                        )
                    }
                }
                else -> error("unexpected ${request.url}")
            }
        }
        val noRedirectClient = OaaHttpClient.createBare(
            json = testJson,
            engine = MockEngine { request ->
                requests += request
                when (request.url.encodedPath) {
                    "/v2/oauth/authorize" -> {
                        val cookie = request.headers[HttpHeaders.Cookie]
                        if (cookie == "token=fresh_token") {
                            respond(
                                content = "",
                                status = HttpStatusCode.Found,
                                headers = headersOf(
                                    HttpHeaders.Location,
                                    "https://oj.suseoaa.com/oauth/suseoaa/callback?code=oaa_code_123&state=xyz"
                                )
                            )
                        } else {
                            respond(
                                content = "",
                                status = HttpStatusCode.Found,
                                headers = headersOf(
                                    HttpHeaders.Location,
                                    "https://suseoaa.com/auth?redirect=%2Fv2%2Foauth%2Fauthorize"
                                )
                            )
                        }
                    }
                    else -> error("unexpected noRedirect ${request.url}")
                }
            },
            followRedirects = false
        )

        stack.sessionStore.saveTokens("stale_token", "r1")
        stack.sessionStore.saveUserId(33)

        val oauthRepo = OAuthRepositoryImpl(
            OaaOAuthApi(stack.requester, noRedirectClient, stack.tokenManager)
        )

        val publicInfo = oauthRepo.getClientPublicInfo("suse_oj", scope = "openid").getOrThrow()
        assertEquals("oj 平台", publicInfo.clientName)
        assertEquals("openid", publicInfo.scopes.single().key)

        val authResult = oauthRepo.authorize(
            OAuthAuthorizeRequest(
                clientId = "suse_oj",
                redirectUri = "https://oj.suseoaa.com/oauth/suseoaa/callback",
                scope = "openid",
                state = "xyz"
            )
        ).getOrThrow()

        assertEquals("oaa_code_123", authResult.code)
        assertEquals("xyz", authResult.state)
        assertEquals("https://oj.suseoaa.com/oauth/suseoaa/callback?code=oaa_code_123&state=xyz", authResult.redirectUrl)
        val authCalls = requests.filter { it.url.encodedPath == "/v2/oauth/authorize" }
        assertEquals(listOf("token=stale_token", "token=fresh_token"), authCalls.map { it.headers[HttpHeaders.Cookie] })
        assertEquals(1, stack.requestsTo("/auth/refresh").size)

        // When refresh fails, authorize surfaces SessionExpired
        val failingStack = OaaTestStack { request ->
            when (request.url.encodedPath) {
                "/v2/auth/refresh" -> json("""{"code":400,"message":"token 无效","data":null}""", HttpStatusCode.BadRequest)
                else -> error("unexpected ${request.url}")
            }
        }
        failingStack.sessionStore.saveTokens("bad_token", "r_bad")
        failingStack.sessionStore.saveUserId(33)
        val failingNoRedirect = OaaHttpClient.createBare(
            json = testJson,
            engine = MockEngine {
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "https://suseoaa.com/auth?redirect=%2Fv2")
                )
            },
            followRedirects = false
        )
        val failingRepo = OAuthRepositoryImpl(
            OaaOAuthApi(failingStack.requester, failingNoRedirect, failingStack.tokenManager)
        )
        val err = (failingRepo.authorize(
            OAuthAuthorizeRequest(clientId = "suse_oj", redirectUri = "https://oj.suseoaa.com/cb")
        ).exceptionOrNull() as AppException).error
        assertIs<AppError.SessionExpired>(err)
    }
}
