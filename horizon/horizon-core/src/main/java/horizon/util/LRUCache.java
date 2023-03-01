/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.util;

import java.util.LinkedHashMap;
import java.util.Map;

import horizon.base.AbstractComponent;

/**Excerpted from the LRUCache class written by Christian d'Heureuse.
 * Author: Christian d'Heureuse (<a href="http://www.source-code.biz/snippets/java/6.htm">www.source-code.biz</a>)<br>
 * License: <a href="http://www.gnu.org/licenses/lgpl.html">LGPL</a>.<br>
 * Modified by Emjay Khan
 */
public class LRUCache<K,V> extends AbstractComponent {
	protected static final float LOAD_FACTOR = 0.75f;

	protected int capacity;
	private final LinkedHashMap<K,V> map;

	public LRUCache (int capacity) {
	   this.capacity = capacity;
	   map = getMap();
	}

	protected LinkedHashMap<K, V> getMap() {
	   int hashTableCapacity = (int)Math.ceil(capacity / LOAD_FACTOR) + 1;
	   return new LinkedHashMap<K,V>(hashTableCapacity, LOAD_FACTOR, true) {
		      private static final long serialVersionUID = 1;

		      @Override
		      protected boolean removeEldestEntry (Map.Entry<K,V> eldest) {
		         return LRUCache.this.size() > LRUCache.this.capacity;
		      }
		   };
	}

	public synchronized V get (K key) {
	   return map.get(key);
	}

	public synchronized void put (K key, V value) {
	   map.put (key,value);
	}

	public synchronized int size() {
		return map.size();
	}
}