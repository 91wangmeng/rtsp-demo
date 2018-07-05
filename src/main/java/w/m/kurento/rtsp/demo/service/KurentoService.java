package w.m.kurento.rtsp.demo.service;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * 浙江卓锐科技股份有限公司 版权所有 © Copyright 2018<br>
 * 说明: <br>
 * 项目名称: rtsp-demo <br>
 * 创建日期: 2018年01月17日 10:07 <br>
 * 作者: <a href="6492178@gmail.com">汪萌萌</a>
 */
@Service
public class KurentoService {

    @Inject
    private KurentoClient client;

    @Inject
    private SimpMessagingTemplate template;

    /**
     * 初始化一个媒体管线
     *
     * @return
     */
    public MediaPipeline createMediaPipeline() {
        return client.createMediaPipeline();
    }

    /**
     * 在媒体管线上创建一个与WebRTC浏览器客户端通信的端点
     *
     * @param pipeline 管线
     * @param sdpoffer 浏览器发送来的SDP邀请
     * @param user     浏览器的身份
     * @return 运行在KMS中的WebRTC端点
     */
    public WebRtcEndpoint createWebRtcEndpoint(MediaPipeline pipeline, String sdpoffer, String user, String namespace) {
        WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
        // 处理SDP
        String sdpAnswer = webRtcEndpoint.processOffer(sdpoffer);
        template.convertAndSendToUser(user, namespace + "/sdpanswer", sdpAnswer);
        // 处理ICE候选
        webRtcEndpoint.addIceCandidateFoundListener(event -> {
            String dest = namespace + "/icecandidate";
            template.convertAndSendToUser(user, dest, event.getCandidate());
        });
        webRtcEndpoint.gatherCandidates();
        return webRtcEndpoint;
    }
}
