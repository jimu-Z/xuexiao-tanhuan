package com.ruoyi.common.utils;

import com.ruoyi.common.constant.UserConstants;

/** Keeps password-setting limits aligned with the login endpoint. */
public final class PasswordLengthUtils
{
    private PasswordLengthUtils() {}

    public static boolean isValid(String password)
    {
        return password != null && password.length() >= UserConstants.PASSWORD_MIN_LENGTH
            && password.length() <= UserConstants.PASSWORD_MAX_LENGTH;
    }

    public static String errorMessage()
    {
        return "密码长度必须在" + UserConstants.PASSWORD_MIN_LENGTH + "到"
            + UserConstants.PASSWORD_MAX_LENGTH + "个字符之间";
    }
}
