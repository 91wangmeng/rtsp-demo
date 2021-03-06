package w.m.kurento.rtsp.demo.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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
        JSONArray jsonArray = JSON.parseArray("[{\"name\":\"1#公厕北\",\"code\":\"001116\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"38\",\"videoType\":\"枪机\"},{\"name\":\"1#公厕弄堂\",\"code\":\"001118\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"37\",\"videoType\":\"枪机\"},{\"name\":\"100-4号北\",\"code\":\"001134\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"40\",\"videoType\":\"枪机\"},{\"name\":\"100-4号南\",\"code\":\"001153\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"33\",\"videoType\":\"枪机\"},{\"name\":\"100-9北\",\"code\":\"001211\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"39\",\"videoType\":\"枪机\"},{\"name\":\"100-9号南\",\"code\":\"001181\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"44\",\"videoType\":\"枪机\"},{\"name\":\"17号东\",\"code\":\"001213\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"40\",\"videoType\":\"枪机\"},{\"name\":\"17号西\",\"code\":\"001219\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"43\",\"videoType\":\"枪机\"},{\"name\":\"21号东\",\"code\":\"001205\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"36\",\"videoType\":\"枪机\"},{\"name\":\"22号河边\",\"code\":\"001110\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"41\",\"videoType\":\"枪机\"},{\"name\":\"22号弄堂\",\"code\":\"001114\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"39\",\"videoType\":\"枪机\"},{\"name\":\"22号西\",\"code\":\"001132\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"38\",\"videoType\":\"枪机\"},{\"name\":\"25号弄堂\",\"code\":\"001146\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"57\",\"videoType\":\"枪机\"},{\"name\":\"26号弄堂\",\"code\":\"001112\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"40\",\"videoType\":\"枪机\"},{\"name\":\"27-3弄堂\",\"code\":\"001221\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"44\",\"videoType\":\"枪机\"},{\"name\":\"31号西\",\"code\":\"001215\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"41\",\"videoType\":\"枪机\"},{\"name\":\"32号弄堂\",\"code\":\"001102\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"45\",\"videoType\":\"枪机\"},{\"name\":\"33号东\",\"code\":\"001191\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"59\",\"videoType\":\"枪机\"},{\"name\":\"38号河边东\",\"code\":\"001106\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"43\",\"videoType\":\"枪机\"},{\"name\":\"38号河边西\",\"code\":\"001108\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"42\",\"videoType\":\"枪机\"},{\"name\":\"39号弄堂\",\"code\":\"001225\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"46\",\"videoType\":\"枪机\"},{\"name\":\"4-1号东\",\"code\":\"001209\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"38\",\"videoType\":\"枪机\"},{\"name\":\"4-1号南\",\"code\":\"001138\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"45\",\"videoType\":\"枪机\"},{\"name\":\"40号弄堂\",\"code\":\"001148\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"60\",\"videoType\":\"枪机\"},{\"name\":\"41号东\",\"code\":\"001203\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"35\",\"videoType\":\"枪机\"},{\"name\":\"41号西\",\"code\":\"001217\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"42\",\"videoType\":\"枪机\"},{\"name\":\"49号弄堂\",\"code\":\"001104\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"44\",\"videoType\":\"枪机\"},{\"name\":\"54号弄堂\",\"code\":\"001100\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"46\",\"videoType\":\"枪机\"},{\"name\":\"54号西\",\"code\":\"001177\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"37\",\"videoType\":\"枪机\"},{\"name\":\"56号东\",\"code\":\"001157\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"39\",\"videoType\":\"枪机\"},{\"name\":\"65号东\",\"code\":\"001169\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"58\",\"videoType\":\"枪机\"},{\"name\":\"67号西\",\"code\":\"001163\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"49\",\"videoType\":\"枪机\"},{\"name\":\"76号西\",\"code\":\"001175\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"34\",\"videoType\":\"枪机\"},{\"name\":\"78号东\",\"code\":\"001130\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"35\",\"videoType\":\"枪机\"},{\"name\":\"8号箱边西\",\"code\":\"001239\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"53\",\"videoType\":\"枪机\"},{\"name\":\"8号箱东\",\"code\":\"001227\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"47\",\"videoType\":\"枪机\"},{\"name\":\"98号东\",\"code\":\"001187\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"53\",\"videoType\":\"枪机\"},{\"name\":\"98号西\",\"code\":\"001155\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"36\",\"videoType\":\"枪机\"},{\"name\":\"菜场码头\",\"code\":\"001223\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"45\",\"videoType\":\"枪机\"},{\"name\":\"出口人流统计\",\"code\":\"001253\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"60\",\"videoType\":\"枪机\"},{\"name\":\"出口人流统计\",\"code\":\"001256\",\"ip\":\"192.168.30.98\",\"port\":\"8000\",\"channelNumber\":\"1\",\"videoType\":\"枪机\"},{\"name\":\"大展厅休息区\",\"code\":\"001092\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"50\",\"videoType\":\"枪机\"},{\"name\":\"服务中心吧台\",\"code\":\"001086\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"53\",\"videoType\":\"枪机\"},{\"name\":\"故居健身器旁\",\"code\":\"001231\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"49\",\"videoType\":\"枪机\"},{\"name\":\"故居围墙\",\"code\":\"001237\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"52\",\"videoType\":\"枪机\"},{\"name\":\"进道弄堂\",\"code\":\"001167\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"55\",\"videoType\":\"枪机\"},{\"name\":\"进口大门\",\"code\":\"001122\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"35\",\"videoType\":\"枪机\"},{\"name\":\"进口东\",\"code\":\"001144\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"54\",\"videoType\":\"枪机\"},{\"name\":\"进口人流统计\",\"code\":\"001251\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"59\",\"videoType\":\"枪机\"},{\"name\":\"进口人流统计\",\"code\":\"001257\",\"ip\":\"192.168.30.99\",\"port\":\"8000\",\"channelNumber\":\"1\",\"videoType\":\"枪机\"},{\"name\":\"进口西\",\"code\":\"001201\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"34\",\"videoType\":\"枪机\"},{\"name\":\"陆家_1\",\"code\":\"001161\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"46\",\"videoType\":\"枪机\"},{\"name\":\"陆家_2\",\"code\":\"001199\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"33\",\"videoType\":\"枪机\"},{\"name\":\"陆家_3\",\"code\":\"001185\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"50\",\"videoType\":\"枪机\"},{\"name\":\"陆家楼4\",\"code\":\"001207\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"37\",\"videoType\":\"枪机\"},{\"name\":\"陆家楼6\",\"code\":\"001255\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"61\",\"videoType\":\"枪机\"},{\"name\":\"门口岗厅\",\"code\":\"001082\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"55\",\"videoType\":\"枪机\"},{\"name\":\"庙二进\",\"code\":\"001229\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"48\",\"videoType\":\"枪机\"},{\"name\":\"庙光场\",\"code\":\"001245\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"56\",\"videoType\":\"枪机\"},{\"name\":\"庙门口东\",\"code\":\"001235\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"51\",\"videoType\":\"枪机\"},{\"name\":\"庙一进\",\"code\":\"001249\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"58\",\"videoType\":\"枪机\"},{\"name\":\"四方弄1\",\"code\":\"001189\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"56\",\"videoType\":\"枪机\"},{\"name\":\"四方弄2\",\"code\":\"001159\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"43\",\"videoType\":\"枪机\"},{\"name\":\"四方弄3\",\"code\":\"001150\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"63\",\"videoType\":\"枪机\"},{\"name\":\"四方弄4\",\"code\":\"001165\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"52\",\"videoType\":\"枪机\"},{\"name\":\"四方弄5\",\"code\":\"001247\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"57\",\"videoType\":\"枪机\"},{\"name\":\"四方弄6\",\"code\":\"001233\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"50\",\"videoType\":\"枪机\"},{\"name\":\"停车场\",\"code\":\"001124\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"34\",\"videoType\":\"枪机\"},{\"name\":\"停车场大门\",\"code\":\"001126\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"33\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺北\",\"code\":\"001098\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"47\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺二进\",\"code\":\"001136\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"42\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺二进过道\",\"code\":\"001142\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"51\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺三进\",\"code\":\"001140\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"48\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺三进走廊\",\"code\":\"001183\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"47\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺一进\",\"code\":\"001096\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"48\",\"videoType\":\"枪机\"},{\"name\":\"同泰当铺一进通道\",\"code\":\"001179\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"41\",\"videoType\":\"枪机\"},{\"name\":\"铜像\",\"code\":\"001120\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"36\",\"videoType\":\"枪机\"},{\"name\":\"洗手池\",\"code\":\"001088\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"52\",\"videoType\":\"枪机\"},{\"name\":\"小展厅1\",\"code\":\"001094\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"49\",\"videoType\":\"枪机\"},{\"name\":\"小展厅2\",\"code\":\"001090\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"51\",\"videoType\":\"枪机\"},{\"name\":\"新桥东\",\"code\":\"001243\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"55\",\"videoType\":\"枪机\"},{\"name\":\"新桥南\",\"code\":\"001193\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"62\",\"videoType\":\"枪机\"},{\"name\":\"新桥西\",\"code\":\"001241\",\"ip\":\"192.168.30.13\",\"port\":\"8000\",\"channelNumber\":\"54\",\"videoType\":\"枪机\"},{\"name\":\"云集昌记南\",\"code\":\"001171\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"61\",\"videoType\":\"枪机\"},{\"name\":\"云集昌记西\",\"code\":\"001173\",\"ip\":\"192.168.30.12\",\"port\":\"8000\",\"channelNumber\":\"64\",\"videoType\":\"枪机\"},{\"name\":\"展厅门口\",\"code\":\"001084\",\"ip\":\"192.168.30.11\",\"port\":\"8000\",\"channelNumber\":\"54\",\"videoType\":\"枪机\"}]");
        Map<String, JSONObject> code = jsonArray.stream().map(o -> JSON.parseObject(JSON.toJSONString(o)))
                .collect(Collectors.toMap(o -> o.getString("code"), Function.identity()));
        JSONObject jsonObject = code.get(ch);
        System.out.println(jsonObject.toJSONString());
        return "rtsp://admin:admin123456@192.168.1.42/h264/ch1/sub/av_stream";
    }
}
