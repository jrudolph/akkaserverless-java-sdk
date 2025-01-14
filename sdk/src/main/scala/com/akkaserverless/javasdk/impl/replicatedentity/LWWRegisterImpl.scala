/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity.LWWRegister
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.replicated_entity.{LWWRegisterDelta, ReplicatedEntityClock, ReplicatedEntityDelta}
import com.google.protobuf.any.{Any => ScalaPbAny}

import java.util.Objects

private[replicatedentity] final class LWWRegisterImpl[T](anySupport: AnySupport)
    extends InternalReplicatedData
    with LWWRegister[T] {
  override final val name = "LWWRegister"
  private var value: T = _
  private var deltaValue: Option[ScalaPbAny] = None
  private var clock: LWWRegister.Clock = LWWRegister.Clock.DEFAULT
  private var customClockValue: Long = 0

  override def set(value: T, clock: LWWRegister.Clock, customClockValue: Long): T = {
    Objects.requireNonNull(value)
    val old = this.value
    if (this.value != value || this.clock != clock || this.customClockValue != customClockValue) {
      deltaValue = Some(anySupport.encodeScala(value))
      this.value = value
      this.clock = clock
      this.customClockValue = customClockValue
    }
    old
  }

  override def get(): T = value

  override def hasDelta: Boolean = deltaValue.isDefined

  override def delta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.Lwwregister(LWWRegisterDelta(deltaValue, convertClock(clock), customClockValue))

  override def resetDelta(): Unit = {
    deltaValue = None
    clock = LWWRegister.Clock.DEFAULT
    customClockValue = 0
  }

  override val applyDelta = {
    case ReplicatedEntityDelta.Delta.Lwwregister(LWWRegisterDelta(Some(any), _, _, _)) =>
      resetDelta()
      this.value = anySupport.decode(any).asInstanceOf[T]
  }

  private def convertClock(clock: LWWRegister.Clock): ReplicatedEntityClock =
    clock match {
      case LWWRegister.Clock.DEFAULT => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_DEFAULT_UNSPECIFIED
      case LWWRegister.Clock.REVERSE => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_REVERSE
      case LWWRegister.Clock.CUSTOM => ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM
      case LWWRegister.Clock.CUSTOM_AUTO_INCREMENT =>
        ReplicatedEntityClock.REPLICATED_ENTITY_CLOCK_CUSTOM_AUTO_INCREMENT
    }

  override def toString = s"LWWRegister($value)"
}
