HTTP_Proxy
基于Java语言实现的简易HTTP协议代理服务器
这个分支是最原始的版本(V0.0.1),功能简陋
适合用来学习HTTP代理的大致流程

实现原理
服务端
开启一个Socket, bind 并 listen一个端口
当客户端连接到这个端口的时候,获得连接的TCP Socket,并新创建一个线程处理这个Socket
接收到来着客户端的请求信息request
从request中解析出method,ip,port,等信息
判断解析出的协议是否支持, 页面是否有缓存
代理服务器连接客户端要访问的目标页面所在的服务器,将request发送过去
代理服务器接收目标服务器的响应response
代理服务器将response发给客户端
关闭socket
客户端
设置浏览器代理为服务器对应的IP和port即可
完成度
 HTTP代理

 缓存

 网站限制

遇到的问题
使用Java的BufferedReader对象的readline()函数来读取服务器响应的内容时,会卡在最后一行,等1.3min后才返回内容

原因:

​ readline()函数是根据换行符来读取的,若响应数据的最后一行没有换行符,则BufferedReader会一直阻塞,直到连接超时被Close掉,readLine才会返回数据.