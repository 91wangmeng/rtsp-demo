class WebRTCEndpoint {
    constructor( mode, options ) {
        /**
         * 选项：
         * remoteVideo，显示远程视频流的元素
         */
        options = options || {};
        let stomp = new StompClient( {
            url: options.url,
            namespace: options.namespace,
            login: options.login
        } );

        let webRtcPeerType;
        switch ( mode ) {
            case WebRTCEndpoint.MODE_SEND:
                webRtcPeerType = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly;
                break;
            case WebRTCEndpoint.MODE_RECV:
                webRtcPeerType = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly;
                break;
            case WebRTCEndpoint.MODE_SEND_RECV:
                webRtcPeerType = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv;
                break;
        }
        stomp.recv( '/icecandidate', candidate => {
            this.peer.addIceCandidate( candidate );
    } );
        stomp.recv( '/sdpanswer', answer => {
            this.peer.processAnswer( answer );
    } );
        options.onicecandidate = candidate => {
            stomp.send( '/icecandidate', candidate );
        }
        this.peer = webRtcPeerType( options, () => {
            this.peer.generateOffer( ( error, sdpOffer ) => {
            stomp.send( '/sdpoffer', sdpOffer );
    } );
    } );
        this.stomp = stomp;
    }

    dispose() {
        this.stomp.send( '/stop', "bye" );
        this.stomp.disconnect();
        this.peer && this.peer.dispose();
    }
}
WebRTCEndpoint.MODE_SEND = 0;
WebRTCEndpoint.MODE_RECV = 1;
WebRTCEndpoint.MODE_SEND_RECV = 2; 