/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 *
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.serena.air

class MultiMap<K, V> implements Map<K, List<V>>{
    @Delegate
    private Map<K, List<V>> map

    MultiMap(){
        map = [:]
    }
    MultiMap(Map map){
        this.map = map
    }

    @Override
    public List<V> put(K key, List<V> value){
        map.put(key, value)
    }

    public V put(K key, V value){
        if(value instanceof List){
            return map.put(key, value)
        }

        List list = get(key)
        list << value
        return list
    }

    public V getSingleValue(K key) {
        List<V> value = map.get(key)
        if (value != null && value.size() > 0){
            return value.get(0)
        }
        return null
    }

    @Override
    public List<V> get(Object key) {
        List<V> value = map.get(key)
        if (value == null){
            value = []
            map.put(key, value)
        }
        return value
    }
}