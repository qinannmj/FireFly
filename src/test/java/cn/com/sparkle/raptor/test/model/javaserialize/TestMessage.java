package cn.com.sparkle.raptor.test.model.javaserialize;

import java.io.Serializable;

public class TestMessage  implements Serializable {
		private static final long serialVersionUID = 1L;
		private long id;
		private boolean isLast;
		private Serializable s;

		public TestMessage(long id, Serializable s, boolean isLast) {
			super();
			this.id = id;
			this.s = s;
			this.isLast = isLast;
		}

		public long getId() {
			return id;
		}

		public boolean isLast() {
			return isLast;
		}

		public Serializable getS() {
			return s;
		}
}
