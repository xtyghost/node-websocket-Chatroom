const express = require('express'),
  app = express(),
  server = require('http').createServer(app),
  io = require('socket.io').listen(server),
  //用于保存用户信息的数组
  PORT=246,
  users = [];
let kit = {
  //判断用户是否存在
  isHaveUser(user) {
    let flag = false;
    users.forEach(function (item) {
      if (item.name == user.name) {
        flag = true;
      }
    })
    return flag;
  },
  //删除某一用户
  delUser(id) {
    users.forEach(function (item, index) {
      if (item.id == id) {
        users.splice(index, 1);
      }
    })
  },
  getDeviceType(userAgent){
    let bIsIpad = userAgent.match(/ipad/i) == "ipad";
    let bIsIphoneOs = userAgent.match(/iphone os/i) == "iphone os";
    let bIsMidp = userAgent.match(/midp/i) == "midp";
    let bIsUc7 = userAgent.match(/rv:1.2.3.4/i) == "rv:1.2.3.4";
    let bIsUc = userAgent.match(/ucweb/i) == "ucweb";
    let bIsAndroid = userAgent.match(/android/i) == "android";
    let bIsCE = userAgent.match(/windows ce/i) == "windows ce";
    let bIsWM = userAgent.match(/windows mobile/i) == "windows mobile";
    if (bIsIpad || bIsIphoneOs || bIsMidp || bIsUc7 || bIsUc || bIsAndroid || bIsCE || bIsWM) {
      return "touch";
    } else {
      return "pc";
    }
  }
}
//设置静态资源
app.use('/static', express.static(__dirname + '/static'));
//用户访问网站页面会根据浏览器userAgent返回不同的页面
app.get("/", (req, res) => {
  let userAgent = req.headers['user-agent'].toLowerCase();
  if (kit.getDeviceType(userAgent)=='touch') {
    console.log("/static/iChat.html")
    let path = __dirname + '/static/iChat.html';
    res.sendFile(path);
  } else {
    console.log("/static/index.html")
    let path = __dirname + '/static/index.html';
    res.sendFile(path);
  }
})
//启动服务器
server.listen(PORT,()=> {
  console.log(`服务器已启动在：${PORT}端口`, `112.17.176.110:${PORT}`)
});