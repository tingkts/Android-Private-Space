LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := PrivateFolderDemo
#LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_RESOURCE_DIR = $(LOCAL_PATH)/res
#LOCAL_AAPT_FLAGS := --auto-add-overlay
include $(BUILD_PACKAGE)