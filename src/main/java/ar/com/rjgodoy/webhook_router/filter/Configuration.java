package ar.com.rjgodoy.webhook_router.filter;

import ar.com.rjgodoy.webhook_router.WebHook;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Configuration {

  public static final String DEFAULT_QUEUE = "default";

  private final Map<String, QueueDecl> queues = new LinkedHashMap<>();

  public Configuration(OrSequence configuration) {
    queues.put(DEFAULT_QUEUE, null);
    Predicate<Directive> isQueueDecl = QueueDecl.class::isInstance;

    configuration.getDirectives().stream()
      .filter(isQueueDecl)
      .map(QueueDecl.class::cast)
      .forEach(this::declare);

    List<Directive> directives =
        configuration.getDirectives().stream().filter(isQueueDecl.negate()).toList();
    if (!queues.containsKey(DEFAULT_QUEUE)) {
      declare(new QueueDecl(DEFAULT_QUEUE, null, null, null, DirectiveParser.wrap(directives))); // Added null for combinator
    } else if (!directives.isEmpty()) {
      throw new RuntimeParserException(1,
          "The definition of 'QUEUE default' requires the entire configuration to contain only queue declarations");
    }

  }

  private void declare(QueueDecl queue) {
    queues.put(queue.getName(), queue);
  }

  public Collection<QueueDecl> getQueues() {
    return queues.values();
  }

  void makeDry() {
    for (var e : queues.entrySet()) {
      Directive body = e.getValue().getBody();
      if (body != null) {
        body = new AndSequence(List.of(new DryAction(), body));
        e.setValue(new QueueDecl(e.getValue(), body));
      }
    }
  }

  public Result call(String queueName, WebHook webHook) {
    return queues.get(queueName).call(webHook);
  }

}
