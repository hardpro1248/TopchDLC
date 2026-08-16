package gg.topchdlc.vse.utils.other;

import java.util.function.Supplier;

public record MultiBoolSupplier(Supplier<Boolean>[] suppliers) implements Supplier<Boolean> {
    @Override
    public Boolean get() {
        for (Supplier<Boolean> s: suppliers) {
            if (!s.get()) return false;
        }
        return true;
    }
}
