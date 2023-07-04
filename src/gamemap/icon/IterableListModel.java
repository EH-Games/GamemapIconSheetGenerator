package gamemap.icon;

import java.util.Iterator;

import javax.swing.DefaultListModel;

public class IterableListModel<E> extends DefaultListModel<E> implements Iterable<E> {

	@Override
	public Iterator<E> iterator() {
		// bumped up the java version just for this
		// could technically just implement it in the same manner, but w/e
		return elements().asIterator();
	}
}