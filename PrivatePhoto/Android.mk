LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := PrivatePhoto
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v4 \
        android-support-v7-appcompat \
        android-support-v7-gridlayout \
        android-support-v7-mediarouter \
        android-support-v7-palette \
        android-support-v7-recyclerview \
        android-support-v7-cardview \
        android-support-v13
LOCAL_RESOURCE_DIR = \
        $(LOCAL_PATH)/res \
        frameworks/support/v7/appcompat/res \
        frameworks/support/v7/gridlayout/res \
        frameworks/support/v7/mediarouter/res \
        frameworks/support/v7/cardview/res \
        frameworks/support/design/res \
        frameworks/support/v7/recyclerview/res
LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --extra-packages android.support.v7.appcompat \
        --extra-packages android.support.v7.cardview \
        --extra-packages android.support.v7.gridlayout \
        --extra-packages android.support.v7.mediarouter \
        --extra-packages android.support.design \
        --extra-packages android.support.v7.recyclerview
include $(BUILD_PACKAGE)