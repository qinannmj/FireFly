package cn.com.sparkle.raptor.core.collections;

public class LastAccessTimeLinkedList<T> {
	private Entity<T> head = new Entity<T>(null);

	public LastAccessTimeLinkedList() {
		head.next = head;
		head.prev = head;
	}

	public void putOrMoveFirst(Entity<T> e) {
		// remove entity from link
		remove(e);
		// insert to head
		e.next = head.next;
		e.prev = head;
		e.next.prev = e;
		e.prev.next = e;

	}

	public Entity<T> getLast() {
		if (head.prev == head) {
			return null;
		}
		return head.prev;
	}

	public void remove(Entity<T> e) {
		e.prev.next = e.next;
		e.next.prev = e.prev;
		e.next = e;
		e.prev = e;
	}

	public static class Entity<T> {
		private Entity<T> prev = this;
		private Entity<T> next = this;
		private T element;

		public Entity(T t) {
			this.element = t;
		}

		public T getElement() {
			return element;
		}
	}
}
