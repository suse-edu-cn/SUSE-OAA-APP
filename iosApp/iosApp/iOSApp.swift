import SwiftUI
import ComposeApp
import BackgroundTasks
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate {

    let taskIdentifier = "com.suseoaa.projectoaa.checkin-refresh"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // 注册后台任务
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: taskIdentifier,
            using: nil
        ) { task in
            self.handleCheckinRefresh(task: task as! BGAppRefreshTask)
        }

        // 请求通知权限
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("[iOS AppDelegate] 通知权限已获取")
            } else {
                print("[iOS AppDelegate] 通知权限被拒绝: \(error?.localizedDescription ?? "")")
            }
        }
        UNUserNotificationCenter.current().delegate = self

        // 调度后台任务
        scheduleCheckinRefresh()

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // 每次回到前台重新调度，确保任务持续有效
        scheduleCheckinRefresh()
    }

    func scheduleCheckinRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)

        // 从 Kotlin 读取配置，设置最早执行时间
        IosKoinInitKt.initializeKoinIfNeeded()
        let config = IosBackgroundCheckinKt.getConfigSync()

        if config.enabled && !config.targetAccountIds.isEmpty {
            // 计算下次签到时间（Swift 侧简单计算）
            let nextRun = calculateNextRunDate(hour: Int(config.scheduledHour), minute: Int(config.scheduledMinute))
            request.earliestBeginDate = nextRun
            print("[iOS AppDelegate] 后台任务已调度，最早执行时间: \(nextRun)")
        } else {
            // 默认1小时后
            request.earliestBeginDate = Date().addingTimeInterval(3600)
            print("[iOS AppDelegate] 后台任务已调度（默认1小时后）")
        }

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("[iOS AppDelegate] 提交后台任务失败: \(error)")
        }
    }

    func handleCheckinRefresh(task: BGAppRefreshTask) {
        // 立即调度下一个任务
        scheduleCheckinRefresh()

        // 初始化 Koin
        IosKoinInitKt.initializeKoinIfNeeded()

        // 发送通知告知用户
        scheduleLocalNotification(title: "定时签到", body: "正在执行自动签到...")

        // 设置过期处理
        let expired = false
        task.expirationHandler = {
            // 任务超时
            print("[iOS AppDelegate] 后台任务超时")
        }

        // 执行签到
        IosBackgroundCheckinKt.executeBackgroundCheckin { success in
            let isSuccess = success.boolValue
            if isSuccess {
                self.scheduleLocalNotification(title: "签到完成", body: "自动签到已成功执行")
            } else {
                self.scheduleLocalNotification(title: "签到提醒", body: "自动签到未能完成，请打开应用手动签到")
            }
            task.setTaskCompleted(success: isSuccess)
        }
    }

    func scheduleLocalNotification(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3, repeats: false)
        let request = UNNotificationRequest(
            identifier: "checkin-\(Date().timeIntervalSince1970)",
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request)
    }

    /// 计算下次签到时间
    private func calculateNextRunDate(hour: Int, minute: Int) -> Date {
        let calendar = Calendar(identifier: .gregorian)
        var components = calendar.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = minute
        components.second = 0
        components.timeZone = TimeZone(identifier: "Asia/Shanghai")

        var targetDate = calendar.date(from: components)!

        // 如果今天该时间已过，设为明天
        if targetDate <= Date() {
            targetDate = calendar.date(byAdding: .day, value: 1, to: targetDate)!
        }

        return targetDate
    }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // App 在前台时也显示通知
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        // 用户点击通知，打开应用后协程调度器会自动处理
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        // 在应用启动时注册 ddddocr
        registerDdddOcr()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    /// 注册 ddddocr 到 Kotlin/Native
    private func registerDdddOcr() {
        // 创建符合 IOSOcrRecognizer 协议的适配器
        let adapter = DdddOcrAdapter()
        IOSOcrRegistry.shared.setRecognizer(recognizer: adapter)
    }
}

/// ddddocr 适配器，实现 Kotlin 的 IOSOcrRecognizer 接口
class DdddOcrAdapter: IOSOcrRecognizer {

    init() {
        // 初始化 ddddocr
        _ = CaptchaOCR.shared
    }

    func recognize(imageData: Foundation.Data) -> String? {
        // 调用 Swift 的 CaptchaOCR
        return CaptchaOCR.shared.recognize(imageData: imageData)
    }
}
