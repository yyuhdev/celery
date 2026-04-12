package club.revived.celery;

public enum CeleryPlatforms implements IPlatformType {

  MONGODB,
  INFLUXDB,
  REDIS,
  NATS,
  SQL;

}
