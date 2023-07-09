/*
 * Simplified Chinese (简体中文)
 *
 * 版权所有 (C) 2023 QiLechan <qilechan@outlook.com> 和本程序的贡献者
 *
 * 本程序是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是 3 任何以后版都可以。
 * 发布该程序是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 * 你应该随程序获得一份 GNU 通用公共许可证的副本。如果没有，请看 <https://www.gnu.org/licenses/>。
 * English (英语)
 *
 * Copyright (C) 2023 QiLechan <qilechan@outlook.com> and contributors to this program
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or 3 any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.yuezhikong.JavaIMAndroid.Encryption;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.yuezhikong.JavaIMAndroid.utils.FileUtils;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;

    public class RSA {
        public static KeyData loadPublicKeyFromFile(String filePath)
        {
            try {
                String keyString = FileUtils.readTxt(filePath).toString();
                KeyData keyData = new KeyData();
                keyData.PublicKey = keyString;
                return keyData;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }
        @NonNull
        public static KeyData generateKeyToReturn()
        {
            KeyData keyData = new KeyData();
            KeyPair pair = SecureUtil.generateKeyPair("RSA");
            keyData.privateKey = pair.getPrivate();
            keyData.publicKey = pair.getPublic();
            return keyData;
        }

        public static String encrypt(String Message, String PublicKey)
        {
            cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(null,PublicKey);
            return Base64.encodeToString(rsa.encrypt(Message, KeyType.PublicKey),Base64.NO_WRAP);
        }

        public static String decrypt(String message, PrivateKey privateKey) {
            cn.hutool.crypto.asymmetric.RSA rsa = new cn.hutool.crypto.asymmetric.RSA(privateKey, null);
            return rsa.decryptStr(message, KeyType.PrivateKey);
        }

    }