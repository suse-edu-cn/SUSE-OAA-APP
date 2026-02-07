import Foundation
import CoreGraphics
import UIKit
import OnnxRuntimeBindings

/// ddddocr 的 iOS 移植版本
/// 使用 ONNX Runtime 进行验证码识别
@objc public class DdddOcrRecognizer: NSObject {

    @objc public static let shared = DdddOcrRecognizer()

    private var ortSession: ORTSession?
    private var ortEnv: ORTEnv?
    private var charset: [String] = []
    private var isInitialized = false

    private let targetHeight: Int = 64

    private override init() {
        super.init()
    }

    /// 初始化 ONNX Runtime 并加载模型
    @objc public func initialize() -> Bool {
        guard !isInitialized else {
            return true
        }

        do {
            // 加载字符集
            loadCharset()

            // 创建 ONNX Runtime 环境
            ortEnv = try ORTEnv(loggingLevel: ORTLoggingLevel.warning)

            // 从 Bundle 加载模型
            guard let modelPath = Bundle.main.path(forResource: "common_old", ofType: "onnx") else {
                print("[DdddOcr] 模型文件未找到")
                return false
            }

            // 创建会话选项
            let sessionOptions = try ORTSessionOptions()
            try sessionOptions.setGraphOptimizationLevel(ORTGraphOptimizationLevel.all)

            // 创建推理会话
            ortSession = try ORTSession(env: ortEnv!, modelPath: modelPath, sessionOptions: sessionOptions)

            isInitialized = true
            print("[DdddOcr] 模型加载成功")
            return true
        } catch {
            print("[DdddOcr] 初始化失败: \(error)")
            return false
        }
    }

    /// 检查是否有可用的模型文件
    @objc public func hasModel() -> Bool {
        return Bundle.main.path(forResource: "common_old", ofType: "onnx") != nil
    }

    /// 识别验证码
    @objc public func recognize(imageData: Data) -> String? {
        guard isInitialized, let session = ortSession else {
            print("[DdddOcr] 模型未初始化")
            return nil
        }

        // 1. 解码图片
        guard let uiImage = UIImage(data: imageData),
              let cgImage = uiImage.cgImage
        else {
            print("[DdddOcr] 无法解码图片")
            return nil
        }

        print("[DdddOcr] 原始图片尺寸: \(cgImage.width)x\(cgImage.height)")

        do {
            // 2. 预处理图片
            let inputTensor = try preprocessImage(cgImage)

            // 3. 运行推理
            // Objective-C 方法 inputNamesWithError: 在 Swift 中变为 inputNames()
            let inputNames = try session.inputNames()
            guard let inputName = inputNames.first else {
                print("[DdddOcr] 无法获取输入名称")
                return nil
            }

            // 获取输出名称
            let outputNames = try session.outputNames()
            let outputNameSet = Set(outputNames)

            let outputs = try session.run(
                withInputs: [inputName: inputTensor],
                outputNames: outputNameSet,
                runOptions: nil
            )

            // 4. CTC 解码
            guard let firstOutputName = outputNames.first,
                  let outputTensor = outputs[firstOutputName]
            else {
                print("[DdddOcr] 无法获取输出")
                return nil
            }

            let result = ctcDecode(outputTensor)
            print("[DdddOcr] 识别结果: \(result)")

            return result.isEmpty ? nil : result
        } catch {
            print("[DdddOcr] 识别异常: \(error)")
            return nil
        }
    }

    /// 预处理图片
    private func preprocessImage(_ cgImage: CGImage) throws -> ORTValue {
        // 计算目标宽度（保持宽高比）
        let scale = Float(targetHeight) / Float(cgImage.height)
        let targetWidth = max(1, Int(Float(cgImage.width) * scale))

        // 创建灰度图像
        let colorSpace = CGColorSpaceCreateDeviceGray()
        guard let context = CGContext(
            data: nil,
            width: targetWidth,
            height: targetHeight,
            bitsPerComponent: 8,
            bytesPerRow: targetWidth,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        )
        else {
            throw NSError(domain: "DdddOcr", code: -1, userInfo: [NSLocalizedDescriptionKey: "无法创建图形上下文"])
        }

        // 绘制缩放后的图像
        context.interpolationQuality = .high
        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: targetWidth, height: targetHeight))

        guard let grayData = context.data else {
            throw NSError(domain: "DdddOcr", code: -2, userInfo: [NSLocalizedDescriptionKey: "无法获取图像数据"])
        }

        print("[DdddOcr] 缩放后尺寸: \(targetWidth)x\(targetHeight)")

        // 转换为归一化的 Float 数组: (x/255 - 0.5) / 0.5
        var floatData = [Float](repeating: 0, count: targetHeight * targetWidth)
        let pixelBuffer = grayData.bindMemory(to: UInt8.self, capacity: targetHeight * targetWidth)

        for i in 0..<(targetHeight * targetWidth) {
            let grayValue = Float(pixelBuffer[i])
            floatData[i] = (grayValue / 255.0 - 0.5) / 0.5
        }

        // 创建 ONNX Tensor [1, 1, height, width]
        let shape: [NSNumber] = [1, 1, NSNumber(value: targetHeight), NSNumber(value: targetWidth)]
        let inputData = Data(bytes: floatData, count: floatData.count * MemoryLayout<Float>.size)

        let inputTensor = try ORTValue(
            tensorData: NSMutableData(data: inputData),
            elementType: ORTTensorElementDataType.float,
            shape: shape
        )

        return inputTensor
    }

    /// CTC 解码
    private func ctcDecode(_ tensor: ORTValue) -> String {
        guard let tensorInfo = try? tensor.tensorTypeAndShapeInfo(),
              let shape = tensorInfo.shape as? [Int]
        else {
            return ""
        }

        guard let tensorData = try? tensor.tensorData() as Data else {
            return ""
        }

        print("[DdddOcr] 输出形状: \(shape), charset大小: \(charset.count)")

        // 确定各维度
        let seqLength: Int
        let numClasses: Int

        if shape.count == 3 {
            if shape[1] == 1 {
                seqLength = shape[0]
                numClasses = shape[2]
            } else {
                seqLength = shape[1]
                numClasses = shape[2]
            }
        } else if shape.count == 2 {
            seqLength = shape[0]
            numClasses = shape[1]
        } else {
            return ""
        }

        print("[DdddOcr] seqLength=\(seqLength), numClasses=\(numClasses)")

        // 将数据转换为 Float 数组
        let floatData = tensorData.withUnsafeBytes { (pointer: UnsafeRawBufferPointer) -> [Float] in
            guard let baseAddress = pointer.baseAddress else {
                return []
            }
            let floatPointer = baseAddress.assumingMemoryBound(to: Float.self)
            return Array(UnsafeBufferPointer(start: floatPointer, count: seqLength * numClasses))
        }

        // 贪婪解码
        var result = ""
        var prevIndex = -1

        for t in 0..<seqLength {
            var maxIndex = 0
            var maxValue: Float = -.infinity

            for c in 0..<numClasses {
                let value = floatData[t * numClasses + c]
                if value > maxValue {
                    maxValue = value
                    maxIndex = c
                }
            }

            // CTC 解码规则: 0 是空白符，重复字符只保留一个
            if maxIndex != 0 && maxIndex != prevIndex {
                if maxIndex < charset.count {
                    let char = charset[maxIndex]
                    if !char.isEmpty {
                        result += char
                    }
                }
            }

            prevIndex = maxIndex
        }

        return result
    }

    /// 加载字符集
    private func loadCharset() {
        // 尝试从 Bundle 加载 charsets_old.json
        if let charsetPath = Bundle.main.path(forResource: "charsets_old", ofType: "json"),
           let jsonData = FileManager.default.contents(atPath: charsetPath),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            charset = parseJsonArray(jsonString)
            print("[DdddOcr] 从文件加载字符集成功, 大小: \(charset.count)")
        } else {
            // 使用内置的简化字符集
            print("[DdddOcr] 无法加载字符集文件, 使用内置字符集")
            charset = [""] + "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".map {
                String($0)
            }
        }
    }

    /// 解析 JSON 字符串数组
    private func parseJsonArray(_ json: String) -> [String] {
        guard let data = json.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [String]
        else {
            return []
        }
        return array
    }
}
