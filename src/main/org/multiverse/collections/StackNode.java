package org.multiverse.collections;

import org.multiverse.api.TmEntity;

@TmEntity
public final class StackNode<E> {
    protected StackNode<E> next;
    protected E value;

    public StackNode(StackNode<E> next, E value) {
        this.next = next;
        this.value = value;
    }
}