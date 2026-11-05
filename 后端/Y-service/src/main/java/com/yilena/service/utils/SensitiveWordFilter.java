package com.yilena.service.utils;

import com.github.houbb.sensitive.word.core.SensitiveWordHelper;

/**
 * 敏感词过滤工具类
 * 
 * 功能说明：
 * 1. 检测文本中的敏感词
 * 2. 替换敏感词为指定字符
 * 3. 支持自定义敏感词库
 * 
 * 使用场景：
 * - 用户评论内容过滤
 * - 帖子内容审核
 * - 聊天消息过滤
 * 
 * 技术实现：
 * - 使用houbb的sensitive-word库
 * - 支持DFA算法进行高效匹配
 * - 支持中文敏感词过滤
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
public class SensitiveWordFilter {

    /**
     * 过滤敏感词
     * 
     * 功能：检测并替换文本中的敏感词
     * 业务逻辑：
     * 1. 检测文本是否包含敏感词
     * 2. 如果包含，将敏感词替换为'喵'
     * 3. 返回处理后的文本
     * 
     * @param content 待过滤的文本内容
     * @return 过滤后的文本内容
     */
    public static String filterSensitiveWords(String content) {
        if(SensitiveWordHelper.contains(content)) {
            return SensitiveWordHelper.replace(content, '喵');
        }
        return content;
    }
}
