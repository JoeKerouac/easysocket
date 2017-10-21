package com.joe.easysocket.server.data;


/**
 * 数据报，用户不能直接构建，可以通过DatagramUtil构建<br/>
 * socket通讯发送的数据，该数据为最小数据，不能再分<br/>
 * <p>
 * 数据报head为固定长度16个字节<br/>
 * 第一个字节为版本号<br/>
 * 第二到第五个字节为数据报长度（请求体的长度，不包含请求头）<br/>
 * 第六个字节为数据报数据类型<br/>
 * 第七到第十六字节为数据报编码<br/>
 * <p>
 * 数据报body为变长，长度等于数据报head里边的数据报长度<br/>
 * <p>
 *
 * @author joe
 */
@lombok.Data
public class Datagram {
    // 数据报的最大长度，包含请求头和请求体
    public static final int MAX_LENGTH = Integer.MAX_VALUE;
    // 存放数据报数据，包含头信息，只读信息，只要创建出来后就无法更改
    private final byte[] data;
    // 该长度不包含头信息的长度，只有body的长度
    private final int size;
    // 数据报版本
    private final byte version;
    // 数据报body的编码
    private final String charset;
    // 数据报body
    private final byte[] body;
    // 数据报数据类型（0：心跳包；1：内置MVC数据处理器数据类型；2：文件传输；除了0和1外可以自己定义数据类型）
    private final byte type;

    /**
     * 初始化数据报
     *
     * @param data    包含头信息的data
     * @param size    该长度不包含头信息的长度，只有body的长度
     * @param body    数据报数据实体类
     * @param version 数据报版本号
     * @param charset 字符集
     * @param type    数据报数据类型（1：接口请求）
     */
    public Datagram(byte[] data, int size, byte[] body, byte version, String charset, byte type) {
        this.data = data;
        this.size = size;
        this.body = body;
        this.version = version;
        this.charset = charset;
        this.type = type;
    }
}