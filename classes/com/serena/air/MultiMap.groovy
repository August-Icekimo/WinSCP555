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