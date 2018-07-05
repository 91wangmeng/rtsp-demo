package w.m.kurento.rtsp.demo.api;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import w.m.kurento.rtsp.demo.service.KurentoService;
import w.m.kurento.rtsp.demo.service.MediaSession;

import javax.inject.Inject;
import java.security.Principal;

/**
 * 浙江卓锐科技股份有限公司 版权所有 © Copyright 2018<br>
 * 说明: <br>
 * 项目名称: rtsp-demo <br>
 * 创建日期: 2018年01月17日 10:02 <br>
 * 作者: <a href="6492178@gmail.com">汪萌萌</a>
 */
@Controller
@MessageMapping( RtspPreviewController.NAMESPACE )
public class RtspPreviewController {

    private static final Logger LOGGER = LoggerFactory.getLogger( RtspPreviewController.class );

    public static final String NAMESPACE = "/rtsp/preview";

    @Inject
    private KurentoService kurento;

    @MessageMapping( "/icecandidate" )
    public void onIceCandidate(IceCandidate candidate, @Header MediaSession session ) {
        session.addIceCandidate( candidate );
        return;
    }

    @MessageMapping( "/stop" )
    public void onStop( @Header MediaSession session ) {
        session.getEndpoint().release();
        session.getPipeline().release();
    }

    @MessageMapping( "/sdpoffer" )
    public void onSdpOffer(String sdpoffer, Principal principal, @Header MediaSession session) {
        MediaPipeline pipeline = kurento.createMediaPipeline();
        session.setPipeline( pipeline );

//        PlayerEndpoint.Builder peb = new PlayerEndpoint.Builder( pipeline, "rtsp://admin:1111@192.168.10.6/h264/ch1/sub/av_stream" );
        PlayerEndpoint.Builder peb = new PlayerEndpoint.Builder( pipeline, "rtsp://admin:abcd1234@192.168.30.11/h264/ch1/sub/av_stream" );
        PlayerEndpoint playerEndpoint = peb.build();
        playerEndpoint.addMediaFlowInStateChangeListener( e -> LOGGER.info( "RTSP input flow state changed, media type: {}, media state: {}", e.getMediaType(), e.getState() ));

        WebRtcEndpoint webRtcEndpoint = kurento.createWebRtcEndpoint( pipeline, sdpoffer, principal.getName(), NAMESPACE );
        session.setEndpoint( webRtcEndpoint );
        playerEndpoint.connect( webRtcEndpoint );
        playerEndpoint.play();
    }
}
