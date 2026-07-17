package io.obya.api.onboarding;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class Application {
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RegistryEventConsumer<CircuitBreaker> myRegistryEventConsumer() {

        return new RegistryEventConsumer<>() {
           @Override
           public void onEntryAddedEvent(@NonNull EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
              entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
           }

           @Override
           public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
              entryRemoveEvent.getRemovedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
           }

           @Override
           public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
               entryReplacedEvent.getNewEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
           }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {

        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(@NonNull EntryAddedEvent<Retry> entryAddedEvent) {
                entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
            }

            @Override
            public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<Retry> entryRemoveEvent) {
                entryRemoveEvent.getRemovedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
            }

            @Override
            public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<Retry> entryReplacedEvent) {
                entryReplacedEvent.getNewEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
            }
        };
    }

}
