package launcher;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import vo.PlmTalkUserVo;
import vo.TalkMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static com.ingsys.plmtalk.constants.SocketIoEventType.*;

/**
 * 项目聊天功能的后台代码 基本思路，使用socket.io思想聊天室功能， 把每个socket的id作为房间号， 共享房间号，的房间id为groupMessage TODO
 * 添加共享房间的动态添加
 */
@Slf4j
public class PLMTalkLauncher {
  /** 记录当前登陆用户的信息 ；TODO 新增历史用户信息 */
  private ConcurrentHashMap<String, PlmTalkUserVo> loginUsers = new ConcurrentHashMap<>();

  {
    // 从后台获取onLineUsers的推送
    //              {
    //                  id: "group",
    //                          avatarUrl:
    // "http://148.70.90.247/static/images/group-icon.png",
    //                      name: "聊天室群",
    //                      type: "room"
    //              }
    loginUsers.put(
        "",
        PlmTalkUserVo.builder()
            .id("group")
            .avatarUrl("http://148.70.90.247/static/images/group-icon.png")
            .name("聊天室群")
            .type("room")
            .build());
  }

  /**
   * 校验用户是否合法，并修改用户的基本行
   *
   * @param plmTalkUserVo
   * @param socketIOClient
   * @return
   */
  private boolean isRightFul(PlmTalkUserVo plmTalkUserVo, SocketIOClient socketIOClient) {
    if (isSupportConcurrentLogin()) {
      return false;
    } else {
      // 获取用户信息进行验证
      //        OkHttpUtil.getQueryString()

    }
    plmTalkUserVo.setId(socketIOClient.getSessionId().toString());
    plmTalkUserVo.setRoomIds(socketIOClient.getSessionId().toString());
    plmTalkUserVo.setAddress(socketIOClient.getHandshakeData().getAddress().getHostName());
    plmTalkUserVo.setLoginTime(LocalDateTime.now());
    plmTalkUserVo.setType("user");
    return true;
  }

  /**
   * 是否支持多地登陆;TODO 新增多地登陆功能
   *
   * @return
   */
  private boolean isSupportConcurrentLogin() {
    return false;
  }

  public void main(String[] args) throws InterruptedException {

    Configuration config = new Configuration();
    config.setHostname("localhost");
    config.setPort(3000);

    final SocketIOServer server = new SocketIOServer(config);
    server.addConnectListener(
        socketIOClient -> {
          // 创建用户连接
          System.out.println("创建用户连接" + ToStringBuilder.reflectionToString(socketIOClient));
          // 打印当前连接中的用户
          System.out.println(ToStringBuilder.reflectionToString(loginUsers));
        });
    // 登陆时更新用户信息
    server.addEventListener(
        LOGIN.getName(),
        PlmTalkUserVo.class,
        (socketIOClient, plmTalkUserVo, ackRequest) -> {
          // 进行用户校验
          if (isRightFul(plmTalkUserVo, socketIOClient)) {
            // 添加用户信息，向其它对象发送广播
            log.info("user=={};login success++++++++++++++++++", plmTalkUserVo);
            System.out.println("登陆时更新用户信息" + ToStringBuilder.reflectionToString(socketIOClient));
            //添加用户是加入roomid为sessionid的房间
              socketIOClient.joinRoom(plmTalkUserVo.getRoomIds());
              loginUsers.put(plmTalkUserVo.getId(), plmTalkUserVo);
              server
                .getBroadcastOperations()
                .sendEvent("system", Arrays.asList(loginUsers.values().toArray(), "join"));
            socketIOClient.sendEvent(
                LOGINSUCCESS.getName(), plmTalkUserVo, loginUsers.values().toArray());
          } else {
            log.warn("login fail User=={}", plmTalkUserVo);
            socketIOClient.sendEvent(LOGINFAIL.getName(), "登陆失败，登陆密码或用户名有误！！");
          }
        });
    // 用户登出
    server.addDisconnectListener(
        client -> {
          PlmTalkUserVo remove = loginUsers.remove(client.getSessionId());
          log.info(" user=={} user logout---------------", remove);
          server
              .getBroadcastOperations()
              .sendEvent(SYSTEM.getName(), Arrays.asList(remove, "logout"));
        });
    // 群发消息，暂时是向grouMessage的房间发消息
    server.addEventListener(
        "groupMessage",
        TalkMessage.class,
        (client, data, ackSender) -> {
          System.out.println(data);

          BroadcastOperations broadcastOperations = server.getBroadcastOperations();
//          broadcastOperations
//              .getClients()
//              .removeIf(client1 -> !client1.getSessionId().equals(client.getSessionId()));

          broadcastOperations.sendEvent(
              "groupMessage",
              TalkMessage.builder()
                  .from(loginUsers.get(client.getSessionId().toString()))
                  .to(data.getTo())
                  .message(data.getMessage())
                  .type(data.getType())
                  .build());
          System.out.println(loginUsers.get(client.getSessionId().toString()));
        });
    // 发送私信
    server.addEventListener(
        "message",
        TalkMessage.class,
        (client, data, ackSender) -> {
          System.out.println(data);
          server
              .getRoomOperations(data.getTo().getRoomIds())
              .sendEvent(
                  "message",
                  TalkMessage.builder()
                      .from(loginUsers.get(client.getSessionId().toString()))
                      .to(data.getTo())
                      .message(data.getMessage())
                      .type(data.getType())
                      .build());
        });
    server.start();
    Thread.sleep(Integer.MAX_VALUE);

    server.stop();
  }
}
