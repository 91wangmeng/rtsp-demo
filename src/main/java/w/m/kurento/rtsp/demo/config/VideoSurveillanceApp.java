package w.m.kurento.rtsp.demo.config;

import org.kurento.client.KurentoClient;
import org.kurento.client.KurentoClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import sun.security.acl.PrincipalImpl;
import w.m.kurento.rtsp.demo.service.MediaSession;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
public class VideoSurveillanceApp extends AbstractWebSocketMessageBrokerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoSurveillanceApp.class);

    private Map<String, MediaSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 信号处理在 /signal下进行
        registry.addEndpoint("/signal").setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptorAdapter() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                String simpSessionId = (String) accessor.getHeader("simpSessionId");
                MediaSession session = getMediaSession(simpSessionId);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 设置当前用户身份
                    String login = accessor.getNativeHeader("login").get(0);
                    Principal principal = new PrincipalImpl(login);
                    accessor.setUser(principal);
                    session.setPrincipal(principal);
                    LOGGER.info("User {} connected with session id {}", login, simpSessionId);
                }
                // 每次处理消息之前，设置session头，便于消息处理方法注入之
                accessor.setHeader("session", session);
                return message;
            }
        });
    }

    private MediaSession getMediaSession(String simpSessionId) {
        if (sessions.containsKey(simpSessionId)) {
            return sessions.get(simpSessionId);
        } else {
            MediaSession session = new MediaSession(simpSessionId);
            sessions.put(simpSessionId, session);
            return session;
        }
    }

    @Bean
    public KurentoClient kurentoClient() {
        return new KurentoClientBuilder().setKmsWsUri("ws://192.168.201.128:8888/kurento").connect();
    }

    public static void main(String[] args) {
        new SpringApplication(VideoSurveillanceApp.class).run(args);
    }
}