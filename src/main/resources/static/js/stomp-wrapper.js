class StompClient {
    /**
     * 选项：
     * url，WebSocket连接地址
     * namespace，不包含/app、/user的目的地前缀
     * login，用户名
     * passcode，密码
     */
    constructor( options ) {
        this.namespace = options.namespace || {};
        this.pending = [];
        this.stomp = Stomp.over( new WebSocket( options.url ) );
        this.stomp.heartbeat.outgoing = 20000;
        this.stomp.connect( options.login, options.passcode, ( frame ) => {
            this.connected = true;
        this.processPending();
    } );
    }

    processPending() {
        if ( this.connected ) {
            let pending = this.pending;
            this.pending = [];
            pending.forEach( callback => callback() );
        }
    }

    recv( destination, callback ) {
        this.pending.push( () => {
            this.stomp.subscribe( '/user' + this.namespace + destination, ( frame ) => {
            callback( this.decode( frame.body, frame.headers[ 'content-type' ] ), frame );
    } );
    } );
        this.processPending();
    }

    encode( obj ) {
        return JSON.stringify( obj );
    }

    decode( str, mimeType ) {
        // 自动分析MIME类型，进行适当的解析
        if ( mimeType.startsWith( 'application/json;' ) ) {
            return JSON.parse( str );
        }
        else {
            return str;
        }
    }

    send( destination, object ) {
        this.pending.push( () => {
            this.stomp.send( '/app' + this.namespace + destination, {
            "content-type": "application/json;charset=UTF-8"
        }, this.encode( object ) );
    } );
        this.processPending();
    }

    disconnect() {
        this.stomp.disconnect();
    }
}