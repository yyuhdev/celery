package de.yyuh.testing.entity;

import de.yyuh.celery.api.annotation.Identifier;
import de.yyuh.celery.api.entity.IEntity;

public record TestEntity(
    @Identifier String id,
    String name,
    int value) implements IEntity {

}
