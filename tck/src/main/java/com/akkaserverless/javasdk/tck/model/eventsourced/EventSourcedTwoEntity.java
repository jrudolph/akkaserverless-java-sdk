/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.eventsourced;

import com.akkaserverless.javasdk.eventsourced.CommandHandler;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntity;
import com.akkaserverless.tck.model.Eventsourced.Request;
import com.akkaserverless.tck.model.Eventsourced.Response;

@EventSourcedEntity
public class EventSourcedTwoEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
