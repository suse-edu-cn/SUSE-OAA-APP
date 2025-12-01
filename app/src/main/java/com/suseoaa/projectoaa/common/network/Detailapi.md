//给后端开发者的说明：
//
//你们需要实现一个 API 端点，例如 GET /api/task/{taskId}。
//
//它需要接收一个 Authorization 头（例如 <token>）。
//
//当请求 GET /api/task/1 时，你们需要返回如下格式的 JSON：
//
//JSON
//
//{
//    "taskId": "1",
//    "title": "协会事务处理事项 #2",
//    "blocks": [
//    {
//        "title": "任务状态",
//        "content": "进行中"
//    },
//    {
//        "title": "详细描述",
//        "content": "请相关负责人尽快处理此事务，确保项目按时推进。"
//    },
//    {
//        "title": "截止日期",
//        "content": "2025-12-31"
//    }
//    ]
//}