package test.mutiny.subscription;

import static io.smallrye.mutiny.subscription.Subscribers.NO_ON_FAILURE;

import io.smallrye.mutiny.subscription.Subscribers.CallbackBasedSubscriber;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.MultipleFailuresError;
import org.reactivestreams.Subscription;

public class AssertBooleanSubscriber<T> extends CallbackBasedSubscriber<T> {

    private Assert assertions;
    private boolean soft = false;
    private Function<T, Boolean> item;
    final List<Throwable> listExceptions = new LinkedList<>();
    final Map<T, Boolean> results = new HashMap<>();
    private static final Consumer DEFAULT = (item) -> {
        return;
    };

    public AssertBooleanSubscriber(
            boolean soft,
            Assert assertions,
            Function<T, Boolean> item) {
        this(DEFAULT, NO_ON_FAILURE, null, s -> s.request(Long.MAX_VALUE));
        this.item = item;
        this.soft = soft;
        this.assertions = assertions;
    }

    public AssertBooleanSubscriber(
            boolean soft,
            Assert assertions,
            Function<T, Boolean> item,
            Consumer<? super Throwable> onFailure) {
        this(DEFAULT, onFailure, null, s -> s.request(Long.MAX_VALUE));
        this.item = item;
        this.soft = soft;
        this.assertions = assertions;
    }

    public AssertBooleanSubscriber(
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure, Runnable onCompletion,
            Consumer<? super Subscription> onSubscription) {
        super(onItem, onFailure, onCompletion, onSubscription);
    }

    @Override
    public void onItem(T item) {
        try {
            super.onItem(item);
            Boolean result = this.item.apply(item);
            results.put(item, result);
            assertions.claim(result);
        } catch (Throwable e) {
            results.put(item, false);
            if (soft) {
                listExceptions.add(e);
            } else {
                cancel();
                listExceptions.add(e);
                throw e;
            }
        }
    }

    public void assertFailed() {
        fail(listExceptions);
    }

    public Map<T, Boolean> getResults() {
        return results;
    }

    private void fail(List<Throwable> failures) {
        if (!failures.isEmpty()) {
            MultipleFailuresError multipleFailuresError = new MultipleFailuresError(null, failures);
            failures.forEach(multipleFailuresError::addSuppressed);
            throw multipleFailuresError;
        }
    }

    public enum Assert {
        TRUE(Assertions::assertTrue),
        FALSE(Assertions::assertFalse);

        private final Consumer<Boolean> action;

        Assert(Consumer<Boolean> o) {
            this.action = o;
        }

        public void claim(Boolean value) {
            action.accept(value);
        }
    }
}