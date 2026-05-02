package de.yyuh.platform.s3.credentials;

import java.util.Optional;

import de.yyuh.celery.api.credentials.Credentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

// S3 is such a shithole
public final class S3IdentitiyProvider implements AwsCredentialsProvider {

  private final Credentials credentials;

  public S3IdentitiyProvider(final Credentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public AwsCredentials resolveCredentials() {
    return new AwsCredentials() {
      @Override
      public String accessKeyId() {
        return credentials.accessKeyId();
      }

      @Override
      public Optional<String> accountId() {
        return Optional.of(credentials.user());
      }

      @Override
      public String secretAccessKey() {
        return credentials.accessKey();
      }
    };
  }
}
