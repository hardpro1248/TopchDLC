package gg.topchdlc.vse.utils.other;

import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@UtilityClass
public class JavaUtility {
    public <T> void sortedInsert(List<T> list, T item, Comparator<T> cmp) {
        int index = Collections.binarySearch(list, item, cmp);
        if (index < 0) index = ~index;
        list.add(index, item);
    }

    public <T> T join(Future<T> future) {
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            LogUtility.error(e, "joining future");
            return null;
        }
    }

    public void joinAll(CompletableFuture<?>... futures) {
        join(CompletableFuture.allOf(futures));
    }
}
