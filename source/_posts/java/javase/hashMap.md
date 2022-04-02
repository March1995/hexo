---
title: JDK1.8 HashMap源码分析
date: 2019-01-08 
desc: JDK1.8 HashMap源码分析
keywords: hashMap
categories: [JavaSE]
---
[Hashmap的结构，1.7和1.8有哪些区别，史上最深入的分析](https://blog.csdn.net/qq_36520235/article/details/82417949)
[jdk1.7死环](https://www.cnblogs.com/Hangtutu/p/9251999.html)
[jdk1.7线程不安全](https://juejin.im/post/5a66a08d5188253dc3321da0)
[jdk1.8扩容核心-链表复制处理](https://blog.csdn.net/u013494765/article/details/77837338)
[HashMap1.8面试连问](https://mp.weixin.qq.com/s/NvmqdPOyWQdeHG2s-xSbHg)
[HashMap1.8源码解析：resize方法（有问题）](https://blog.csdn.net/weixin_42340670/article/details/80503517?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromBaidu-3&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromBaidu-3)

# 继承关系

![hashMap继承关系](/uploads/java/javase/hashMap.png)

# 成员变量
    
    // Hash表结构
    transient Node<K,V>[] table;
    
    // 保证 fail-fast机制
    transient int modCount;
     
    // 下一次扩容时的阈值 (capacity * load factor).
    int threshold;
    
    // 负载因子，决定hash表的数据填充程度，此值越大说明hash表填充的越满，空间利用率高，但是增加了查询开销，此值若太小，hash表空间利用率不高且rehash更加频繁
    final float loadFactor;    
    
    // 默认初始容量,必须是2的次方
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    
    // 最大容量，2的30次方，因为int类型32位，去掉一位符号位，只能左移30位
    static final int MAXIMUM_CAPACITY = 1 << 30;
    
    // 默认负载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    // jdk1.8增加 链表转红黑树的阈值
    static final int TREEIFY_THRESHOLD = 8;
    
    // jdk1.8增加 红黑树转链表的阈值
    static final int UNTREEIFY_THRESHOLD = 6;
    
# 构造函数
HashMap一共有4个构造方法，主要的工作就是完成容量和加载因子的赋值。Hash表都是采用的**懒加载**方式，当第一次插入数据时才会创建。

     // 指定默认容量和负载因子   
     public HashMap(int initialCapacity, float loadFactor) {
             if (initialCapacity < 0)
                 throw new IllegalArgumentException("Illegal initial capacity: " +
                                                    initialCapacity);
             if (initialCapacity > MAXIMUM_CAPACITY)
                 initialCapacity = MAXIMUM_CAPACITY;
             if (loadFactor <= 0 || Float.isNaN(loadFactor))
                 throw new IllegalArgumentException("Illegal load factor: " +
                                                    loadFactor);
             this.loadFactor = loadFactor;
             // 找到大于等于initialCapacity的最小的2的幂  若传入6 threshold=8
             this.threshold = tableSizeFor(initialCapacity);
     }
     
     // 使用默认负载因子
     public HashMap(int initialCapacity) {
             this(initialCapacity, DEFAULT_LOAD_FACTOR);
     }
     
     // 参数全部都是默认值
     public HashMap() {
            this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
     }
     
     // 构造一个新的 HashMap与指定的相同的映射 Map 。 
     public HashMap(Map<? extends K, ? extends V> m) {
             this.loadFactor = DEFAULT_LOAD_FACTOR;
             putMapEntries(m, false);
     }
     
# 基本操作
## 添加一个元素put(K k,V v)
HashMap允许K和V都为null，添加一个键值对时使用put方法，如果之前已经存在K的键值，那么旧值将会被新值替换。

    // return 前一个值与key相关联 ，或null如果没有key的映射。 （A null返回也可以指示以前关联的地图null与key。 
    public V put(K key, V value) {
            return putVal(hash(key), key, value, false, true);
    }
    
    //先来看看hash方法，采用了Object的hash算法返回的是对象的内存地址,每个对象在内存中地址不一样，所以他们的hash值也不一样，不同的算法hashCode不同。
    // 这里使用右移操作是为了让高位也参与运算提高散射率。低16位与高16位异或作为key的最终hash值。
    // （h >>> 16，表示无符号右移16位，高位补0，任何数跟0异或都是其本身，因此key的hash值高16位不变。） 
    // 为什么要这么干呢？ 这个与HashMap中table下标的计算有关。n = table.length; index = （n-1） & hash;因为，table的长度都是2的幂，因此index仅与hash值的低n位有关，hash值的高位都被与操作置为0了。 
    // key为null 默认为0即数组第一个
    static final int hash(Object key) {
            int h;
            return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
    
    /**
     * @param hash key的Hash值 经过高位与地位的异或运算
     * @param key 
     * @param value 
     * @param onlyIfAbsent 如果是，则不要更改现有值
     * @param evict 如果为false，则表处于创建模式。
     * @return 返回旧值或者null值。
     */        
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                       boolean evict) {
            Node<K,V>[] tab; Node<K,V> p; int n, i;
            // 如果哈希表的长度为空 调用resize()扩容方法 n为表长度
            if ((tab = table) == null || (n = tab.length) == 0)
                n = (tab = resize()).length;
            // 如果当前数组下标为null值， (i = (n - 1) & hash]即计算数组下标)，数组当前下标新建一个节点，即为头结点
            if ((p = tab[i = (n - 1) & hash]) == null)
                tab[i] = newNode(hash, key, value, null);
            // 数组上该位置有值,桶处有节点，发生hash冲突
            else {
                Node<K,V> e; K k;
                // 如果头结点的值与添加的值一致，hash冲突后，key的值也一致，进行替换。将e指向p
                if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                    e = p;
                // 如果与头节点不同，并且该桶目前已经是红黑树状态，调用putTreeVal()方法 
                else if (p instanceof TreeNode)
                    e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
                // 如果与头节点不同,桶中仍是链表阶段
                else {
                    // 循环遍历链表
                    for (int binCount = 0; ; ++binCount) {
                    // 讲e指向下一个节点，如果是null，说明该桶中只有头节点，直接添加到链表尾部即可
                        if ((e = p.next) == null) {
                            p.next = newNode(hash, key, value, null);
                            // 如果此时链表个数达到了8，那么需要将该桶处链表转换成红黑树，treeifyBin()方法将hash处的桶转成红黑树
                            if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                                treeifyBin(tab, hash);
                            break;
                        }
                        //如果与已有节点相同，跳出循环
                        if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                            break;
                        p = e;
                    }
                }
                // 存在重复key进行覆盖
                if (e != null) { // existing mapping for key
                    V oldValue = e.value;
                    if (!onlyIfAbsent || oldValue == null)
                        e.value = value;
                    // 子类实现 linkHashMap 
                    afterNodeAccess(e);
                    return oldValue;
                }
            }
            //是一个全新节点，那么size需要+1
            ++modCount;
            // 如果大小朝阳了阈值，需要扩容
            if (++size > threshold)
                resize();
            //子类实现    
            afterNodeInsertion(evict);
            return null;
    }
    
putVal()方法的流程：

- 1.若hash表为空，调用resize方法创建。
- 2.若桶的头结点为空，创建新结点放在数组中作为链表的头结点。
- 3.若头节点不为空，即发生hash冲突，取出头结点与当前值判断，若重复直接覆盖；
- 4.若不重复且当前处于红黑树状态，调用putTreeVal()方法；若不重复且当前处于链表阶段，遍历链表直到找到重复节点或者链表尾部，把该节点插入尾部；存在重复key就跳出循环。
- 5.存在重复key，进行value替换。
- 6.扩容验证。
   
## resize()方法
resize()在哈希表为null时将会初始化，但是在已经初始化后就会进行容量扩展

    final Node<K,V>[] resize() {
            Node<K,V>[] oldTab = table;
            int oldCap = (oldTab == null) ? 0 : oldTab.length;// 旧表容量
            int oldThr = threshold;// 旧表的扩容阈值
            int newCap, newThr = 0;
            // 旧表存在
            if (oldCap > 0) {
                // 容量已达上限 
                if (oldCap >= MAXIMUM_CAPACITY) {
                    threshold = Integer.MAX_VALUE;
                    return oldTab;
                }
                // 否则新容量与新阈值都扩大2倍
                else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                         oldCap >= DEFAULT_INITIAL_CAPACITY)
                    newThr = oldThr << 1; // double threshold
            }
            / /如果就阈值>0，说明构造方法中指定了容量
            else if (oldThr > 0) // initial capacity was placed in threshold
                newCap = oldThr;
            // 初始化时没有指定阈值和容量，使用默认的容量16和阈值16*0.75=12    
            else {               // zero initial threshold signifies using defaults
                newCap = DEFAULT_INITIAL_CAPACITY;
                newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            }
            //如果新的阈值为 0 ，就得用 新容量*加载因子 重计算一次
            if (newThr == 0) {
                float ft = (float)newCap * loadFactor;
                newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                          (int)ft : Integer.MAX_VALUE);
            }
            threshold = newThr;
            //常见扩容后的hash表
            @SuppressWarnings({"rawtypes","unchecked"})
                Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
            table = newTab;
            // 属于扩容
            if (oldTab != null) {
                //遍历旧表 
                for (int j = 0; j < oldCap; ++j) {
                    Node<K,V> e;
                    // 当前位置有值
                    if ((e = oldTab[j]) != null) {
                        oldTab[j] = null;
                        //如果只有一个节点，直接在新表中赋值
                        if (e.next == null)
                            newTab[e.hash & (newCap - 1)] = e;
                        else if (e instanceof TreeNode)
                            //如果旧哈希表中这个位置的桶是树形结构，就要把新哈希表里当前桶也变成树形结构
                            ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                        else { // preserve order 保留旧哈希表桶中链表的顺序
                            // 通过e.hash & oldCap将链表分为两队，参考知乎上的一段解释 
                            /** 
                            * 把链表上的键值对按hash值分成lo和hi两串，lo串的新索引位置与原先相同[原先位j]，hi串的新索引位置为[原先位置j+oldCap]； 
                            * 链表的键值对加入lo还是hi串取决于 判断条件if ((e.hash & oldCap) == 0)，因为* capacity是2的幂，所以oldCap为10...0的二进制形式，若判断条件为真，意味着 
                            * oldCap为1的那位对应的hash位为0，对新索引的计算没有影响（新索引 
                            * =hash&(newCap-*1)，newCap=oldCap<<2）；若判断条件为假，则 oldCap为1的那位* 对应的hash位为1， 
                            * 即新索引=hash&( newCap-1 )= hash&( (oldCap<<2) - 1)，相当于多了10...0， 
                            * 即 oldCap 
                            * 例子： 
                            * 旧容量=16，二进制10000；新容量=32，二进制100000 
                            * 旧索引的计算： 
                            * hash = xxxx xxxx xxxy xxxx 
                            * 旧容量-1 1111 
                            * &运算 xxxx 
                            * 新索引的计算： 
                            * hash = xxxx xxxx xxxy xxxx 
                            * 新容量-1 1 1111 
                            * &运算 y xxxx 
                            * 新索引 = 旧索引 + y0000，若判断条件为真，则y=0(lo串索引不变)，否则y=1(hi串 
                            * 索引=旧索引+旧容量10000) 
                               */  
                            Node<K,V> loHead = null, loTail = null;
                            Node<K,V> hiHead = null, hiTail = null;
                            Node<K,V> next;
                            do {
                                next = e.next;
                                // 双链表 尾插法 
                                if ((e.hash & oldCap) == 0) {
                                    if (loTail == null)
                                        loHead = e;
                                    else
                                        loTail.next = e;
                                    loTail = e;
                                }
                                else {
                                    if (hiTail == null)
                                        hiHead = e;
                                    else
                                        hiTail.next = e;
                                    hiTail = e;
                                }
                            } while ((e = next) != null);
                            if (loTail != null) {
                                loTail.next = null;
                                newTab[j] = loHead;
                            }
                            if (hiTail != null) {
                                hiTail.next = null;
                                newTab[j + oldCap] = hiHead;
                            }
                        }
                    }
                }
            }
            return newTab;
    }
    
resize()首先获取新容量以及新阈值，然后根据新容量创建新表。如果是扩容操作，则需要进行rehash操作，通过e.hash&oldCap将链表分为两列，更好地均匀分布在新表中。     

## get(K k)操作
get(K k)根据键得到值，如果值不存在，那么返回null

    public V get(Object key) {
            Node<K,V> e;
            return (e = getNode(hash(key), key)) == null ? null : e.value;
        }
    
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        // hash表不为空且第一个节点存在
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            // 第一个节点即为要查询的节点
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            // 链表不止一个头节点    
            if ((e = first.next) != null) {
                // 首节点是红黑树节点，调用getTreeNode方法
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                // 首节点是链表,循环遍历链表
                do {
                    // 匹配就返回
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }  

getNode()过程：

- 1.如果hash表为空或者长度为0或找不到hash值对应的数组位置，返回null
- 2.如果头节点匹配，返回头结点。
- 3.如果头结点不匹配且没有后续节点，返回null
- 4.如果头结点不匹配且头结点是红黑树类型，调用getTreeNode方法寻找节点
- 5.如果头结点不匹配且头结点是链表结构，从前往后遍历，找到相应的节点就返回
     
## remove()操作
remove(K k)用于根据键值删除键值对，如果哈希表中存在该键，那么返回键对应的值，否则返回null。  
    
    public V remove(Object key) {
        Node<K,V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }
    
    final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        // 当前hash表存在且长度大于0且数组中存在头结点
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;
            // 头节点即为key所对应的节点，node即为头节点
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            // 头结点有下一个节点    
            else if ((e = p.next) != null) {
                // 首节点是红黑树节点，调用getTreeNode方法
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    // 首节点是链表,遍历链表
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            //如果存在待删除节点节点
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
                //如果节点是TreeNode，使用红黑树的方法                 
                if (node instanceof TreeNode)
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                //如果待删除节点是头节点，更改桶中的头节点即可    
                else if (node == p)
                    tab[index] = node.next;
                //在链表遍历过程中，p代表node节点的前驱节点    
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }   
    
总体来说，removeNode（）先查出待删除的节点，查找过程与查询过程类型，只不过多了在遍历链表时还需要保存前驱节点，因为后面删除时要用到（单链表结构）。存在待删除节点，接下来再执行删除操作.

- 1.如果待删除节点是TreeNode，那么调用removeTreeNode()方法 
- 2.如果待删除节点是Node，并且待删除节点就是头节点，那么将头节点更改为原有节点的下一个节点就可以了 
- 3.如果待删除节点是Node且待删除节点不是头节点，那么将遍历过程中保存的前驱节点p的后继节点设为node的后继节点就可以了
