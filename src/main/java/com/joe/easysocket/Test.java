package com.joe.easysocket;

import com.joe.easysocket.data.ProtocolData;
import com.joe.easysocket.ext.*;
import com.joe.easysocket.ext.dataworker.mvc.MvcDataworker;
import com.joe.easysocket.ext.dataworker.mvc.container.Provider;
import com.joe.easysocket.ext.dataworker.mvc.context.RequestContext;
import com.joe.easysocket.ext.dataworker.mvc.context.ResponseContext;
import com.joe.easysocket.ext.dataworker.mvc.context.session.Session;
import com.joe.easysocket.ext.dataworker.mvc.filter.NioResponseFilter;
import com.joe.easysocket.ext.dataworker.mvc.param.Context;
import com.joe.easysocket.ext.dataworker.mvc.param.GeneralParam;
import com.joe.easysocket.ext.dataworker.mvc.resource.annotation.Path;
import com.joe.easysocket.protocol.ServerConfig;
import com.joe.parse.json.JsonParser;
import com.joe.utils.IOUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试
 *
 * @author joe
 */
public class Test {
    public static void main(String[] args) throws Exception {
        System.out.println("准备启动系统");
        PublishCenter publishCenter = new DefaultPublishCenter();
        CustomDeque<ProtocolData> deque = new CustomDequeImpl<>();
        Server server = Server.buildDefault(ServerConfig.builder().heartbeat(3000).port(10051).publishCenter(publishCenter).deque
                (deque).build());

        server.start(() -> {
            System.out.println("系统关闭");
        });

        //MVC数据处理器可以单独启动
        DataWorker dataworker = new MvcDataworker(MvcDataworker.MvcDataworkerConfig.builder().publishCenter
                (publishCenter).deque(deque).build());
        dataworker.start("MVC数据处理器" , () -> {
            System.out.println("系统关闭");
        });
        System.out.println("系统启动成功");
    }

    @Provider(priority = 100)
    private static class FilterTest2 extends NioResponseFilter{
        @Override
        public void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) {
            System.out.println(100);
        }
    }

    @Provider(priority = 1000)
    private static class  FilterTest extends NioResponseFilter {

        @Override
        public void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) {
            System.out.println(1000);
            try {
                System.out.println(IOUtils.read(request.getInputStream() , request.getCharset()));
                System.out.println("\n\n");
                System.out.println(JsonParser.getInstance().toJson(request.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Path("user")
    private static class UserResource {
        private static AtomicInteger count = new AtomicInteger(0);

        @Path("login")
        public void login(@GeneralParam("oppenid") String oppenid, @GeneralParam("account") @NotNull String account,
                                     @GeneralParam("password") String password, @Context Session session) {
            System.out.println(account);
            return ;
        }
    }
}
