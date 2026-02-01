// CaptchaOCRBridge.h
// Objective-C 桥接头文件，让 Kotlin/Native 可以调用 Swift 的 CaptchaOCR

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Objective-C 桥接类，用于从 Kotlin/Native 调用 Swift CaptchaOCR
@interface CaptchaOCRBridge : NSObject

/// 单例访问
+ (instancetype)shared;

/// 识别验证码
/// @param imageData 图片数据 (PNG/JPEG)
/// @return 识别结果，失败返回 nil
- (nullable NSString *)recognizeWithImageData:(NSData *)imageData;

@end

NS_ASSUME_NONNULL_END
