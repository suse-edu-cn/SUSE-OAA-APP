import Foundation
import Vision
import UIKit

/// iOS 验证码 OCR 识别器
/// 优先使用 ddddocr ONNX 模型，如果不可用则回退到 Vision Framework
@objc public class CaptchaOCR: NSObject {
    
    @objc public static let shared = CaptchaOCR()
    
    private var ddddOcrAvailable = false
    
    private override init() {
        super.init()
        // 尝试初始化 ddddocr
        initializeDdddOcr()
    }
    
    /// 初始化 ddddocr
    private func initializeDdddOcr() {
        if DdddOcrRecognizer.shared.hasModel() {
            ddddOcrAvailable = DdddOcrRecognizer.shared.initialize()
            if ddddOcrAvailable {
                print("[iOS OCR] ddddocr 初始化成功，使用 ONNX 模型识别")
            } else {
                print("[iOS OCR] ddddocr 初始化失败，回退到 Vision Framework")
            }
        } else {
            print("[iOS OCR] ddddocr 模型未找到，使用 Vision Framework")
        }
    }
    
    /// 识别验证码图片中的文字
    /// - Parameter imageData: 图片数据 (PNG/JPEG)
    /// - Returns: 识别结果，失败返回 nil
    @objc public func recognize(imageData: Data) -> String? {
        // 优先使用 ddddocr
        if ddddOcrAvailable {
            if let result = DdddOcrRecognizer.shared.recognize(imageData: imageData) {
                return result
            }
            print("[iOS OCR] ddddocr 识别失败，尝试 Vision Framework")
        }
        
        // 回退到 Vision Framework
        return recognizeWithVision(imageData: imageData)
    }
    
    /// 使用 Vision Framework 识别
    private func recognizeWithVision(imageData: Data) -> String? {
        guard let uiImage = UIImage(data: imageData),
              let cgImage = uiImage.cgImage else {
            print("[iOS OCR] 无法解码图片")
            return nil
        }
        
        var recognizedText = ""
        let semaphore = DispatchSemaphore(value: 0)
        
        // 创建文字识别请求
        let request = VNRecognizeTextRequest { request, error in
            defer { semaphore.signal() }
            
            if let error = error {
                print("[iOS OCR] 识别错误: \(error.localizedDescription)")
                return
            }
            
            guard let observations = request.results as? [VNRecognizedTextObservation] else {
                return
            }
            
            for observation in observations {
                if let topCandidate = observation.topCandidates(1).first {
                    recognizedText += topCandidate.string
                }
            }
        }
        
        // 配置识别参数
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["en-US"]  // 验证码通常是英文/数字
        request.usesLanguageCorrection = false    // 验证码不需要语言校正
        
        // 执行识别
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([request])
            semaphore.wait()
        } catch {
            print("[iOS OCR] 执行错误: \(error.localizedDescription)")
            return nil
        }
        
        // 清理结果
        let cleaned = cleanCaptchaText(recognizedText)
        print("[iOS OCR] 识别结果: \(cleaned) (原始: \(recognizedText))")
        
        return cleaned.isEmpty ? nil : cleaned
    }
    
    /// 清理识别结果
    private func cleanCaptchaText(_ text: String) -> String {
        // 移除空格和换行
        var cleaned = text.replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: "\r", with: "")
        
        // 只保留字母和数字
        cleaned = String(cleaned.filter { $0.isLetter || $0.isNumber })
        
        // 处理常见的OCR误识别
        var corrected = ""
        for char in cleaned {
            let fixed: Character
            switch char {
            case "O", "o": fixed = "0"
            case "l", "I", "|": fixed = "1"
            case "Z": fixed = "2"
            case "S", "$": fixed = "5"
            case "B": fixed = "8"
            case "G": fixed = "6"
            case "q": fixed = "9"
            case "D": fixed = "0"
            default: fixed = char
            }
            corrected.append(fixed)
        }
        
        return corrected.uppercased()
    }
}
