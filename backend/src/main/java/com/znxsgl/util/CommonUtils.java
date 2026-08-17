package com.znxsgl.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 通用工具方法集合（基于 Hutool 封装）
 *
 * 示范 Hutool 的常用 API：
 * - {@link Convert}：类型转换，替代手写 (String) obj / ((Number) obj).intValue() 等强转
 * - {@link StrUtil}：字符串判空、格式化
 * - {@link CollUtil}：集合判空、操作
 * - {@link DateUtil}：日期时间格式化
 * - {@link BeanUtil}：Bean 复制（与 MapStruct 互补，适用于一次性场景）
 *
 * 用 Hutool 替代手写强转，可显著减少 ClassCastException 风险。
 */
public final class CommonUtils {

    private CommonUtils() {}

    /** 安全转 String，null 返回空串 */
    public static String toStr(Object obj) {
        return Convert.toStr(obj);
    }

    /** 安全转 int，null/异常返回 0 */
    public static int toInt(Object obj) {
        return Convert.toInt(obj, 0);
    }

    /** 安全转 long */
    public static long toLong(Object obj) {
        return Convert.toLong(obj, 0L);
    }

    /** 从 Map 中安全取值并转换类型 */
    public static <T> T getFromMap(Map<String, Object> map, String key, Class<T> clazz) {
        return Convert.convert(clazz, map.get(key));
    }

    /** 解析 "HH:mm" 字符串为 LocalTime，失败返回 null */
    public static LocalTime parseTime(String time) {
        if (StrUtil.isBlank(time)) return null;
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            return null;
        }
    }

    /** 判断集合是否非空 */
    public static boolean notEmpty(List<?> list) {
        return CollUtil.isNotEmpty(list);
    }

    /** 格式化 LocalDateTime 为 "yyyy-MM-dd HH:mm:ss" */
    public static String formatDateTime(LocalDateTime time) {
        return time == null ? "" : DateUtil.formatLocalDateTime(time);
    }
}
