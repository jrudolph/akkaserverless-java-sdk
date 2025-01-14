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

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.protocol.replicated_entity.ReplicatedEntityDelta

private[replicatedentity] object ReplicatedEntityDeltaTransformer {

  def create(delta: ReplicatedEntityDelta, anySupport: AnySupport): InternalReplicatedData = {
    val entity = delta.delta match {
      case ReplicatedEntityDelta.Delta.Gcounter(_) =>
        new GCounterImpl
      case ReplicatedEntityDelta.Delta.Pncounter(_) =>
        new PNCounterImpl
      case ReplicatedEntityDelta.Delta.Gset(_) =>
        new GSetImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Orset(_) =>
        new ORSetImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Flag(_) =>
        new FlagImpl
      case ReplicatedEntityDelta.Delta.Lwwregister(_) =>
        new LWWRegisterImpl[Any](anySupport)
      case ReplicatedEntityDelta.Delta.Ormap(_) =>
        new ORMapImpl[Any, InternalReplicatedData](anySupport)
      case ReplicatedEntityDelta.Delta.Vote(_) =>
        new VoteImpl
    }
    entity.applyDelta(delta.delta)
    entity
  }

}
