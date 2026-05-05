package de.yyuh.celery.api.messaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.TypeRegistry;

import de.yyuh.libs.core.result.Result;

/**
 * Registry for managing and unpacking Protobuf Any messages.
 *
 * <p>
 * This class scans a given package for all Protobuf message classes and builds
 * a
 * TypeRegistry to enable type-safe unpacking of {@link Any} messages.
 *
 * <p>
 * Usage:
 *
 * <pre>{@code
 * MessageRegistry registry = MessageRegistry.create("com.example.myapp");
 * Message message = registry.unpack(anyBytes);
 * }</pre>
 */
public final class MessageRegistry {

  private TypeRegistry typeRegistry;

  private final Map<String, Parser<? extends Message>> parsers = new HashMap<>();

  private final List<String> packages = new CopyOnWriteArrayList<>();

  /**
   * Constructs a new MessageRegistry and intiates it
   *
   * @param packageStr the package name to scan for Protobuf messages
   */
  public MessageRegistry() {
  }

  /**
   * Adds a package to the package registry for scanning later
   *
   * @param the package to scan through
   *
   * @return the Message Registry's instance
   */
  @NotNull
  public MessageRegistry withPackage(final String packageStr) {
    this.packages.add(packageStr);

    return this;
  }

  /**
   * Iterates through all given packages and then constructs a
   * {@link TypeRegistry} out of all of them.
   */
  public void register() {
    final TypeRegistry.Builder builder = TypeRegistry.newBuilder();

    for (final var packageStr : this.packages) {
      this.findAllProtoClasses(packageStr).forEach(entry -> {
        builder.add(entry.descriptor());
        parsers.put(entry.descriptor().getFullName(), entry.parser());
      });
    }

    this.typeRegistry = builder.build();
  }

  /**
   * Internal record holding a Protobuf descriptor and its corresponding parser.
   *
   * @param descriptor the Protobuf message descriptor
   * @param parser     the parser for deserializing messages of this type
   */
  private record ProtoEntry(Descriptors.Descriptor descriptor, Parser<? extends Message> parser) {
  }

  /**
   * Scans the given package for all Protobuf message classes and creates
   * ProtoEntry objects
   * containing their descriptors and parsers.
   *
   * @param string the package name to scan
   * @return list of ProtoEntry objects for all discovered Protobuf messages
   */
  @NotNull
  private List<ProtoEntry> findAllProtoClasses(final @NotNull String string) {
    final var reflections = new Reflections(string);

    return reflections.getSubTypesOf(GeneratedMessage.class)
        .stream()
        .map(clazz -> Result.of(() -> {
          final var descriptor = (Descriptors.Descriptor) clazz.getMethod("getDescriptor").invoke(null);

          @SuppressWarnings("unchecked")
          final var parser = (Parser<? extends Message>) clazz.getMethod("parser").invoke(null);

          return new ProtoEntry(descriptor, parser);
        }))
        .peek(result -> result.ifErr(e -> System.err.println("Failed to process proto class: " + e.getMessage())))
        .flatMap(result -> result.ok().stream())
        .toList();
  }

  /**
   * Unpacks a Protobuf Any message into its actual message type.
   *
   * @param bytes the serialized Any message
   * @return the unpacked Protobuf message
   * @throws InvalidProtocolBufferException if the message cannot be parsed or the
   *                                        type is unknown
   */
  @NotNull
  public Message unpack(final byte[] bytes) throws InvalidProtocolBufferException {
    final Any any = Any.parseFrom(bytes);
    final Descriptor descriptor = this.typeRegistry.getDescriptorForTypeUrl(any.getTypeUrl());

    if (descriptor == null) {
      throw new InvalidProtocolBufferException("Unknown type: " + any.getTypeUrl());
    }

    final Parser<? extends Message> parser = this.parsers.get(descriptor.getFullName());

    if (parser == null) {
      throw new InvalidProtocolBufferException("No parser for: " + descriptor.getFullName());
    }

    return parser.parseFrom(any.getValue());
  }
}
