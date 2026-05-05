package com.community.platform.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 加密工具类（按需求文档技术选型）
 */
public class MD5Util {
    
    /**
     * MD5 加密
     * @param plainText 明文
     * @return 32位小写MD5密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(plainText.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    /**
     * 验证密码
     * @param plainText 明文
     * @param encryptedText MD5密文
     * @return 是否匹配
     */
    public static boolean verify(String plainText, String encryptedText) {
        if (plainText == null || encryptedText == null) {
            return false;
        }
        return encrypt(plainText).equals(encryptedText);
    }
}
