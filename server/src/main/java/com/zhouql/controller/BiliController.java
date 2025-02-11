package com.zhouql.controller;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SnowDreamXUE
 * 代理控制器
 */
@RestController
public class BiliController {
    /**
     * 移动端链接
     */
    @GetMapping("/move")
    public String getMobileContent(@RequestParam("url") String url) {
        return HttpUtil.createGet(url).execute().body();
    }

    /**
     * 根据bv获取详细信息，其中avid和cid很重要
     */
    @GetMapping("/av/{bv}")
    public String getAv(@PathVariable("bv") String bv) {
        return HttpUtil.createGet("https://api.bilibili.com/x/web-interface/view?bvid=" + bv).execute().body();
    }

    // 修改下载接口
    @GetMapping("/download/{avid}/{cid}")
    public String getDownloadUrl(
            @PathVariable("avid") String avid,
            @PathVariable("cid") String cid,
            @RequestParam("qn") String qn) {

        // 构建API URL并直接附加上查询参数
        String apiUrl = String.format("https://api.bilibili.com/x/player/playurl?avid=%s&cid=%s&qn=%s&fnval=80",
                avid, cid, qn);

//        String apiUrl = "https://api.bilibili.com/x/player/wbi/playurl?avid=113939459346503&cid=28199357904&fnval=80";
//        // 构建Cookie字符串
//        StringBuilder cookieBuilder = new StringBuilder();
//        for (Map.Entry<String, String> entry : cookiesStore.entrySet()) {
//            if (cookieBuilder.length() > 0) {
//                cookieBuilder.append("; ");
//            }
//            cookieBuilder.append(entry.getKey()).append("=").append(entry.getValue());
//        }

        // 直接构建Cookie字符串（替换下方值即可）
        String cookie = "sid=q6llnc6z; " +      // ← 替换这里
                "DedeUserID=497373611; " +  // ← 替换这里
                "DedeUserID__ckMd5=462da33d3dd8d2ea; " +
                "bili_jct=5b4447ade5066895212d3ac261f7a818; " +  // ← 替换这里
                "SESSDATA=3b6b8186%2C1754353318%2C49212%2A22CjA3xv2ccexHaxkhGiLoxDsOh3rAcVcm2gTqrTlWi3BuqfQXukhABO4_fXIR8m5mA-gSVnZjd1R5STI3dHRfcEE4bEJQa19ERTJ6WTBmSVh0Z2xjX0dlNWJLYlAxc3pueExDbzlEN1RRUlJOYzY1UEdwdDRJSWxWMi1zdkY4Y0J0SVBrY2JKbG13IIEC";    // ← 替换这里

        String res =  HttpUtil.createGet(apiUrl)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Cookie", cookie.toString())
                .execute().body();
//        System.out.println(res);
        return res;
    }

    //登录测试
    @GetMapping("/login_url")
    public String loginTest() {
        String apiUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
        return HttpUtil.createGet(apiUrl)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .execute().body();
    }

    private Map<String, String> cookiesStore = new HashMap<>();

    @GetMapping("/login_check")
    public String loginCheck(@RequestParam String qrcode_key) throws IOException {
        String apiUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";
        HttpResponse response = HttpUtil.createGet(apiUrl)
                .header("Referer", "https://www.bilibili.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .form("qrcode_key", qrcode_key)
                .execute();

        System.out.println(response);

        String responseBody = response.body();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode dataNode = rootNode.path("data");
        int code = dataNode.path("code").asInt();

        if (code == 0 && !dataNode.isMissingNode()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("cookies.txt"))) {
                for (String cookie : response.headers().get("Set-Cookie")) {
                    writer.write(cookie + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return responseBody;
    }

}
