package com.springboot.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ---- 大作业 Agent 评测队列 ----
    public static final String EVAL_QUEUE        = "eval.task.queue";
    public static final String EVAL_DEAD_QUEUE   = "eval.task.dead.queue";
    public static final String EVAL_EXCHANGE     = "eval.task.exchange";
    public static final String EVAL_DEAD_EXCHANGE = "eval.task.dead.exchange";
    public static final String EVAL_ROUTING_KEY  = "eval.task";

    /** 死信队列：任务失败/超时后路由到此，便于排查 */
    @Bean
    public Queue evalDeadQueue() {
        return QueueBuilder.durable(EVAL_DEAD_QUEUE).build();
    }

    @Bean
    public DirectExchange evalDeadExchange() {
        return new DirectExchange(EVAL_DEAD_EXCHANGE);
    }

    @Bean
    public Binding evalDeadBinding() {
        return BindingBuilder.bind(evalDeadQueue()).to(evalDeadExchange()).with(EVAL_ROUTING_KEY);
    }

    /** 主评测队列，配置死信路由 */
    @Bean
    public Queue evalQueue() {
        return QueueBuilder.durable(EVAL_QUEUE)
                .withArgument("x-dead-letter-exchange", EVAL_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EVAL_ROUTING_KEY)
                // 单条消息 TTL 10 分钟，防止消息长期卡死
                .withArgument("x-message-ttl", 600000L)
                .build();
    }

    @Bean
    public DirectExchange evalExchange() {
        return new DirectExchange(EVAL_EXCHANGE);
    }

    @Bean
    public Binding evalBinding() {
        return BindingBuilder.bind(evalQueue()).to(evalExchange()).with(EVAL_ROUTING_KEY);
    }

    /** 使用 JSON 序列化消息体 */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Consumer 容器工厂：concurrency=3，prefetchCount=1
     * 最多 3 个容器并发评测，每次只取 1 条消息防止堆积
     */
    @Bean
    public SimpleRabbitListenerContainerFactory evalListenerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(3);
        factory.setPrefetchCount(1);
        // 手动 ACK，消费成功后才确认，失败则重入死信
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
