/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.crdt

import com.akkaserverless.javasdk.crdt._
import com.akkaserverless.javasdk.impl.AnySupport

// TODO JavaDoc
trait AbstractCrdtFactory extends CrdtFactory {
  // TODO JavaDoc
  protected def anySupport: AnySupport
  // TODO JavaDoc
  protected def newCrdt[C <: InternalCrdt](crdt: C): C
  // TODO JavaDoc
  override def newGCounter(): GCounter = newCrdt(new GCounterImpl)
  // TODO JavaDoc
  override def newPNCounter(): PNCounter = newCrdt(new PNCounterImpl)
  // TODO JavaDoc
  override def newGSet[T](): GSet[T] = newCrdt(new GSetImpl[T](anySupport))
  // TODO JavaDoc
  override def newORSet[T](): ORSet[T] = newCrdt(new ORSetImpl[T](anySupport))
  // TODO JavaDoc
  override def newFlag(): Flag = newCrdt(new FlagImpl)
  // TODO JavaDoc
  override def newLWWRegister[T](value: T): LWWRegister[T] = {
    val register = newCrdt(new LWWRegisterImpl[T](anySupport))
    if (value != null) {
      register.set(value)
    }
    register
  }
  // TODO JavaDoc
  override def newORMap[K, V <: Crdt](): ORMap[K, V] =
    newCrdt(new ORMapImpl[K, InternalCrdt](anySupport)).asInstanceOf[ORMap[K, V]]
  // TODO JavaDoc
  override def newVote(): Vote = newCrdt(new VoteImpl)
}
