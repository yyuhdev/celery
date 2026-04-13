package de.yyuh.celery.api.messaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import org.reflections.Reflections;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.Descriptors.Descriptor;

public final class MessageRegistry {
  private static final TypeRegistry TYPE_REGISTRY;
  private static final Map<String, Parser<? extends Message>> PARSERS = new HashMap<>();

  static {
    TypeRegistry.Builder builder = TypeRegistry.newBuilder();

    findAllProtoClasses().forEach(entry -> {
      builder.add(entry.descriptor());
      PARSERS.put(entry.descriptor().getFullName(), entry.parser());
    });

    TYPE_REGISTRY = builder.build();
  }

  private record ProtoEntry(Descriptors.Descriptor descriptor, Parser<? extends Message> parser) {
  }

  @NotNull
  private static List<ProtoEntry> findAllProtoClasses() {
    final var reflections = new Reflections("de.yyuh");

    return reflections.getSubTypesOf(GeneratedMessageV3.class)
        .stream()
        .map(clazz -> {
          try {
            final var descriptor = (Descriptors.Descriptor) clazz.getMethod("getDescriptor").invoke(null);

            @SuppressWarnings("unchecked")
            final var parser = (Parser<? extends Message>) clazz.getMethod("parser").invoke(null);

            return new ProtoEntry(descriptor, parser);
          } catch (Exception e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toList();
  }

  @NotNull
  public static Message unpack(final byte[] bytes) throws InvalidProtocolBufferException {
    final Any any = Any.parseFrom(bytes);
    final Descriptor descriptor = TYPE_REGISTRY.getDescriptorForTypeUrl(any.getTypeUrl());

    if (descriptor == null) {
      throw new InvalidProtocolBufferException("Unknown type: " + any.getTypeUrl());
    }

    final Parser<? extends Message> parser = PARSERS.get(descriptor.getFullName());

    if (parser == null) {
      throw new InvalidProtocolBufferException("No parser for: " + descriptor.getFullName());
    }

    return parser.parseFrom(any.getValue());
  }
}
