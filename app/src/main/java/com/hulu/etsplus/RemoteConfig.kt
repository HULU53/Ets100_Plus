package com.hulu.etsplus

/**
 * 远程配置数据类
 *
 * @param isKillSwitchOn 远程锁定开关
 * @param noticeMessage 启动时的 Toast 公告
 * @param announcementTitle 首页公告标题
 * @param announcementMessage 首页公告/公告详情正文
 * @param announcementUpdatedAt 公告更新时间
 * @param announcementUrl 公告详情远程地址
 * @param changelogUrl 更新日志远程地址
 * @param changelogTitle 首页更新日志标题
 * @param changelogSummary 首页更新日志摘要
 */
data class RemoteConfig(
    val isKillSwitchOn: Boolean,
    val noticeMessage: String,
    val announcementTitle: String = "",
    val announcementMessage: String = "",
    val announcementUpdatedAt: String = "",
    val announcementUrl: String = "",
    val changelogUrl: String = "",
    val changelogTitle: String = "",
    val changelogSummary: String = ""
)

/**
 * 远程配置运行时状态
 */
data class RemoteStatus(
    val isKillSwitch: Boolean,      // 是否 KillSwitch 锁定
    val noticeMessage: String,      // 公告内容（用于 Toast）
    val announcementTitle: String = "",
    val announcementMessage: String = "",
    val announcementUpdatedAt: String = "",
    val announcementUrl: String = "",
    val changelogUrl: String = "",
    val changelogTitle: String = "",
    val changelogSummary: String = ""
)
