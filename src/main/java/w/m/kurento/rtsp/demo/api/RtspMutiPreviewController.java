package w.m.kurento.rtsp.demo.api;

import org.kurento.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import w.m.kurento.rtsp.demo.service.KurentoService;
import w.m.kurento.rtsp.demo.service.MediaSession;

import javax.inject.Inject;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 浙江卓锐科技股份有限公司 版权所有 © Copyright 2018<br>
 * 说明: <br>
 * 项目名称: rtsp-demo <br>
 * 创建日期: 2018年01月17日 11:15 <br>
 * 作者: <a href="6492178@gmail.com">汪萌萌</a>
 */

/**
 * The type Rtsp muti preview controller.
 */
@Controller
@MessageMapping(RtspMutiPreviewController.NSP)
public class RtspMutiPreviewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RtspMutiPreviewController.class);

    /**
     * The constant NSP.
     */
    public static final String NSP = "/rtsp/preview";

    @Inject
    private KurentoService kurento;

    private Map<String, MediaPipeline> mediaPipelines = new ConcurrentHashMap<>();

    /**
     * On ice candidate.
     *
     * @param ch        the ch
     * @param candidate the candidate
     * @param session   the session
     */
    @MessageMapping("/{ch}/icecandidate")
    public void onIceCandidate(@DestinationVariable String ch, IceCandidate candidate, @Header MediaSession session) {
        WebRtcEndpoint endpoint = (WebRtcEndpoint) session.getEndpoint();
        session.addIceCandidate(candidate);
        return;
    }

    /**
     * On stop.
     *
     * @param ch      the ch
     * @param session the session
     */
    @MessageMapping("/{ch}/stop")
    public void onStop(@DestinationVariable String ch, @Header MediaSession session) {
            Endpoint endpoint = session.getEndpoint();
        // 获取连接到当前端点SINK的那些连接，注意，PlayerEndpoint会创建三个连接过来，分别用于AUDIO、VEDIO、DATA
        endpoint.getSourceConnections().forEach(data -> {
            MediaElement source = data.getSource();
            MediaElement sink = data.getSink();
            source.disconnect(sink);
        });
        endpoint.release();

    }

    /**
     * On sdp offer.
     *
     * @param ch        the ch
     * @param sdpoffer  the sdpoffer
     * @param principal the principal
     * @param session   the session
     */
    @MessageMapping("/{ch}/sdpoffer")
    public void onSdpOffer(@DestinationVariable String ch, String sdpoffer, Principal principal, @Header MediaSession session) {
        // 媒体管线现在不是当前会话独占了，而是每个通道一个
        PlayerEndpoint playerEndpoint = getPlayerEndpoint(ch);
        MediaPipeline pipeline = playerEndpoint.getMediaPipeline();
        session.setPipeline(pipeline);

        WebRtcEndpoint webRtcEndpoint = kurento.createWebRtcEndpoint(pipeline, sdpoffer, principal.getName(), NSP + '/' + ch);
        session.setEndpoint(webRtcEndpoint);

        playerEndpoint.connect(webRtcEndpoint);
    }

    private synchronized PlayerEndpoint getPlayerEndpoint(String ch) {
        MediaPipeline pipeline = getMediaPipline(ch);
        if (pipeline == null) {
            pipeline = kurento.createMediaPipeline();
            mediaPipelines.put(ch, pipeline);
            PlayerEndpoint.Builder peb = new PlayerEndpoint.Builder(pipeline, getRtspUrlFor(ch));
            PlayerEndpoint playerEndpoint = peb.build();
            playerEndpoint.addMediaFlowInStateChangeListener(e -> {
                LOGGER.info("RTSP input flow state changed, media type: {}, media state: {}", e.getMediaType(), e.getState());
            });
            playerEndpoint.play();
            return playerEndpoint;
        } else {
            PlayerEndpoint playerEndpoint = null;
            for (MediaObject mo : pipeline.getChildren()) {
                if (mo instanceof PlayerEndpoint) {
                    playerEndpoint = (PlayerEndpoint) mo;
                }
            }
            return playerEndpoint;
        }
    }

    private MediaPipeline getMediaPipline(String ch) {
        return mediaPipelines.get(ch);
    }



    private String getRtspUrlFor(String ch) {
        return "rtsp://admin:xxsd12345@192.168.16.164";
    }
}
