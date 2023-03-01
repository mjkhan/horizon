package horizon.tutorial;

import horizon.base.AbstractComponent;
import horizon.data.DataList;

public class TutorialSupport extends AbstractComponent {
	protected void println(Object obj) {
		if (obj == null) return;
		System.out.println(obj.getClass().getName() + ": " + obj);
	}

	protected void println(Iterable<?> objs) {
		if (objs == null) return;

		if (objs instanceof DataList) {
			DataList<?> dataset = DataList.class.cast(objs);
			System.out.println(String.format(
				"size:%d, totalSize:%d, start:%d, fetchSize:%d",
				dataset.size(), dataset.getTotalSize(), dataset.getStart(), dataset.getFetchSize()
			));
		}

		for (Object obj: objs)
			println(obj);
	}
}