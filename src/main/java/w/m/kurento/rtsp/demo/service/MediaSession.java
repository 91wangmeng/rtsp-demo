package w.m.kurento.rtsp.demo.service;

import org.kurento.client.Endpoint;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

public class MediaSession {

    private String id;

    private Principal principal;

    private MediaPipeline pipeline;

    private WebRtcEndpoint endpoint;

    private List<IceCandidate> candidatesPending;

    public MediaSession(String id) {
        this.id = id;
        candidatesPending = new ArrayList<>();
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(MediaPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public synchronized void setEndpoint(WebRtcEndpoint endpoint) {
        this.endpoint = endpoint;
        // ICE可能在端点创建之前就送达
        if (candidatesPending != null) {
            candidatesPending.forEach(cp -> {
                endpoint.addIceCandidate(cp);
            });
            candidatesPending = null;
        }
    }

    @Override
    public String toString() {
        return String.format("id = %s  ep = %s  pp = %s", getId(), getEndpoint(), getPipeline());
    }

    public String getId() {
        return id;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public synchronized void addIceCandidate(IceCandidate candidate) {
        // ICE可能在端点创建之前就送达
        if (endpoint == null) {
            candidatesPending.add(candidate);
        } else {
            endpoint.addIceCandidate(candidate);
        }
    }
}
