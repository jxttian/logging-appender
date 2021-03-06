package net.myscloud.plugin.logging.sender;

import ch.qos.logback.core.spi.ContextAwareBase;
import com.alibaba.fastjson.JSON;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.myscloud.plugin.logging.LoggerEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka消息发送者
 */
@Setter
@Getter
@Builder
public class KafkaSender extends ContextAwareBase implements MessageSender {

    private final KafkaProducer<Long, String> producer;

    private String topic;

    private long timeout;

    @Override
    public boolean send(LoggerEvent event) {
        return send(topic, event);
    }

    @Override
    public boolean sendAsync(LoggerEvent event) {
        return sendAsync(topic, event);
    }

    @Override
    public boolean send(String topic, LoggerEvent event) {
        try {
            final Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, JSON.toJSONString(event)));
            if (timeout > 0L)
                future.get(timeout, TimeUnit.MILLISECONDS);
            else if (timeout == 0) future.get();
            return true;
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException | TimeoutException e) {
            addError("Kafka 同步发送日志失败 : " + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean sendAsync(String topic, LoggerEvent event) {
        producer.send(new ProducerRecord<>(topic, JSON.toJSONString(event)), (metadata, exception) -> {
            if (exception != null) {
                addError("Kafka 异步发送日志失败 : " + exception.getMessage());
            }
        });
        return true;
    }

    @Override
    public void init() {
        // init
    }

    @Override
    public void destroy() {
        producer.close();
    }
}
