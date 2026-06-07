package com.example.metricplatform.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.AssetStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "asset", autoResultMap = true)
public class Asset extends BaseEntity {

    /**
     * 素材ID
     */
    @TableId("asset_id")
    private String assetId;

    /**
     * 素材标题
     */
    @TableField("title")
    private String title;

    /**
     * 上传人
     */
    @TableField("uploader")
    private String uploader;

    /**
     * 上传时间
     */
    @TableField("uploaded_at")
    private LocalDateTime uploadedAt;

    /**
     * 文件大小(字节)
     */
    @TableField("file_size_bytes")
    private Long fileSizeBytes;

    /**
     * 审核状态
     */
    @TableField("status")
    private AssetStatus status;

    /**
     * 审核人
     */
    @TableField("reviewer")
    private String reviewer;

    /**
     * 审核时间
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 标签数组
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 城市
     */
    @TableField("city")
    private String city;

    /**
     * 投放平台
     */
    @TableField("platform")
    private String platform;

    /**
     * 视频时长(秒)
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 曝光次数
     */
    @TableField("impression_count")
    private Long impressionCount;

    /**
     * 播放次数
     */
    @TableField("play_count")
    private Long playCount;

    /**
     * 完播次数
     */
    @TableField("complete_play_count")
    private Long completePlayCount;
}
