/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.dispatcher;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
/**
 *
 * Special Set that maintains the following semantics:
 * All elements that are un-ordered (do not implement {@link Ordered} interface or annotated {@link Order} annotation) 
 * will be stored in the order in which they were added, maintaining the semantics of the {@link LinkedHashSet}.
 * However, for all {@link Ordered} elements a {@link Comparator} (instantiated by default) for this implementation of {@link Set}, 
 * will be used. Those elements will have 
 * precedence over un-ordered elements. If elements have the same order but themselves do not equal to one another
 * they will be placed to the right (appended next to) of the element with the same order, thus preserving the order 
 * of the insertion and maintaining {@link LinkedHashSet} semantics.
 * 
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
class OrderedAwareLinkedHashSet<E> extends LinkedHashSet<E> {

	OrderComparator comparator = new OrderComparator();
	Lock lock = new ReentrantLock();
	/**
	 * Every time when Ordered element is added via this method
	 * this Set will be re-sorted, otherwise the element is simply added to the end of the stack.
	 * If adding multiple objects for performance reasons it is recommended to use
	 * addAll(Collection c) method which first adds all the elements to this set
	 * and then calls reinitializeThis() method.
	 * Added element must not be null;
	 */
	public  boolean add(E o){
		lock.lock();
		try {
			Assert.notNull(o,"Can not add NULL object");
			boolean present = false;
			if (o instanceof Ordered){
				present = this.reinitializeThis(o);
			} else {
				present = super.add(o);
			}
			return present;
		} finally {
			lock.unlock();
		}	
	}
	/**
	 * Adds all elements in this Collection and then resorts this set
	 * via call to the reinitializeThis() method
	 */
	public  boolean addAll(Collection<? extends E> c){
		lock.lock();
		try {
			Assert.notNull(c,"Can not merge with NULL set");
			for (E object : c) {		
				this.add(object);
			}		
			return true;
		} finally {
			lock.unlock();
		}
	}
	/**
	 * 
	 */
	public  boolean remove(Object o){
		lock.lock();
		try {
			return super.remove(o);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * 
	 */
	public boolean removeAll(Collection<?> c){
		if (CollectionUtils.isEmpty(c)){
			return false;
		}
		lock.lock();
		try {
			super.removeAll(c);	
			return true;
		} finally {
			lock.unlock();
		}
		
	}

	/**
	 * 
	 */
	private boolean reinitializeThis(Object adding){	
		boolean added = false;
		E[] tempUnorderedElements = (E[]) this.toArray();
		if (super.contains(adding)){
			return false;
		}
		super.clear();

		if (tempUnorderedElements.length == 0){
			added = super.add((E) adding);
		} else {
			Set tempSet = new LinkedHashSet();
			for (E current : tempUnorderedElements) {
				if (current instanceof Ordered && adding instanceof Ordered){
					if (((Ordered)adding).getOrder() < ((Ordered)current).getOrder()){
						added = super.add((E) adding);
						super.add(current);
					}  else {
						super.add(current);
					}
				} else {
					tempSet.add(current);
				}
			}
			if (!added){
				added = super.add((E) adding);
			}
			for (Object object : tempSet) {
				super.add((E) object);
			}
		}
		return added;
	}
	
}
