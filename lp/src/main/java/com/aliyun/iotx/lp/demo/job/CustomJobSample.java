package com.aliyun.iotx.lp.demo.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.api.InitResult;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.IoTMqttClientConfig;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tools.AError;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;

/**
 * @author yaosen.pys
 * @date 2021/03/23
 */
public class CustomJobSample {

    /**
     * 产品productKey，待补充
     */
    private static String productKey = "";
    /**
     * 设备名称，待补充
     */
    private static String deviceName = "";

    /**
     * 设备deviceSecret，待补充
     */
    private static String deviceSecret = "";

    /**
     * 区域信息，产品&设备所在region，待补充
     */
    private static String region = "";

    /**
     * 系统启动参数
     */
    private static Boolean initSuccess = false;
    private static Boolean subscribeNotify = false;
    private static Boolean subscribeGetReply = false;
    private static Boolean subscribeUpdateReply = false;

    /**
     * 命令行解析
     */
    private static Options updateOptions;
    private static Options getOptions;

    public static void main(String[] args) {
        // 初始化命令行解析
        initOptions();

        // 初始化设备启动，订阅相关topic
        init(productKey, deviceName, deviceSecret, region);

        // 检查启动初始化状态
        checkInitStatus();
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("enter your command:");
            String command = input.nextLine();
            if (StringUtils.equals(command, "exit")) {
                // 退出程序
                break;
            } else if (StringUtils.startsWith(command, "update")) {
                // 更新task状态
                try {
                    //解析入参
                    String[] params = parseParams(command);
                    CommandLineParser parser = new PosixParser();
                    CommandLine cmd = parser.parse(updateOptions, params);

                    if (cmd.hasOption("h")) {
                        // help命令
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("update", updateOptions);
                    } else {
                        // 更新task状态
                        updateTaskStatus(cmd.getOptionValue("t"), cmd.getOptionValue("s"),
                            cmd.hasOption("p") ? Integer.valueOf(cmd.getOptionValue("p"))
                                : null, cmd.getOptionValue("d"));
                    }
                } catch (Exception e) {
                    // 异常提示
                    System.out.println(
                        "update commond format error. format: \"update -t taskId -s status -p progress -d "
                            + "statusDetails \"");
                }
            } else if (StringUtils.startsWith(command, "get")) {
                // 获取task信息
                try {
                    //解析入参
                    String[] params = parseParams(command);
                    CommandLineParser parser = new PosixParser();
                    CommandLine cmd = parser.parse(getOptions, params);

                    if (cmd.hasOption("h")) {
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("get", getOptions, true);
                    } else {
                        getJobStatus(cmd.getOptionValue("t"));
                    }
                } catch (Exception e) {
                    System.out.println("update commond format error. format: \"get -t taskId\"");
                }
            } else {
                // 不支持的命令提示
                System.out.println("unsupported command!");
            }
        }
    }

    /**
     * 检查启动初始化状态
     */
    private static void checkInitStatus() {
        if (initSuccess && subscribeNotify && subscribeGetReply && subscribeUpdateReply) {
            System.out.println("Start Succeeded !!!");
        } else {
            System.out.println("Start Failed !!!");
            throw new RuntimeException("Start Failed !!!");
        }
    }

    /**
     * sleep
     * @param delayMillSeconds 毫秒
     */
    private static void sleep(long delayMillSeconds) {
        try {
            Thread.sleep(delayMillSeconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析命令行入参
     * @param command 入参已空格分隔
     * @return
     */
    private static String[] parseParams(String command) {
        String[] commandString = command.split(" ");
        String[] value = new String[commandString.length - 1];

        for (int i = 1; i < commandString.length; i++) {
            value[i - 1] = commandString[i];
        }
        return value;
    }

    /**
     * 初始化
     * @param productKey     产品Key
     * @param deviceName     设备名称
     * @param deviceSecret   设备秘钥
     * @param region         区域信息
     */
    private  static void init(String productKey, String deviceName, String deviceSecret, String region) {
        LinkKitInitParams params = new LinkKitInitParams();
        /**
         * 设置 Mqtt 初始化参数
         */
        IoTMqttClientConfig config = new IoTMqttClientConfig();
        config.productKey = productKey;
        config.deviceName = deviceName;
        config.deviceSecret = deviceSecret;
        config.channelHost = productKey + ".iot-as-mqtt." + region + ".aliyuncs.com:1883";

        /**
         * 是否接受离线消息
         * 对应 mqtt 的 cleanSession 字段
         */
        config.receiveOfflineMsg = false;
        params.mqttClientConfig = config;

        /**
         * 设置初始化三元组信息，用户传入
         */
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.productKey = productKey;
        deviceInfo.deviceName = deviceName;
        deviceInfo.deviceSecret = deviceSecret;

        params.deviceInfo = deviceInfo;

        /**
         * 设置设备当前的初始状态值，属性需要和云端创建的物模型属性一致
         * 如果这里什么属性都不填，物模型就没有当前设备相关属性的初始值。
         * 用户调用物模型上报接口之后，物模型会有相关数据缓存。
         */
        Map<String, ValueWrapper> propertyValues = new HashMap<String, ValueWrapper>();
        params.propertyValues = propertyValues;
        LinkKit.getInstance().init(params, new ILinkKitConnectListener() {
            public void onError(AError aError) {
                System.out.println("connection failed");
            }

            public void onInitDone(InitResult initResult) {
                System.out.println("connection success");
                initSuccess = true;
            }
        });

        //等待
        sleep(5 * 1000);
        subscribeNotify();
        subscribeGetReply();
        subscribeUpdateReply();
        registerListener();

        //等待
        sleep(5 * 1000);
    }

    /**
     * 初始化订阅信息
     */
    private static void subscribeNotify() {
        MqttSubscribeRequest
            request = new MqttSubscribeRequest();
        // topic 用户根据实际场景填写
        request.topic = "/sys/" + productKey + "/" + deviceName + "/thing/job/notify";
        request.isSubscribe = true;
        LinkKit.getInstance().subscribe(request, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                System.out.println("subscribe notify success");
                subscribeNotify = true;
            }

            @Override
            public void onFailure(AError aError) {
                System.out.println("subscribe failed" + JSON.toJSONString(aError));
            }
        });
    }

    /**
     * 初始化订阅信息
     */
    private  static void subscribeGetReply() {
        MqttSubscribeRequest
            request = new MqttSubscribeRequest();
        // topic 用户根据实际场景填写
        request.topic = "/sys/" + productKey + "/" + deviceName + "/thing/job/get_reply";
        request.isSubscribe = true;
        LinkKit.getInstance().subscribe(request, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                System.out.println("subscribe get_reply success");
                subscribeGetReply = true;
            }

            @Override
            public void onFailure(AError aError) {
                System.out.println("subscribe get_reply failed");
            }
        });
    }

    /**
     * 初始化订阅信息
     */
    private static void subscribeUpdateReply() {
        MqttSubscribeRequest
            request = new MqttSubscribeRequest();
        // topic 用户根据实际场景填写
        request.topic = "/sys/" + productKey + "/" + deviceName + "/thing/job/update_reply";
        request.isSubscribe = true;
        LinkKit.getInstance().subscribe(request, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                System.out.println("subscribe update_reply success");
                subscribeUpdateReply = true;
            }

            @Override
            public void onFailure(AError aError) {
                System.out.println("subscribe update_reply failed");
            }
        });
    }


    /**
     * 打印错误信息
     */
    private static String printAMessage(AMessage aMessage) {
        return (aMessage == null || aMessage.data == null) ? "" : new String((byte[])aMessage.data);
    }

    /**
     * 解析入参
     */
    private static void registerListener() {
        LinkKit.getInstance().registerOnNotifyListener(new IConnectNotifyListener() {
            @Override
            public void onNotify(String connectId, String topic, AMessage aMessage) {
                if (StringUtils.isNotBlank(topic) &&
                    topic.startsWith("/sys/" + productKey + "/" + deviceName + "/thing/job/notify")) {
                    System.out.print("收到任务通知消息:" + printAMessage(aMessage));
                }

                if (StringUtils.isNotBlank(topic) &&
                    topic.startsWith("/sys/" + productKey + "/" + deviceName + "/thing/job/get_reply")) {
                    System.out.print("收到任务get信息:" + printAMessage(aMessage));
                }
            }

            @Override
            public boolean shouldHandle(String s, String s1) {
                return false;
            }

            @Override
            public void onConnectStateChange(String s, ConnectState connectState) {
                System.out.print("s=" + s + ",connectState=" + JSON.toJSONString(connectState));
            }
        });
    }

    /**
     * 更新task状态
     *
     * @param taskId
     * @param status
     * @param progress
     * @param detail
     */
    private static void updateTaskStatus(String taskId, String status, Integer progress, String detail) {
        JSONObject response = new JSONObject();
        response.put("id", "123");
        response.put("version", "1.0");

        JSONObject params = new JSONObject();
        params.put("taskId", taskId);
        params.put("status", status);

        if (null != progress) {
            params.put("progress", progress);
        }

        if (StringUtils.isNotBlank(detail)) {
            params.put("statusDetails", JSON.parseObject(detail));
        }
        response.put("params", params);

        MqttPublishRequest request = new MqttPublishRequest();
        request.isRPC = false;
        request.topic = "/sys/" + productKey + "/" + deviceName + "/thing/job/update";
        request.msgId = "123";

        request.payloadObj = response.toJSONString();
        LinkKit.getInstance().getMqttClient().publish(request, new IConnectSendListener() {
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                System.out.print("update sccess");
            }

            public void onFailure(ARequest aRequest, AError aError) {
                System.out.print("update sccess");
            }
        });
    }

    /**
     * 获取Job状态
     * @param taskId
     */
    private static void getJobStatus(String taskId) {
        JSONObject response = new JSONObject();
        response.put("id", "123");
        response.put("version", "1.0");

        JSONObject params = new JSONObject();
        params.put("taskId", taskId);
        response.put("params", params);

        MqttPublishRequest request = new MqttPublishRequest();
        request.isRPC = false;
        request.topic = "/sys/" + productKey + "/" + deviceName + "/thing/job/get";
        request.msgId = "123";

        request.payloadObj = response.toJSONString();
        LinkKit.getInstance().getMqttClient().publish(request, new IConnectSendListener() {
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                System.out.print("update sccess");
            }

            public void onFailure(ARequest aRequest, AError aError) {
                System.out.print("update sccess");
            }
        });
    }

    /**
     * 初始化命令行解析
     */
    private static void initOptions() {
        updateOptions = new Options();
        updateOptions.addOption(Option.builder("t").hasArg(true).argName("taskId").desc("Task唯一标识符, 取值不能包含空格")
            .type(String.class)
            .build());
        updateOptions.addOption(Option.builder("s").hasArg(true).argName("status").desc("Task 状态 status, 取值不能包含空格")
            .type(String.class)
            .build());
        updateOptions.addOption(Option.builder("p").hasArg(true).argName("progress").desc("Task 进度 progress, 取值不能包含空格")
            .type(String.class)
            .build());
        updateOptions.addOption(Option.builder("d").hasArg(true).argName("statusDetails").desc("Task 执行详情, 取值不能包含空格")
            .type(String.class)
            .build());
        updateOptions.addOption(Option.builder("h").argName("help").desc("帮助").type(String.class).build());

        getOptions = new Options();
        getOptions.addOption(Option.builder("t").hasArg(true).argName("taskId").desc("请求入参, 取值不能包含空格")
            .type(String.class).build());
        getOptions.addOption(Option.builder("h").argName("help").desc("帮助").type(String.class).build());
    }
}



