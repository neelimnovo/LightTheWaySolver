package searchLogic;

public class ShortQueue {
    private short[] elements;
    private int head;
    private int tail;
    private int size;

    public ShortQueue(int capacity) {
        elements = new short[capacity];
        head = 0;
        tail = 0;
        size = 0;
    }

    public void add(short e) {
        if (size == elements.length) {
            resize();
        }
        elements[tail] = e;
        tail = (tail + 1) % elements.length;
        size++;
    }

    public short remove() {
        if (size == 0) throw new IllegalStateException("Queue is empty");
        short e = elements[head];
        head = (head + 1) % elements.length;
        size--;
        return e;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }
    
    private void resize() {
        short[] newElements = new short[elements.length * 2];
        for (int i = 0; i < size; i++) {
            newElements[i] = elements[(head + i) % elements.length];
        }
        elements = newElements;
        head = 0;
        tail = size;
    }
}
