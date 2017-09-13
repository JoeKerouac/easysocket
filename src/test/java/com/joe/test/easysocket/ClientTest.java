package com.joe.test.easysocket;

import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.ext.dataworker.mvc.data.InterfaceData;
import com.joe.parse.json.JsonParser;
import com.joe.test.easysocket.client.Client;
import com.joe.test.easysocket.client.EventListenerAdapter;
import com.joe.test.easysocket.ext.Logger;
import com.joe.test.easysocket.ext.Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author joe
 */
public class ClientTest {
    private static final JsonParser parser = JsonParser.getInstance();

    public static void main(String[] args) throws Exception {

        //客户端设置心跳周期为50秒，服务端的设置为了30秒，所以服务端会检测到心跳超时最终关闭该客户端
        Client client = Client.builder().host("127.0.0.1").port(10051).listener(new EventListenerAdapter() {
            @Override
            public void receive(InterfaceData data) {
                System.out.println(data);
            }

            @Override
            public void faild(Throwable cause) {
                cause.printStackTrace();
            }

            @Override
            public void register(Client socket) {
                System.out.println("register");
            }

            @Override
            public void reconnect(Client socket) {
                socket.write("user/register", parser.toJson(new User("oppenid", "account", "password")));
                System.out.println("reconnect");
            }

            @Override
            public void unregister() {
                System.out.println("unregister");
            }
        }).logger(new Logger() {
            @Override
            public void debug(String msg) {
                System.out.println(msg);
            }

            @Override
            public void info(String msg) {
                System.out.println(msg);
            }

            @Override
            public void warn(String msg) {
                System.out.println(msg);
            }

            @Override
            public void error(String msg) {
                System.out.println(msg);
            }

            @Override
            public void debug(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void info(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void warn(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }

            @Override
            public void error(String flag, String msg) {
                System.out.println(flag + ":" + msg);
            }
        }).serializer(new Serializer() {
            @Override
            public byte[] write(Object obj) {
                return parser.toJson(obj).getBytes();
            }

            @Override
            public <T> T read(byte[] data , Class<T> clazz) {
                return parser.readAsObject(data , clazz);
            }
        }).heartbeat(50).build();
        client.start();
        client.write("user/register", parser.toJson(new User("oppenid", "account", "password")));
        //以下两个请求只有invoke不同，结果相同，也就是最前边可以以/开头，也可以不以/开头
        //一下请求的是服务端测试接口
//        client.write("user/login", parser.write(new User("oppenid", "account", "password")));
//        client.write(build("/user/login", parser.write(new User("oppenid", "account", "password"))));
    }

    /**
     * 构建接口请求数据报
     *
     * @param invoke 接口名称
     * @param data   接口数据
     * @return 接口请求需要的数据报
     */
    private static byte[] build(String invoke, String data) {
        InterfaceData interfaceData = new InterfaceData(String.valueOf(System.currentTimeMillis()), invoke, data);
        return DatagramUtil.build(parser.toJson(interfaceData).getBytes(), (byte) 1, (byte) 1).getData();
    }

    @Data
    @AllArgsConstructor
    private static class User {
        private String oppenid;
        private String account;
        private String password;
    }
}
