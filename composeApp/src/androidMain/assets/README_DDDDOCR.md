# ddddocr 模型文件说明

## 下载地址
请从以下地址下载 `common_old.onnx` 模型文件：

### 方法1: 直接下载
https://github.com/sml2h3/ddddocr/raw/master/ddddocr/common_old.onnx

### 方法2: pip 安装后复制
```bash
pip install ddddocr
# 模型位置：site-packages/ddddocr/common_old.onnx
```

## 使用说明
1. 下载 `common_old.onnx` 文件（约 10MB）
2. 将文件放到此目录：`composeApp/src/androidMain/assets/`
3. 重新编译应用

## 模型信息
- 输入: 灰度图片，高度固定为64像素，宽度自适应
- 输出: 字符序列 (CTC 解码)
- 支持字符: 0-9, a-z, A-Z (共62个字符)
- 模型大小: 约 10MB

## 注意事项
- 如果不放置模型文件，应用会自动使用 ML Kit 作为后备方案
- ddddocr 对扭曲验证码的识别率远高于普通 OCR
