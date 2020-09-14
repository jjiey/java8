/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.sun.corba.se.impl.orbutil ;

import java.util.ArrayList ;

/** Utility for managing mappings from densely allocated integer
 * keys to arbitrary objects.  This should only be used for
 * keys in the range 0..max such that "most" of the key space is actually
 * used.
 *
 * 用于 管理从 密集分配的整型 key 到任意对象 的映射 的实用程序。
 * 这应该只用于 keys 的范围在 0..max 之间 such that “大部分”的 key 空间被实际使用。
 */
public class DenseIntMapImpl
{
    private ArrayList list = new ArrayList() ;

    private void checkKey( int key )
    {
        if (key < 0)
            throw new IllegalArgumentException( "Key must be >= 0." ) ;
    }

    /** If key >= 0, return the value bound to key, or null if none.
     * Throws IllegalArgumentException if key <0.
     */
    public Object get( int key )
    {
        checkKey( key ) ;

        Object result = null ;
        if (key < list.size())
            result = list.get( key ) ;

        return result ;
    }

    /** If key >= 0, bind value to the key.
     * Throws IllegalArgumentException if key <0.
     */
    public void set( int key, Object value )
    {
        checkKey( key ) ;
        extend( key ) ;
        list.set( key, value ) ;
    }

    private void extend( int index )
    {
        if (index >= list.size()) {
            list.ensureCapacity( index + 1 ) ;
            int max = list.size() ;
            while (max++ <= index)
                list.add( null ) ;
        }
    }
}
