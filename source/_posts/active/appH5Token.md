---
title: app嵌入H5如何同步登录状态
date: 2021-10-11
desc:
keywords: token
categories: [active]
---
目前使用的方案：
1.h5通过js和app交互调用app的token
2.拿到token后使用ajax做异步登录并刷新页面
目前的缺点：
1.会多次刷新页面
2.通过js调用时H5需要做部分改动

优化的方向：
A.APP登陆状态的变化是请求页面，登录后使用webview调用对应域名下的接口实现H5的登录B.每次请求H5url增加get登陆的信息，例如访问 index.php变成index.php？a=xxx通过附加信息同步登录状态
优化的方案不知道那个更好一点


