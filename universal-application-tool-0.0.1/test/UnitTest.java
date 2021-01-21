import akka.actor.ActorSystem;
import controllers.AsyncController;
import controllers.CountController;
import controllers.PostgresController;
import models.Person;
import org.junit.Test;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.PersonRepository;
import scala.concurrent.ExecutionContextExecutor;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.contentAsString;

/**
 * Unit testing does not require Play application start up.
 *
 * <p>https://www.playframework.com/documentation/latest/JavaTest
 */
public class UnitTest {

  @Test
  public void simpleCheck() {
    int a = 1 + 1;
    assertThat(a).isEqualTo(2);
  }

  // Unit test a controller
  @Test
  public void testCount() {
    final CountController controller = new CountController(() -> 49);
    Result result = controller.count();
    assertThat(contentAsString(result)).isEqualTo("49");
  }

  // Unit test a controller with async return
  @Test
  public void testAsync() {
    final ActorSystem actorSystem = ActorSystem.create("test");
    try {
      final ExecutionContextExecutor ec = actorSystem.dispatcher();
      final AsyncController controller = new AsyncController(actorSystem, ec);
      final CompletionStage<Result> future = controller.message();

      // Block until the result is completed
      await()
          .untilAsserted(
              () ->
                  assertThat(future.toCompletableFuture())
                      .isCompletedWithValueMatching(
                          result -> contentAsString(result).equals("Hi!")));
    } finally {
      actorSystem.terminate();
    }
  }

  // Unit test Postgres controller
  @Test
  public void testPostgresList() {
    HttpExecutionContext ec = new HttpExecutionContext(ForkJoinPool.commonPool());
    PersonRepository repository = mock(PersonRepository.class);
    Person p = new Person();
    p.id = 1L;
    p.name = "Alice";
    when(repository.list()).thenReturn(supplyAsync(() -> Set.of(p)));
    final PostgresController controller = new PostgresController(repository, ec);
    final CompletionStage<Result> future = controller.list();

    // Block until the result is completed
    await()
        .untilAsserted(
            () ->
                assertThat(future.toCompletableFuture())
                    .isCompletedWithValueMatching(
                        result -> contentAsString(result).equals("1: Alice\n")));
  }

  // Unit test Postgres controller
  @Test
  public void testPostgresAdd() {
    HttpExecutionContext ec = new HttpExecutionContext(ForkJoinPool.commonPool());
    PersonRepository repository = mock(PersonRepository.class);
    when(repository.insert(any(Person.class))).thenReturn(supplyAsync(() -> 1234L));
    final PostgresController controller = new PostgresController(repository, ec);
    final CompletionStage<Result> future = controller.add("John");

    // Block until the result is completed
    await()
        .untilAsserted(
            () ->
                assertThat(future.toCompletableFuture())
                    .isCompletedWithValueMatching(
                        result ->
                            contentAsString(result).equals("person John with ID: 1234 added.")));
  }
}
