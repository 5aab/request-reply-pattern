package org.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.HeaderEnricherSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@EnableBinding({GatewayApplication.GatewayChannels.class})
@SpringBootApplication
public class GatewayApplication {

    public static final UUID instanceUUID = UUID.randomUUID();

    interface GatewayChannels {

        String TO_UPPERCASE_REPLY = "to-uppercase-reply";
        String TO_UPPERCASE_REQUEST = "to-uppercase-request";

        @Input(TO_UPPERCASE_REPLY)
        SubscribableChannel toUppercaseReply();

        @Output(TO_UPPERCASE_REQUEST)
        MessageChannel toUppercaseRequest();
    }

    @MessagingGateway
    public interface StreamGateway {
        @Gateway(requestChannel = ENRICH, replyChannel = FILTER)
        byte[] process(String payload);
    }

    private static final String ENRICH = "enrich";
    private static final String FILTER = "filter";

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public IntegrationFlow headerEnricherFlow() {
        return IntegrationFlows.from(ENRICH)
                .enrichHeaders(HeaderEnricherSpec::headerChannelsToString)
                .enrichHeaders(headerEnricherSpec -> headerEnricherSpec.header("instanceId",instanceUUID))
                .channel(GatewayChannels.TO_UPPERCASE_REQUEST).get();
    }

    @Bean
    public IntegrationFlow replyFiltererFlow() {
        return IntegrationFlows.from(GatewayChannels.TO_UPPERCASE_REPLY)
                .filter(Message.class, message -> instanceUUID.toString().equals(message.getHeaders().get("instanceId")))
                .channel(FILTER)
                .get();
    }

    @RestController
    public class UppercaseController {
        @Autowired
        StreamGateway gateway;

        @ResponseBody
        @PostMapping("/string/{string}")
        public String getUser(@PathVariable("string") String string) {
            String result = new String(gateway.process(string));
            log.info("Result is {}", result);
            return result;
        }
    }

}
