package com.islxz.weather.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by Qingsu on 2017/6/12.
 */

public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
