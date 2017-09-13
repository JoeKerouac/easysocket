package com.joe.test.easysocket;

import com.joe.easysocket.server.Server;
import com.joe.easysocket.server.data.ProtocolData;
import com.joe.easysocket.server.ext.*;
import com.joe.easysocket.server.ext.mvc.MvcDataworker;
import com.joe.easysocket.server.ext.mvc.container.Provider;
import com.joe.easysocket.server.ext.mvc.context.RequestContext;
import com.joe.easysocket.server.ext.mvc.context.ResponseContext;
import com.joe.easysocket.server.ext.mvc.context.session.Session;
import com.joe.easysocket.server.ext.mvc.filter.NioFilter;
import com.joe.easysocket.server.ext.mvc.filter.NioRequestFilter;
import com.joe.easysocket.server.ext.mvc.filter.NioResponseFilter;
import com.joe.easysocket.server.ext.mvc.param.Context;
import com.joe.easysocket.server.ext.mvc.param.GeneralParam;
import com.joe.easysocket.server.ext.mvc.resource.annotation.Path;
import com.joe.easysocket.server.protocol.ServerConfig;
import com.joe.parse.json.JsonParser;
import com.joe.utils.IOUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试
 *
 * @author joe
 */
public class ServerTest {
    public static void main(String[] args) throws Exception {
        System.out.println("准备启动系统");
        PublishCenter publishCenter = new DefaultPublishCenter();
        CustomDeque<ProtocolData> deque = new CustomDequeImpl<>();
        Server server = Server.buildDefault(ServerConfig.builder().heartbeat(30).port(10051).publishCenter
                (publishCenter).deque
                (deque).build());

        server.start(() -> {
            System.out.println("系统关闭");
        });

        //MVC数据处理器可以单独启动
        DataWorker dataworker = new MvcDataworker(MvcDataworker.MvcDataworkerConfig.builder().publishCenter
                (publishCenter).deque(deque).build());
        dataworker.start("MVC数据处理器", () -> {
            System.out.println("系统关闭");
        });
        System.out.println("系统启动成功");
    }

    @Provider(priority = 100)
    private static class FilterTest implements NioFilter {
        @Override
        public void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) {
            System.out.println("\n\n");
            System.out.println("权重为100的响应filter被调用了");
            System.out.println("\n\n");
        }

        @Override
        public void requestFilter(RequestContext.RequestWrapper request) {
            System.out.println("\n\n");
            System.out.println("权重为100的请求filter被调用了");
            System.out.println("\n\n");
        }
    }

    @Provider(priority = 1000)
    private static class FilterTest1 extends NioResponseFilter {

        @Override
        public void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) {
            System.out.println("\n\n");
            System.out.println("权重为1000的filter被调用了");
            try {
                //测试读流的方式和直接获取entity的方式
                System.out.println(IOUtils.read(request.getInputStream(), request.getCharset()));
                System.out.println(JsonParser.getInstance().toJson(request.getEntity()));
                System.out.println("结果为：" + response.getResult());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("\n\n");
        }
    }

    @Provider(priority = 1000)
    private static class FilterTest2 extends NioRequestFilter {

        @Override
        public void requestFilter(RequestContext.RequestWrapper request) {
            System.out.println("\n\n");
            System.out.println("权重为1000的请求filter被调用了");
            try {
                //测试读流的方式和直接获取entity的方式
                System.out.println(IOUtils.read(request.getInputStream(), request.getCharset()));
                System.out.println(JsonParser.getInstance().toJson(request.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("\n\n");
        }
    }

    @Path("user")
    private static class UserResource {
        private static AtomicInteger count = new AtomicInteger(0);

        @Path("login")
        public User login(@GeneralParam("openid") String openid, @GeneralParam("account") @NotNull String account,
                          @GeneralParam("password") String password, @Context Session session) {
            System.out.println("oppenid = [" + openid + "], account = [" + account + "], password = [" + password +
                    "], session = [" + session + "]");
            return new User(openid, account, password);
        }

        //只有一个参数的时候
        @Path("register")
        public User register(User user) {
            System.out.println("注册用户为：" + user);
            return user;
        }

        @Data
        @AllArgsConstructor
        private static class User {
            private String openid;
            private String account;
            private String password;
        }
    }
}
