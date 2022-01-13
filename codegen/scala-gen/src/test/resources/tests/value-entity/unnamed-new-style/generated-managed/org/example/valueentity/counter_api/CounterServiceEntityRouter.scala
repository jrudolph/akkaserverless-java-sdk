package org.example.valueentity.counter_api

import com.akkaserverless.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound
import com.akkaserverless.scalasdk.impl.valueentity.ValueEntityRouter
import com.akkaserverless.scalasdk.valueentity.CommandContext
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import org.example.valueentity.counter_api
import org.example.valueentity.domain.counter_domain.CounterState

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A value entity handler that is the glue between the Protobuf service <code>CounterService</code>
 * and the command handler methods in the <code>Counter</code> class.
 */
class CounterServiceEntityRouter(entity: CounterServiceEntity) extends ValueEntityRouter[CounterState, CounterServiceEntity](entity) {
  def handleCommand(commandName: String, state: CounterState, command: Any, context: CommandContext): ValueEntity.Effect[_] = {
    commandName match {
      case "Increase" =>
        entity.increase(state, command.asInstanceOf[IncreaseValue])

      case "Decrease" =>
        entity.decrease(state, command.asInstanceOf[DecreaseValue])

      case _ =>
        throw new CommandHandlerNotFound(commandName)
    }
  }
}
