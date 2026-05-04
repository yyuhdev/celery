[![wakatime](https://wakatime.com/badge/user/dd51c621-143b-4311-843c-7ee475851640/project/c75f6f08-0f92-4625-bd63-eba8e066a276.svg)](https://wakatime.com/badge/user/dd51c621-143b-4311-843c-7ee475851640/project/c75f6f08-0f92-4625-bd63-eba8e066a276)

<div align="center">
  <img src="img/logo.png" alt="Celery Logo" height="240">
</div>

# Celery

A modular database abstraction framework for Java. Celery unifies access to multiple database backends behind a consistent, extensible API — one interface for storage, pub/sub messaging, and caching across MongoDB, Redis, InfluxDB, NATS, and S3.

## What it does

Celery decouples application code from database implementations. You define entities with annotations, query through a typed query API, and let Celery route operations to the right backend. Switch databases or mix backends without rewriting business logic.

### Supported backends

| Backend | Storage | Pub/Sub | Cache |
|---------|:-------:|:-------:|:-----:|
| MongoDB | ✓ | | |
| Redis | | ✓ | ✓ |
| Redis Cluster | | ✓ | ✓ |
| InfluxDB | ✓ | | |
| NATS | | ✓ | |
| S3 | ✓ | | |

## Structure

```
celery-api/         # Public interfaces and annotations
celery/             # Main entry point (Celery builder, platform wiring)
celery-platform/    # Backend implementations (each platform is a submodule)
shared/             # Shared utilities (Result<T,E>, Timer, DI)
testing/            # Integration test suite
```

## Build

```bash
./gradlew build
```

---

<img src="https://yyuh.beer/banners/vim-tenor.gif" width="88" height="33" />
