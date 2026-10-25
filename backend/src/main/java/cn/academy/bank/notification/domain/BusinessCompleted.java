package cn.academy.bank.notification.domain;

/** 事件只携带不可变数据，发布方无需知道通知如何保存。 */
public record BusinessCompleted(long actorId, String title, String content) {}
