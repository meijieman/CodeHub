package com.geek.ngrok;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


class SockInfo {
    int type;//1远程连接,2本地连接,3代理连接
    SslInfo sinfo;
    SelectionKey tokey;//对方的连接
    int forward;//转发标记,代理连接专用，
    byte[] buf;
    int buflen;
}

public class NgrokClient {
    String ver = "ngrok-java v1.4(2018/8/21)";
    String serveraddr = "tunnel.qydev.com";
    int serverport = 4443;
    public String ClientId = "";
    public String localhost = "127.0.0.1";
    public int localport = 80;
    public String protocol = "http";
    public long lasttime = 0;
    public long pingtime = 0;
    public long reconnecttime = 0;
    public String authtoken = "";
    public List<HashMap<String, String>> tunnels = new ArrayList<HashMap<String, String>>();
    public NioSSLProvider ssl;
    Selector selector;
    public HashMap<String, HashMap<String, String>> tunnelinfos = new HashMap<String, HashMap<String, String>>();
    public SelectionKey mainkey;
    public boolean run = true;

    MsgOn msg = new MsgOn(NgrokClient.this);
    MsgSend msgSend = new MsgSend(NgrokClient.this);

    public NgrokClient(String serveraddr, int serverport, String authtoken, Boolean debug) {
        this.serveraddr = serveraddr;
        this.serverport = serverport;
        Log.isdebug = debug;
        init();
    }

    public NgrokClient() {
        init();
    }

    public void init() {
        // create the worker threads
        final Executor ioWorker = Executors.newSingleThreadExecutor();
        final Executor taskWorkers = Executors.newFixedThreadPool(1);//Executors.newSingleThreadExecutor();//Executors.newFixedThreadPool(1);


        final int ioBufferSize = 32 * 1024;
        ssl = new NioSSLProvider(ioBufferSize, ioWorker, taskWorkers) {
            @Override
            public void onFailure(SelectionKey key, Exception ex) {
                System.out.println("handshake failure");
                ex.printStackTrace();
                freeSock(key);//回收内存
            }

            @Override
            public void onSuccess(SelectionKey key) {
                SockInfo sockinfo = (SockInfo) key.attachment();
                //控制连接，直接登录认证
                if (sockinfo.type == 1) {
                    //ssl认证成功.ngrokr认证
                    msgSend.SendAuth("", authtoken, key);
                }
                //代理连接
                if (sockinfo.type == 3) {
                    //非控制连接，注册端口
                    msgSend.SendRegProxy(ClientId, key);
                }
            }

            @Override
            public void onInput(SelectionKey key, ByteBuffer decrypted) {
                SockInfo sockinfo = (SockInfo) key.attachment();
                //代理连接，转发模式
                if (sockinfo.type == 3 && sockinfo.forward == 1) {
                    RemoteToLocal(key, decrypted, sockinfo.tokey);
                } else {
                    // 监听
                    msg.unpack(key, decrypted);
                }
            }

            @Override
            public void onClosed(SelectionKey key) {
                freeSock(key);//回收内存
                System.out.println("ssl session closed");
            }

        };
    }

    public void reconnect() {
        long ctime = System.currentTimeMillis() / 1000;
        lasttime = 0;
        reconnecttime = ctime;
        try {
            selector = Selector.open();
        } catch (IOException e1) {
        }
        ssl.clearBuf();//清空一些缓存
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }
        SockInfo msockinfo = new SockInfo();
        msockinfo.type = 1;
        //连接到服务器
        mainkey = connect(serveraddr, serverport, false, msockinfo);
    }


    public SSLEngine NewEngine(String peerHost, int peerPort) {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {

            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {

            }
        }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLEngine engine = sc.createSSLEngine(peerHost, peerPort);
            //SSLEngine engine = sc.createSSLEngine();
            engine.setUseClientMode(true);
            return engine;
        } catch (Exception e) {
        }
        return null;
    }

    public void start() {
        System.out.println(ver);
        // NIO selector
        while (run) {
            long ctime = System.currentTimeMillis() / 1000;
            //检查断线
            if ((lasttime > 0 && lasttime + 60 < ctime && reconnecttime + 65 < ctime) || (lasttime == 0 && reconnecttime + 65 < ctime)) {
                reconnect();
                continue;
            }


            //定时心跳
            if (lasttime > 0) {
                if ((lasttime + 25 < ctime) && pingtime + 15 < ctime) {
                    msgSend.SendPing(mainkey);
                    pingtime = ctime;
                }
            }
            //为空睡毫秒,避免cpu过高
            if (selector == null || selector.keys().isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                continue;
            }


            try {
                selector.select(10);
                Iterator<SelectionKey> keySet = selector.selectedKeys().iterator();
                while (keySet.hasNext()) {
                    SelectionKey sKey = keySet.next();
                    keySet.remove();
                    SockInfo sockinfo = (SockInfo) sKey.attachment();
                    if (sockinfo != null && sKey.isValid()) {
                        if (sKey.isConnectable()) {
                            if (sockinfo.type == 1 || sockinfo.type == 3) {
                                if (ssl.bindEngine(sockinfo.sinfo)) {
                                    try {
                                        sKey.channel().register(selector, SelectionKey.OP_READ).attach(sockinfo);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                            if (sockinfo.type == 2) {

                                try {
                                    if (((SocketChannel) sKey.channel()).finishConnect()) {
                                        SockInfo remotesockinfo = (SockInfo) sockinfo.tokey.attachment();
                                        //如果有数据,清空
                                        if (remotesockinfo.buflen > 0) {
                                            ByteBuffer buf = ByteBuffer.allocate(remotesockinfo.buflen);
                                            buf.clear();
                                            buf.put(remotesockinfo.buf, 0, remotesockinfo.buflen);
                                            buf.flip();
                                            ((SocketChannel) sKey.channel()).write(buf);
                                            remotesockinfo.buflen = 0;//清空
                                        }
                                    }
                                    sKey.channel().register(selector, SelectionKey.OP_READ).attach(sockinfo);
                                } catch (Exception e) {
                                    freeSock(sKey);
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (sKey.isReadable()) {
                            if (sockinfo.type == 1 || sockinfo.type == 3) {
                                boolean falg = ssl.processInput(sockinfo.sinfo);
                                if (!falg) {
                                    //取消监听
                                    sKey.cancel();
                                }
                            }
                            if (sockinfo.type == 2) {
                                int len = LocalToRemote(sKey, sockinfo.tokey);
                                if (len == -1) {
                                    //取消监听
                                    sKey.cancel();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
    }


    public void close() {
        run = false;
    }

    public void addTun(String localhost, int localport, String Protocol, String Hostname, String Subdomain, int RemotePort, String HttpAuth) {
        HashMap<String, String> tunelInfo = new HashMap<>();
        tunelInfo.put("localhost", localhost);
        tunelInfo.put("localport", localport + "");
        tunelInfo.put("Protocol", Protocol);
        tunelInfo.put("Hostname", Hostname);
        tunelInfo.put("Subdomain", Subdomain);
        tunelInfo.put("HttpAuth", HttpAuth);
        tunelInfo.put("RemotePort", RemotePort + "");
        tunnels.add(tunelInfo);

    }

    /*本地的转发远程*/
    public int LocalToRemote(SelectionKey key, SelectionKey remoteKey) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(4096);
        try {
            int len = clientChannel.read(buf);
            buf.flip();
            if (len > 0) {
                SockInfo sockinfo = (SockInfo) remoteKey.attachment();
                ssl.sendAsync(sockinfo.sinfo, buf);
            }
            //关闭连接
            if (len == -1) {
                freeSock(key);//回收内存
            }
            return len;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /*远程数据转发到本地*/
    public void RemoteToLocal(SelectionKey localKey, ByteBuffer decrypted, SelectionKey remoteKey) {
        byte[] buffer = new byte[decrypted.remaining()];
        decrypted.get(buffer);
        ByteBuffer buf = ByteBuffer.allocate(4096);
        buf.clear();
        buf.put(buffer);
        buf.flip();
        SockInfo sockinfo = (SockInfo) localKey.attachment();
        try {

            SocketChannel remoteChannel = (SocketChannel) remoteKey.channel();
            if (remoteChannel.finishConnect()) {
                remoteChannel.write(buf);
            } else {
                //先存起来
                BytesUtil.myaddBytes(sockinfo.buf, sockinfo.buflen, buffer, buffer.length);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public SelectionKey connect(String host, int port, boolean block, SockInfo sockinfo) {
        SocketChannel channel;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(block);
            channel.setOption(java.net.StandardSocketOptions.TCP_NODELAY, true);
            InetSocketAddress addr = new InetSocketAddress(host, port);
            if (addr.getAddress() == null) {
                return null;
            }
            channel.connect(addr);
            SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
            if (sockinfo.type == 1 || sockinfo.type == 3) {
                SslInfo sinfo = new SslInfo();
                sinfo.key = key;
                sinfo.engine = NewEngine(host, port);
                sockinfo.sinfo = sinfo;
            }
            key.attach(sockinfo);
            return key;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    //回收内存
    public void freeSock(SelectionKey key) {
        SockInfo sockinfo = (SockInfo) key.attachment();
        if (sockinfo != null) {

            //本地连接
            if (sockinfo.type == 2) {
                try {
                    ((SocketChannel) key.channel()).socket().close();
                    if (sockinfo.tokey != null) {
                        ((SocketChannel) sockinfo.tokey.channel()).socket().close();
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                }
                //sockinfo.tokey.close();//remoteKey.close();//关闭远程
            }


            //远程的关闭本地的
            if (sockinfo.type == 3) {

                if (sockinfo.tokey != null) {
                    try {
                        ((SocketChannel) sockinfo.tokey.channel()).socket().close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }
                }
            }
        }
    }


}
