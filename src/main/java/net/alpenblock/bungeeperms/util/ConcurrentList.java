package net.alpenblock.bungeeperms.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import lombok.SneakyThrows;

public class ConcurrentList<E> extends ArrayList<E>
{
    private final Object lock=new Object();
    
    @Override
    public boolean add(E e)
    {
        synchronized(lock)
        {
            return super.add(e);
        }
    }
    @Override
    public void add(int index, E element) 
    {
        synchronized(lock)
        {
            super.add(index, element);
        }
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) 
    {
        synchronized(lock)
        {
            return super.addAll(c);
        }
    }
    @Override
    public boolean addAll(int index, Collection<? extends E> c)
    {
        synchronized(lock)
        {
            return super.addAll(index, c);
        }
    }
    
    @Override
    public void clear() 
    {
        synchronized(lock)
        {
            super.clear();
        }
    }
    @Override
    @SneakyThrows
    public Object clone() 
    {
        synchronized(lock)
        {
            ConcurrentList<E> clist=(ConcurrentList<E>)super.clone();
            clist.modCount=0;
            Field f=ArrayList.class.getDeclaredField("elementData");
            f.setAccessible(true);
            f.set(clist, Arrays.copyOf((Object[])f.get(this), this.size()));
            
            return clist;
        }
    }
    @Override
    public boolean contains(Object o) 
    {
        synchronized(lock)
        {
            return super.contains(o);
        }
    }
    @Override
    public void ensureCapacity(int minCapacity) 
    {
        synchronized(lock)
        {
            super.ensureCapacity(minCapacity);
        }
    }
    @Override
    public E get(int index) 
    {
        synchronized(lock)
        {
            return super.get(index);
        }
    }
    @Override
    public int indexOf(Object o) 
    {
        synchronized(lock)
        {
            return super.indexOf(o);
        }
    }
    @Override
    public int lastIndexOf(Object o) 
    {
        synchronized(lock)
        {
            return super.lastIndexOf(o);
        }
    }
    @Override
    public E remove(int index) 
    {
        synchronized(lock)
        {
            return super.remove(index);
        }
    }
    @Override
    public boolean remove(Object o) 
    {
        synchronized(lock)
        {
            return super.remove(o);
        }
    }
    @Override
    public boolean removeAll(Collection<?> c) 
    {
        synchronized(lock)
        {
            return super.removeAll(c);
        }
    }
    @Override
    public boolean retainAll(Collection<?> c) 
    {
        synchronized(lock)
        {
            return super.retainAll(c);
        }
    }
    @Override
    public E set(int index, E element) 
    {
        synchronized(lock)
        {
            return super.set(index, element);
        }
    }
    @Override
    public List<E> subList(int fromIndex, int toIndex) 
    {
        synchronized(lock)
        {
            return super.subList(fromIndex, toIndex);
        }
    }
    @Override
    public Object[] toArray() 
    {
        synchronized(lock)
        {
            return super.toArray();
        }
    }
    @Override
    public <T> T[] toArray(T[] a) 
    {
        synchronized(lock)
        {
            return super.toArray(a);
        }
    }
    @Override
    public void trimToSize() 
    {
        synchronized(lock)
        {
            super.trimToSize();
        }
    }
    
    @Override
    public ListIterator<E> listIterator() 
    {
        return new ListItr(0);
    }

    @Override
    public Iterator<E> iterator() 
    {
        return new Itr();
    }
    
    
    
    
    @SneakyThrows
    private Object[] getElementData()
    {
        Field f=ArrayList.class.getDeclaredField("elementData");
        f.setAccessible(true);
        return (Object[]) f.get(this);
    }
    @SneakyThrows
    private void setElementData(Object[] elementData)
    {
        Field f=ArrayList.class.getDeclaredField("elementData");
        f.setAccessible(true);
        f.set(this, elementData);
    }
    
    
    private class Itr implements Iterator<E> 
    {
        protected int cursor;
        protected int lastRet;
        final ConcurrentList l;
        public Itr()
        {
            cursor = 0;
            lastRet = -1;
            l=(ConcurrentList) ConcurrentList.this.clone();
        }
        

        @Override
        public boolean hasNext() 
        {
            return cursor < l.size();
        }

        @Override
        public E next() 
        {
            int i = cursor;
            if (i >= l.size())
            {
                throw new NoSuchElementException();
            }
            cursor = i + 1;
            return (E) l.get(lastRet = i);
        }

        @Override
        public void remove() 
        {
            if (lastRet < 0)
            {
                throw new IllegalStateException();
            }

            l.remove(lastRet);
            ConcurrentList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }

    public class ListItr extends Itr implements ListIterator<E> 
    {
        ListItr(int index) 
        {
            super();
            cursor = index;
        }

        @Override
        public boolean hasPrevious() 
        {
            return cursor > 0;
        }

        @Override
        public int nextIndex() 
        {
            return cursor;
        }

        @Override
        public int previousIndex()
        {
            return cursor - 1;
        }

        @Override
        public E previous()
        {
            int i = cursor - 1;
            if (i < 0)
            {
                throw new NoSuchElementException();
            }
            cursor = i;
            return (E) l.get(lastRet = i);
        }

        @Override
        public void set(E e) 
        {
            if (lastRet < 0)
            {
                throw new IllegalStateException();
            }

            l.set(lastRet, e);
            ConcurrentList.this.set(lastRet, e);
        }

        @Override
        public void add(E e) 
        {
            int i = cursor;
            l.add(i, e);
            ConcurrentList.this.add(i,e);
            cursor = i + 1;
            lastRet = -1;
        }
    }
}
