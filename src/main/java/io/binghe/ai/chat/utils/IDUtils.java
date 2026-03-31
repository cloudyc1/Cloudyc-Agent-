package io.binghe.ai.chat.utils;

import java.util.UUID;

/**
 * @author binghe(微信 : hacker_binghe)
 * @version 1.0.0
 * @description ID工具类
 * @github https://github.com/binghe001
 * @copyright 公众号: 冰河技术
 */
public class IDUtils {

    public static  String getID(int length){
        return UUID.randomUUID().toString().substring(0, length);
    }
}
