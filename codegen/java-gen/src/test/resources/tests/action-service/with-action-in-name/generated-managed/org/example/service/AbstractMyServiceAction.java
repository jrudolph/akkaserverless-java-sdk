package org.example.service;

import com.google.protobuf.Empty;
import org.example.Components;
import org.example.ComponentsImpl;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/** An action. */
public abstract class AbstractMyServiceAction extends com.akkaserverless.javasdk.action.Action {

  protected final Components components() {
    return new ComponentsImpl(actionContext());
  }

  /** Handler for "simpleMethod". */
  public abstract Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);
}