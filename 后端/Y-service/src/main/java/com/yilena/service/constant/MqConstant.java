package com.yilena.service.constant;

/**
 * RabbitMQ消息队列常量类
 * 
 * 功能说明：
 * 1. 定义交换机名称
 * 2. 定义各业务模块的队列和绑定键名称
 * 3. 定义CRUD操作类型
 * 
 * 交换机：YVW.CRUD.exchange
 * 
 * 队列模块：
 * - user: 用户相关
 * - follow: 关注相关
 * - collection: 合集相关
 * - favorites: 收藏夹相关
 * - video: 视频相关
 * - post: 动态相关
 * - chat: 聊天相关
 * - history: 历史记录相关
 * - hotSearch: 热搜相关
 * - videoFavorites: 视频收藏相关
 * 
 * 操作类型：
 * - SEND_ADD = 1: 新增
 * - SEND_PUT = 2: 修改
 * - SEND_DELETE = 3: 删除
 * - SEND_QUERY = 4: 查询
 * 
 */
public class MqConstant {

    public static final String CRUD_EXCHANGE = "YVW.CRUD.exchange";

    public static final String USER_QUEUE = "user.queue";
    public static final String USER_BINDING_KEY = "user.key";

    public static final String FOLLOW_QUEUE = "follow.queue";
    public static final String FOLLOW_BINDING_KEY = "follow.key";

    public static final String COLLECTION_BINDING_KEY = "collection.key";
    public static final String COLLECTION_QUEUE = "collection.queue";

    public static final String FAVORITES_QUEUE = "favorites.queue";
    public static final String FAVORITES_BINDING_KEY = "favorites.key";

    public static final String VIDEO_QUEUE = "video.queue";
    public static final String VIDEO_BINDING_KEY = "video.key";

    public static final String POST_QUEUE = "post.queue";
    public static final String POST_BINDING_KEY = "post.key";

    public static final String CHAT_QUEUE = "chat.queue";
    public static final String CHAT_BINDING_KEY = "chat.key";

    public static final String HISTORY_QUEUE = "history.queue";
    public static final String HISTORY_BINDING_KEY = "history.key";

    public static final String HOT_SEARCH_QUEUE = "hotSearch.queue";
    public static final String HOT_SEARCH_BINDING_KEY = "hotSearch.key";

    public static final String VIDEO_FAVORITES_QUEUE = "videoFavorites.queue";
    public static final String VIDEO_FAVORITES_BINDING_KEY = "videoFavorites.key";

    public static final Integer SEND_ADD = 1;
    public static final Integer SEND_PUT = 2;
    public static final Integer SEND_DELETE = 3;
    public static final Integer SEND_QUERY = 4;
}
