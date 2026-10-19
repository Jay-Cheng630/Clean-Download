package com.cleanhub;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;

// 工具类，负责网络和算哈希
public class DownloadUtil {

    // 计算SHA256，网上抄的CSDN的代码
    public static String getHash(Path f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(f)) {
            byte[] buf = new byte[8192]; 
            int len;
            while ((len = is.read(buf)) != -1) {
                md.update(buf, 0, len);
            }
        }
        // 把byte转成16进制字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
           sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 去找github的下载链接
    public static String findGitUrl(String repo) {
        try {
            HttpClient c = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                .header("User-Agent", "CleanHub")
                .GET().build();

            HttpResponse<String> res = c.send(req, HttpResponse.BodyHandlers.ofString());
            
            // 只要状态码200就说明请求成功了
            if (res.statusCode() == 200) {
                String text = res.body();
                // 鼓捣找
                int i = text.indexOf("browser_download_url");
                while (i > 0) {
                    int start = text.indexOf("\"", i + 22) + 1;
                    int end = text.indexOf("\"", start);
                    if (start > 0 && end > start) {
                        String u = text.substring(start, end);
                        // 必须是以exe或者msi结尾的，不然不要
                        if (u.endsWith(".exe") || u.endsWith(".msi")) {
                            return u;
                        }
                    }
                    i = text.indexOf("browser_download_url", i + 1);
                }
            }
        } catch (Exception e) {
            System.out.println("请求失败了: " + e.getMessage());
            // e.printStackTrace(); 这句不开了，控制台看着烦
        }
        return null;
    }
}