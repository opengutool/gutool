# DDD
使用：[damibus](https://github.com/noear/damibus) 来实现事件的传递。

# 说说我的理解

> 1、DDD 是充血模型，那么在调用方法的时候，就要发出事件，即使这个事件没有人订阅。
> 
> 2、事件发出之后，就推送到 bus 中，可新增订阅，或者批量订阅。
> 
> 3、事件订阅的一个很常见的场景是：存储。


# 想法
1、一个对象可以被 代理 成为一个事件发送器。

2、调用方法的时候，就会发送事件。

3、如果有订阅那么，订阅者就会收到事件，并响应。

# 怎么监听呢？
通过表达式来监听刚好，使用 `TopicRouterPatterned` 非常合适。

示例：
```java
DDDFactory.init();
//拦截
Dami.bus().listen("gutool.**.VersionSummary.**.after", (payload) -> {
    System.err.println((VersionSummary) payload.getContent());
});
VersionSummary versionSummaryProxy = DDDFactory.create(new VersionSummary());
versionSummaryProxy.setCurrentVersion("v1.0.1");
```



