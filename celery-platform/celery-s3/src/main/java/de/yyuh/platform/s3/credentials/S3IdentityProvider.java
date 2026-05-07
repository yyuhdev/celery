package de.yyuh.platform.s3.credentials;

import java.util.Optional;

import de.yyuh.celery.api.credentials.Credentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * AWS credentials provider that bridges Celery {@link Credentials}
 * to the AWS SDK's {@link AwsCredentialsProvider}.
 *
 * <p>Maps the Celery {@code accessKeyId} and {@code accessKey} fields
 * to AWS access key and secret access key respectively.
 */
public final class S3IdentityProvider implements AwsCredentialsProvider {

  private final Credentials credentials;

  /**
   * Creates a new S3IdentityProvider.
   *
   * @param credentials the Celery credentials containing AWS access keys
   */
  public S3IdentityProvider(final Credentials credentials) {
    this.credentials = credentials;
  }

  /** {@inheritDoc} */
  @Override
  public AwsCredentials resolveCredentials() {
    return new AwsCredentials() {
      /** {@inheritDoc} */
      @Override
      public String accessKeyId() {
        return credentials.accessKeyId();
      }

      /** {@inheritDoc} */
      @Override
      public Optional<String> accountId() {
        return Optional.of(credentials.user());
      }

      /** {@inheritDoc} */
      @Override
      public String secretAccessKey() {
        return credentials.accessKey();
      }
    };
  }
}
