cmake_minimum_required(VERSION 3.22.1)

project(suseoaa_litert_preload)

add_library(suseoaa_litert_preload SHARED
    litert_preload.c
)

target_link_options(suseoaa_litert_preload PRIVATE
    "-Wl,-z,max-page-size=16384"
)

target_link_libraries(suseoaa_litert_preload
    android
    log
    dl
)

add_library(LiteRtTopKOpenClSampler SHARED
    opencl_sampler_proxy.c
)

set_target_properties(LiteRtTopKOpenClSampler PROPERTIES
    OUTPUT_NAME "LiteRtTopKOpenClSampler"
    NO_SONAME TRUE
)

target_link_options(LiteRtTopKOpenClSampler PRIVATE
    "-Wl,-z,max-page-size=16384"
)

target_link_libraries(LiteRtTopKOpenClSampler
    android
    log
    dl
)
