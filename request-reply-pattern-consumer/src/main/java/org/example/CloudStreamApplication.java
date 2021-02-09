package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;

@EnableBinding({CloudStreamApplication.CloudStreamChannels.class})
@SpringBootApplication
public class CloudStreamApplication {

    interface CloudStreamChannels {

        String TO_UPPERCASE_REPLY = "to-uppercase-reply";
        String TO_UPPERCASE_REQUEST = "to-uppercase-request";

        @Output(TO_UPPERCASE_REPLY)
        SubscribableChannel toUppercaseReply();

        @Input(TO_UPPERCASE_REQUEST)
        MessageChannel toUppercaseRequest();
    }

    public static void main(String[] args) {
        SpringApplication.run(CloudStreamApplication.class, args);
    }

    @StreamListener(CloudStreamChannels.TO_UPPERCASE_REQUEST)
    @SendTo(CloudStreamChannels.TO_UPPERCASE_REPLY)
    public Message<?> process(Message<String> request) {
        Message<String> message = MessageBuilder.withPayload(request.getPayload().toUpperCase()).copyHeaders(request.getHeaders()).build();
        return message;
    }

}