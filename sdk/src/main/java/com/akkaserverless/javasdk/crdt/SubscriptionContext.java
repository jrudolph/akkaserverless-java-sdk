/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.crdt;

import com.akkaserverless.javasdk.ClientActionContext;
import com.akkaserverless.javasdk.EffectContext;

import java.util.function.Function;

/**
 * The context for a subscription, passed with every invocation of a {@link
 * StreamedCommandContext#onChange(Function)} callback.
 */
public interface SubscriptionContext extends CrdtContext, EffectContext, ClientActionContext {
  /** End this stream. */
  void endStream();
}
