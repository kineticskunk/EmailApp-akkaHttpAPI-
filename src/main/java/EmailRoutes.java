import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import akka.stream.impl.Timers;

import javax.annotation.processing.Completion;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class EmailRoutes extends AllDirectives {

    final private ActorRef emailActor;
    final private LoggingAdapter log;


    public EmailRoutes(ActorSystem system, ActorRef emailActor) {
        this.emailActor = emailActor;
        log = Logging.getLogger(system, this);
    }

    Duration timeout = Duration.ofSeconds(60);



//    all routes
    public Route routes() {
        return route(pathPrefix("users", () ->
                        route(
                                getOrPostUsers(),
                                path(PathMatchers.segment(), name -> route(
                                        getUser(name),
                                        deleteUser(name)
                                        )
                                )
                        )
                ));

    }

    private Route getUser(String name) {
        return get(() -> {
           CompletionStage<Optional<EmailActor.User>> maybeUser = Patterns
                  .ask(emailActor, new emailMessages.GetUser(name), timeout)
                   .thenApply(Optional.class::cast);
                return onSuccess(() -> maybeUser,
                        performed -> {
                            if (performed.isPresent())
                                return complete(StatusCodes.OK, performed.get(), Jackson.marshaller());

                            else
                                return complete(StatusCodes.NOT_FOUND);
                        }
                        );
        });
    }

    private Route deleteUser(String name) {
        return
                delete(() -> {
                    CompletionStage<emailMessages.ActionPerformed> userDeleted = Patterns
                            .ask(emailActor, new emailMessages.DeleteUser(name), timeout)
                            .thenApply(emailMessages.ActionPerformed.class::cast);
                    return onSuccess(() -> userDeleted,
                            performed -> {
                                log.info("Deleted user[{}]: {}", name, performed.getDescription());
                                return complete(StatusCodes.OK, performed, Jackson.marshaller());
                            });
                });
    }
    private Route getOrPostUsers() {
        return pathEnd(() ->
                route(
                        get(() -> {
                            CompletionStage<EmailActor.Users> futureUsers = Patterns
                                    .ask(emailActor, new emailMessages.GetUsers(), timeout)
                                    .thenApply(EmailActor.Users.class::cast);
                            return onSuccess(() -> futureUsers,
                                    users -> complete(StatusCodes.OK, users, Jackson.marshaller()));
                        }),
                        post(() ->
                                entity(
                                        Jackson.unmarshaller(EmailActor.User.class),
                                        user -> {
                                            CompletionStage<emailMessages.ActionPerformed> userCreated = Patterns
                                                    .ask(emailActor, new emailMessages.CreateUser(user), timeout)
                                                    .thenApply(emailMessages.ActionPerformed.class::cast);
                                            return onSuccess(() -> userCreated,
                                                    performed -> {
                                                        log.info("Created user [{}]: {}", user.getName(), performed.getDescription());
                                                        return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                                                    });
                                        }))
                )
        );
    }


}


