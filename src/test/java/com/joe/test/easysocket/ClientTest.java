package com.joe.test.easysocket;

import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.ext.dataworker.mvc.data.InterfaceData;
import com.joe.parse.json.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.UnsupportedEncodingException;

/**
 * @author joe
 */
public class ClientTest {
    private static final JsonParser parser = JsonParser.getInstance();

    public static void main(String[] args) throws Exception {
        //客户端设置心跳周期为50秒，服务端的设置为了30秒，所以服务端会检测到心跳超时最终关闭该客户端
        Client client = Client.builder().host("127.0.0.1").port(10051).consumer(datagram -> {
            try {
                System.out.println(new String(datagram.getBody(), datagram.getCharset()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }).heartbeat(50).build();
        client.start();
        client.write(build("user/register", parser.toJson(new User("oppenid", "account", "password"))));
        //以下两个请求只有invoke不同，结果相同，也就是最前边可以以/开头，也可以不以/开头
        //一下请求的是服务端测试接口
        client.write(build("user/login", parser.toJson(new User("oppenid", "account", "password"))));
//        client.write(build("/user/login", parser.toJson(new User("oppenid", "account", "password"))));
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
