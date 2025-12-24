# 1. 设置 CMake 最低版本
cmake_minimum_required(VERSION 3.18.1)

# 2. 项目名称
project("oaa-native")

# 3. 设置 C++ 标准 (httplib 需要 C++11 以上，推荐 17)
set(CMAKE_CXX_STANDARD 17)

# 4. 定义 OpenSSL 根路径 (指向当前 CMakeLists.txt 所在目录)
set(OPENSSL_ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# 5. 添加头文件搜索路径
# 这样代码里的 #include <openssl/ssl.h> 才能找到文件
include_directories(${OPENSSL_ROOT_DIR}/include)

# 6. 导入静态库 libssl.a
add_library(libssl STATIC IMPORTED)
set_target_properties(libssl PROPERTIES IMPORTED_LOCATION
    ${OPENSSL_ROOT_DIR}/libs/${ANDROID_ABI}/libssl.a)

# 7. 导入静态库 libcrypto.a
add_library(libcrypto STATIC IMPORTED)
set_target_properties(libcrypto PROPERTIES IMPORTED_LOCATION
    ${OPENSSL_ROOT_DIR}/libs/${ANDROID_ABI}/libcrypto.a)

# 8. 编译你的源码生成动态库 (.so)
add_library(oaa-native SHARED
        native-lib.cpp    # JNI 接口
        Evaluator.cpp     # 核心逻辑
        # .h 和 .hpp 不需要列在这里，只要 #include 能找到即可
)

# 9. 查找安卓系统的日志库 (用于 __android_log_print)
find_library(log-lib log)

# 10. 链接所有库到你的动态库
# 注意顺序：libssl 必须在 libcrypto 之前
target_link_libraries(oaa-native
        libssl
        libcrypto
        ${log-lib})

# 11. 开启 httplib 的 SSL 支持宏
# 这相当于在 C++ 代码里写了 #define CPPHTTPLIB_OPENSSL_SUPPORT
target_compile_definitions(oaa-native PRIVATE CPPHTTPLIB_OPENSSL_SUPPORT)