package cn.com.sparkle.firefly.util;

import java.util.Comparator;

import cn.com.sparkle.firefly.stablestorage.model.StoreModel.IdOrBuilder;

public class IdComparator implements Comparator<IdOrBuilder> {
	private final static IdComparator instance = new IdComparator();

	public static IdComparator getInstance() {
		return instance;
	}

	private IdComparator() {
	}

	@Override
	public int compare(IdOrBuilder o1, IdOrBuilder o2) {
		int result;
		if (o1.getIncreaseId() == o2.getIncreaseId()) {
			result = o1.getAddress().compareTo(o2.getAddress());
			if (result != 0) {
				result = result > 0 ? 1 : -1;
			}
		} else {
			result = o1.getIncreaseId() > o2.getIncreaseId() ? 1 : -1;
		}

		return result;
	}
}
