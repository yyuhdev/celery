package de.yyuh.celery.cluster;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ClusterType {

  String name();

  int nodePort();

}
