package de.yyuh.platform.s3.provider;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import de.yyuh.celery.api.credentials.Credentials;
import de.yyuh.celery.api.provider.IFileStorageProvider;
import de.yyuh.libs.core.result.Result;
import de.yyuh.libs.core.timer.Timer;
import de.yyuh.platform.s3.credentials.S3IdentityProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Amazon S3 implementation of {@link IFileStorageProvider}.
 *
 * <p>This provider connects to an S3 bucket using the AWS SDK and supports
 * uploading, downloading, and deleting files. Credentials must include the
 * bucket name, region, access key ID, and secret access key.
 *
 * <p>Operations are asynchronous and execute on the common ForkJoinPool.
 */
public final class S3FileStorageProvider implements IFileStorageProvider {

  private S3Client s3Client;
  private String bucket;

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Result<Long, String>> connect(final Credentials credentials) {
    final var timer = Timer.start();

    this.bucket = credentials.bucket();

    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      final var region = Region.of(credentials.region());

      this.s3Client = S3Client.builder()
          .region(region)
          .credentialsProvider(new S3IdentityProvider(credentials))
          .build();

      return timer.end();
    }).mapErr(Exception::getMessage));

  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> delete(final String path) {
    return CompletableFuture.runAsync(() -> {
      this.s3Client.deleteObject(DeleteObjectRequest.builder()
          .bucket(this.bucket)
          .key(path)
          .build());
    });
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Optional<File>> get(final String path, final String dest) {
    final var destPath = Path.of(dest);

    return CompletableFuture.supplyAsync(() -> {
      this.s3Client.getObject(
          GetObjectRequest.builder()
              .bucket(this.bucket)
              .key(path)
              .build(),
          ResponseTransformer.toFile(destPath));

      return Optional.of(destPath.toFile());
    });
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Void> save(
      final File file,
      final String path) {
    return CompletableFuture.runAsync(() -> {
      this.s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(this.bucket)
              .key(path)
              .build(),
          RequestBody.fromFile(file));
    });
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    this.s3Client.close();
  }

  /** {@inheritDoc} */
  @Override
  public @NotNull CompletableFuture<Boolean> isConnected() {
    return CompletableFuture.supplyAsync(() -> Result.of(() -> {
      this.s3Client.headBucket(b -> b.bucket(this.bucket));

      return true;
    }).map(_ -> false).unwrapOr(false));
  }

}
