import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

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

