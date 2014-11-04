# Copyright 2007-2008 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := MmsAppTests

LOCAL_STATIC_JAVA_LIBRARIES += android-common jsr305

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
