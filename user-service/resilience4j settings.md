
## 🔑 Easy Way To Remember
| Setting                    | Meaning                            |
| -------------------------- | ---------------------------------- |
| ignore-exceptions          | Never count these exceptions       |
| record-exceptions          | Always count these exceptions      |
| ignore-exception-predicate | Dynamic rule to ignore exceptions  |
| record-result-predicate    | Treat certain responses as failure |
| record-failure-predicate   | Custom logic to decide failures    |

### @TimeLimiter expects the method to return an asynchronous type such as CompletionStage<T> or Future<T> (e.g., CompletableFuture<T>), not a plain synchronous object.
