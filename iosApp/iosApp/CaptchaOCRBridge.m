// CaptchaOCRBridge.m
// Objective-C 桥接实现，让 Kotlin/Native 可以调用 Swift 的 CaptchaOCR

#import "CaptchaOCRBridge.h"
#import "青蟹-Swift.h"  // 自动生成的 Swift 头文件

@implementation CaptchaOCRBridge

+ (instancetype)shared {
    static CaptchaOCRBridge *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[CaptchaOCRBridge alloc] init];
    });
    return instance;
}

- (nullable NSString *)recognizeWithImageData:(NSData *)imageData {
    return [[CaptchaOCR shared] recognizeWithImageData:imageData];
}

@end
