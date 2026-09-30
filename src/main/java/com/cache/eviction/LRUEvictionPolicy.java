package com.cache.eviction;

import com.cache.model.CacheEntry;
import java.util.HashMap;
import java.util.Map;

/**
 * O(1) LRU Eviction Policy using a Doubly Linked List.
 * Inspired by Caffeine's use of O(1) access-order queues for efficiency.
 */
public class LRUEvictionPolicy implements EvictionPolicy {

    private static class Node {
        String key;
        Node prev;
        Node next;
        Node(String key) {
            this.key = key;
        }
    }

    private final Map<String, Node> map = new HashMap<>();
    private final Node head = new Node(null);
    private final Node tail = new Node(null);

    public LRUEvictionPolicy() {
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public void onPut(CacheEntry entry) {
        // If it exists, remove it first (should not happen if CacheManager calls onRemove before onPut for replacements)
        if (map.containsKey(entry.getKey())) {
            onRemove(entry);
        }
        Node node = new Node(entry.getKey());
        map.put(entry.getKey(), node);
        addToTail(node);
    }

    @Override
    public void onGet(CacheEntry entry) {
        Node node = map.get(entry.getKey());
        if (node != null) {
            removeNode(node);
            addToTail(node);
        }
    }

    @Override
    public void onRemove(CacheEntry entry) {
        Node node = map.remove(entry.getKey());
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public String evict() {
        if (head.next == tail) {
            return null;
        }
        Node victim = head.next;
        removeNode(victim);
        map.remove(victim.key);
        return victim.key;
    }

    @Override
    public void clear() {
        map.clear();
        head.next = tail;
        tail.prev = head;
    }

    private void addToTail(Node node) {
        Node prev = tail.prev;
        prev.next = node;
        node.prev = prev;
        node.next = tail;
        tail.prev = node;
    }

    private void removeNode(Node node) {
        Node prev = node.prev;
        Node next = node.next;
        if (prev != null) prev.next = next;
        if (next != null) next.prev = prev;
    }
}
