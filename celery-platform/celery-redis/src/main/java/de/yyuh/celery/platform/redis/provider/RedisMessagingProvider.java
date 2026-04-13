package de.yyuh.celery.platform.redis.provider;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.messaging.IMessagingProvider;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.listener.PubSubListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class RedisMessagingProvider implements IMessagingProvider {

}
