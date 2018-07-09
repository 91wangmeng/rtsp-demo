/**
 * Copyright (C), 2018, 浙江力石科技有限公司
 * FileName: VideoConfig
 * Author:   YHY
 * Date:     2018/7/6 14:45
 * Description:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package w.m.kurento.rtsp.demo.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;


/**
 * 〈一句话功能简述〉<br> 
 * 〈〉
 *
 * @author yhy
 * @create 2018/7/6
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "videos")
public class VideoConfig {
    private Map<String,Map<String,String>> hjxzVodeos = new HashMap<>();

    public Map<String, Map<String, String>> getHjxzVodeos() {
        return hjxzVodeos;
    }

    public void setHjxzVodeos(Map<String, Map<String, String>> hjxzVodeos) {
        this.hjxzVodeos = hjxzVodeos;
    }
}