package com.snow;

import cn.hutool.http.HttpUtil;

public class download {

    public void getDownloadUrl(){

        // 构建API URL并直接附加上查询参数
        String apiUrl = "https://api.bilibili.com/x/player/wbi/playurl?avid=113939459346503&cid=28199357904&fnval=80";

        // 直接构建Cookie字符串（替换下方值即可）
        String cookie = "sid=q6llnc6z; " +      // ← 替换这里
                "DedeUserID=497373611; " +  // ← 替换这里
                "DedeUserID__ckMd5=462da33d3dd8d2ea; " +
                "bili_jct=5b4447ade5066895212d3ac261f7a818; " +  // ← 替换这里
                "SESSDATA=3b6b8186%2C1754353318%2C49212%2A22CjA3xv2ccexHaxkhGiLoxDsOh3rAcVcm2gTqrTlWi3BuqfQXukhABO4_fXIR8m5mA-gSVnZjd1R5STI3dHRfcEE4bEJQa19ERTJ6WTBmSVh0Z2xjX0dlNWJLYlAxc3pueExDbzlEN1RRUlJOYzY1UEdwdDRJSWxWMi1zdkY4Y0J0SVBrY2JKbG13IIEC";    // ← 替换这里

        String response =  HttpUtil.createGet(apiUrl)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Cookie", cookie)
                .execute().body();
        System.out.println(response);
    }

    public static void main(String[] args) {
        download d = new download();
        d.getDownloadUrl();
    }
}
