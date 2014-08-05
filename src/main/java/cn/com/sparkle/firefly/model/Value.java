package cn.com.sparkle.firefly.model;

import java.util.Iterator;
import java.util.LinkedList;

import cn.com.sparkle.firefly.addprocess.AddRequestPackage;

public class Value implements Iterable<Value.IterElement> {
	private ValueType valueType;
	private LinkedList<AddRequestPackage> addPackage = null; //just for transport message by pipe in jvm
	private byte[] valuebytes;
	private int valueoffset = 0;

	public Value(ValueType valueType, int bytesCapacity) {
		this.valuebytes = new byte[bytesCapacity];
		this.valueType = valueType;
	}

	public Value(ValueType valueType, byte[] valuebytes) {
		this.valueType = valueType;
		this.valuebytes = valuebytes;
	}

	public Value(ValueType valueType, byte[] valuebytes, int length) {
		this(valueType, valuebytes);
		this.valueoffset = length;
	}

	public static enum ValueType {
		ADMIN(0), COMM(1);
		private int value;

		private ValueType(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}

		public static ValueType getValueType(int type) {
			switch (type) {
			case 0:
				return ADMIN;
			case 1:
				return COMM;
			default:
				throw new RuntimeException("unsupported type:" + type);
			}
		}
	}

	public byte[] getValuebytes() {
		return valuebytes;
	}

	public int length() {
		return valueoffset;
	}

	public ValueType getValueType() {
		return valueType;
	}

	public LinkedList<AddRequestPackage> getAddPackage() {
		return addPackage;
	}

	public void setAddPackage(LinkedList<AddRequestPackage> addPackage) {
		this.addPackage = addPackage;
	}

	

	private void writeVariableLength(int length) {
		while (true) {
			if ((length & ~0x7FL) == 0) {
				valuebytes[valueoffset++] = (byte) length;
				break;
			} else {
				valuebytes[valueoffset++] = (byte) (((int) length & 0x7F) | 0x80);
				length >>>= 7;
			}
		}
	}

	public void add(byte[] b) {
		add(b, 0, b.length);
	}
	
	public void add(byte[] b, int offset, int length) {
		writeVariableLength(b.length);
		System.arraycopy(b, offset, valuebytes, valueoffset, length);
		valueoffset += length;
	}
	
	public void fill(byte[] b){
		fill(b,0,b.length);
	}
	public void fill(byte[] b,int offset,int length){
		System.arraycopy(b, offset, valuebytes, valueoffset, length);
		valueoffset += length;
	}

	private final static class ValueIterator implements Iterator<IterElement> {
		private byte[] bytes;
		private int offset = 0;
		private IterElement next;
		private IterElement result = new IterElement();
		private int length;

		public ValueIterator(byte[] bytes,int length) {
			this.bytes = bytes;
			this.length = length;
			int nextSize = readVariableLength();
			if (nextSize >= 0) {
				next = new IterElement();
				next.offset = offset;
				next.size = nextSize;
				offset += nextSize;
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public IterElement next() {
			if (next != null) {
				//copy to result from next
				result.offset = next.offset;
				result.size = next.size;
				//find next
				int nextSize = readVariableLength();
				if (nextSize >= 0) {
					next.offset = offset;
					next.size = nextSize;
					offset += nextSize;
				} else {
					next = null;
				}

				return result;
			} else {
				return null;
			}
		}

		@Override
		public void remove() {
			throw new RuntimeException("unsupported operation!");
		}

		private int readVariableLength() {
			int length = 0;
			for (int i = 0; i < 5; i++) {
				if (offset >= this.length) {
					return -1;
				}
				length |= (bytes[offset++] & 0x7f) << (7 * i);
				if (length >= 0) {
					break;
				}
			}
			return length;
		}
	}

	public final static class IterElement {
		private int offset;
		private int size;

		public int getOffset() {
			return offset;
		}

		public int getSize() {
			return size;
		}

	}

	@Override
	public Iterator<IterElement> iterator() {
		return new ValueIterator(valuebytes,valueoffset);
	}

	public static void main(String[] args) {
		Value v = new Value(ValueType.COMM, 1024 * 1024);
		v.add(new byte[] { 24 });
		v.add(new byte[] { 25 });
		v.add(new byte[] { 26 });
		v.add(new byte[] { 27, 28, 29, 30, 31, 32, 32, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 22, 2, 2, 2, 2, 2, 2, 2, 4 });
		Iterator<IterElement> iter = v.iterator();
		while (iter.hasNext()) {
			IterElement ie = iter.next();
			for (int i = ie.getOffset(); i < ie.getOffset() + ie.getSize(); ++i) {
				System.out.print(v.getValuebytes()[i] + " ");
			}
			System.out.println();
		}

		v = new Value(v.getValueType(), v.getValuebytes(), v.length());
		iter = v.iterator();
		while (iter.hasNext()) {
			IterElement ie = iter.next();
			for (int i = ie.getOffset(); i < ie.getOffset() + ie.getSize(); ++i) {
				System.out.print(v.getValuebytes()[i] + " ");
			}
			System.out.println();
		}
		System.out.println(v.length());
	}

}
