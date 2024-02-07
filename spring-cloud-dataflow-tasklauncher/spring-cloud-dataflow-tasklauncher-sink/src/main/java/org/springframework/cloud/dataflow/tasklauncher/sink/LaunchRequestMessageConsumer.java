package org.springframework.cloud.dataflow.tasklauncher.sink;

import java.util.function.Consumer;

import org.springframework.cloud.dataflow.tasklauncher.LaunchRequest;
import org.springframework.messaging.Message;

interface LaunchRequestMessageConsumer extends Consumer<Message<LaunchRequest>> {

}
