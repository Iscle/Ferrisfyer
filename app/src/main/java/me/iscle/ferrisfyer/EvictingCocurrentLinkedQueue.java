package me.iscle.ferrisfyer;

import java.util.concurrent.ConcurrentLinkedQueue;

public class EvictingCocurrentLinkedQueue<E> extends ConcurrentLinkedQueue<E> {
    private final int limit;

    public EvictingCocurrentLinkedQueue(int limit) {
        super();
        this.limit = limit;
    }

    @Override
    public boolean add(E e) {
        if (super.add(e)) {
            if (size() > limit) {
                remove();
            }
            return true;
        }

        return false;
    }
}
