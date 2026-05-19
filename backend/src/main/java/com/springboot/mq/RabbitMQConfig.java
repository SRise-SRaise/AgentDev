package com.springboot.mq;

import org.springframework.amqp.core.AcknowledgeMode;
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

    public static final String EVAL_QUEUE         = "eval.task.queue";
    public static final String EVAL_DEAD_QUEUE    = "eval.task.dead.queue";
    public static final String EVAL_EXCHANGE      = "eval.task.exchange";
    public static final String EVAL_DEAD_EXCHANGE = "eval.task.dead.exchange";
    public static final String EVAL_ROUTING_KEY   = "eval.task";

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

    /** Main eval queue with dead-letter routing on rejection/TTL */
    @Bean
    public Queue evalQueue() {
        return QueueBuilder.durable(EVAL_QUEUE)
                .withArgument("x-dead-letter-exchange", EVAL_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", EVAL_ROUTING_KEY)
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
     * Consumer factory: concurrency=3, prefetchCount=1, manual ACK.
     * At most 3 containers evaluated concurrently.
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
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
