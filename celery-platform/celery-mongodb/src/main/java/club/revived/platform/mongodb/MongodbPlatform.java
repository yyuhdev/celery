package club.revived.platform.mongodb;

import club.revived.celery.AbstractCeleryPlatform;

import club.revived.commons.result.Result;

public final class MongodbPlatform extends AbstractCeleryPlatform {

  public MongodbPlatform() {
    super("mongodb");
  }

}
