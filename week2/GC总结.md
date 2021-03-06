**1、堆(Heap)**

JVM管理的内存叫堆。在32Bit操作系统上有1.5G-2G的限制，而64Bit的就没有。

JVM初始分配的内存由-Xms指定，默认是物理内存的1/64但小于1G。

JVM最大分配的内存由-Xmx指定，默认是物理内存的1/4但小于1G。

默认空余堆内存小于40%时，JVM就会增大堆直到-Xmx的最大限制，可以由-XX:MinHeapFreeRatio=指定。
默认空余堆内存大于70%时，JVM会减少堆直到-Xms的最小限制，可以由-XX:MaxHeapFreeRatio=指定。

服务器一般设置-Xms、-Xmx相等以避免在每次GC 后调整堆的大小。

**2、分代**
分代是Java垃圾收集的一大亮点，根据对象的生命周期长短，把堆分为3个代：Young，Old和Permanent，根据不同代的特点采用不同的收集算法，扬长避短也。
Young(Nursery)，年轻代。研究表明大部分对象都是朝生暮死，随生随灭的。因此所有收集器都为年轻代选择了复制算法。
复制算法优点是只访问活跃对象，缺点是复制成本高。因为年轻代只有少量的对象能熬到垃圾收集，因此只需少量的复制成本。而且复制收集器只访问活跃对象，对那些占了最大比率的死对象视而不见，充分发挥了它遍历空间成本低的优点。

1. **Young（年轻代**
   年 轻代分三个区。一个Eden区，两个Survivor区。大部分对象在Eden区中生成。当Eden区满时，还存活的对象将被复制到Survivor区 （两个中的一个），当这个Survivor区满时，此区的存活对象将被复制到另外一个Survivor区，当这个Survivor去也满了的时候，从第一 个Survivor区复制过来的并且此时还存活的对象，将被复制“年老区(Tenured)”。需要注意，Survivor的两个区是对称的，没先后关 系，所以同一个区中可能同时存在从Eden复制过来 对象，和从前一个Survivor复制过来的对象，而复制到年老区的只有从第一个Survivor去过来的对象。而且，Survivor区总有一个是空 的。
2. **Tenured（年老代**
   **年老代存放从年轻代存活的对象。一般来说年老代存放的都是生命期较长的对象。
3. **Perm（持久代**
   用 于存放静态文件，如今Java类、方法等。持久代对垃圾回收没有显著影响，但是有些应用可能动态生成或者调用一些class，例如Hibernate等， 在这种时候需要设置一个比较大的持久代空间来存放这些运行过程中新增的类。持久代大小通过-XX:MaxPermSize=进行设置。

二：Gc的基本概念
gc分为full gc 跟 minor gc，当每一块区满的时候都会引发gc。

1. Scavenge GC
   一般情况下，当新对象生成，并且在Eden申请空间失败时，就触发了Scavenge GC，堆Eden区域进行GC，清除非存活对象，并且把尚且存活的对象移动到Survivor区。然后整理Survivor的两个区。

2. Full GC

   对整个堆进行整理，包括Young、Tenured和Perm。Full GC比Scavenge GC要慢，因此应该尽可能减少Full GC。有如下原因可能导致Full GC：

   - Tenured被写满

   - Perm域被写满

   - System.gc()被显示调用

   - 上一次GC之后Heap的各域分配策略动态变化


三：GC的相关介绍

1：垃圾收集器介绍。

**串行收集器**
使用单线程处理所有垃圾回收工作，因为无需多线程交互，所以效率比较高。但是，也无法使用多处理器的优势，所以此收集器适合单处理器机器。当然，此收集器也可以用在小数据量（100M左右）情况下的多处理器机器上。可以使用-XX:+UseSerialGC打开。
**并行收集器**

1. 对年轻代进行并行垃圾回收，因此可以减少垃圾回收时间。一般在多线程多处理器机器上使用。使用-XX:+UseParallelGC.打开。并行收集器在J2SE5.0第六6更新上引入，在Java SE6.0中进行了增强--可以堆年老代进行并行收集。如果年老代不使用并发收集的话，是使用单线程进行垃圾回收，因此会制约扩展能力。使用-XX:+UseParallelOldGC打开。

2. 使用-XX:ParallelGCThreads=设置并行垃圾回收的线程数。此值可以设置与机器处理器数量相等。

3. 此收集器可以进行如下配置：
   最大垃圾回收暂停:指定垃圾回收时的最长暂停时间，通过-XX:MaxGCPauseMillis=指定。为毫秒.如果指定了此值的话，堆大小和垃圾回收相关参数会进行调整以达到指定值。设定此值可能会减少应用的吞吐量。
   吞吐量:吞吐量为垃圾回收时间与非垃圾回收时间的比值，通过-XX:GCTimeRatio=来设定，公式为1/（1+N）。例如，-XX:GCTimeRatio=19时，表示5%的时间用于垃圾回收。默认情况为99，即1%的时间用于垃圾回收。

**并发收集器**
可以保证大部分工作都并发进行（应用不停止），垃圾回收只暂停很少的时间，此收集器适合对响应时间要求比较高的中、大规模应用。使用-XX:+UseConcMarkSweepGC打开。

1. 并发收集器主要减少年老代的暂停时间，他在应用不停止的情况下使用独立的垃圾回收线程，跟踪可达对象。在每个年老代垃圾回收周期中，在收集初期并发收集器会对整个应用进行简短的暂停，在收集中还会再暂停一次。第二次暂停会比第一次稍长，在此过程中多个线程同时进行垃圾回收工作。

2. 并发收集器使用处理器换来短暂的停顿时间。在一个N个处理器的系统上，并发收集部分使用K/N个可用处理器进行回收，一般情况下1<=K<=N/4。

3. 在只有一个处理器的主机上使用并发收集器，设置为incremental mode模式也可获得较短的停顿时间。

4. 浮动垃圾：由于在应用运行的同时进行垃圾回收，所以有些垃圾可能在垃圾回收进行完成时产生，这样就造成了“Floating Garbage”，这些垃圾需要在下次垃圾回收周期时才能回收掉。所以，并发收集器一般需要20%的预留空间用于这些浮动垃圾。

5. Concurrent Mode Failure：并发收集器在应用运行时进行收集，所以需要保证堆在垃圾回收的这段时间有足够的空间供程序使用，否则，垃圾回收还未完成，堆空间先满了。这种情况下将会发生“并发模式失败”，此时整个应用将会暂停，进行垃圾回收。

6. 启动并发收集器：因为并发收集在应用运行时进行收集，所以必须保证收集完成之前有足够的内存空间供程序使用，否则会出现“Concurrent Mode Failure”。通过设置-XX:CMSInitiatingOccupancyFraction=指定还有多少剩余堆时开始执行并发收集

**小结**

 串行处理器：
--适用情况：数据量比较小（100M左右）；单处理器下并且对响应时间无要求的应用。
--缺点：只能用于小型应用
并行处理器：
--适用情况：“对吞吐量有高要求”，多CPU、对应用响应时间无要求的中、大型应用。举例：后台处理、科学计算。
--缺点：应用响应时间可能较长
 并发处理器：传说中的CMS
--适用情况：“对响应时间有高要求”，多CPU、对应用响应时间有较高要求的中、大型应用。举例：Web服务器/应用服务器、电信交换、集成开发环境。

2.基本收集算法

1. 复制：将堆内分成两个相同空间，从根(ThreadLocal的对象，静态对象）开始访问每一个关联的活跃对象，将空间A的活跃对象全部复制到空间B，然后一次性回收整个空间A。
   因为只访问活跃对象，将所有活动对象复制走之后就清空整个空间，不用去访问死对象，所以遍历空间的成本较小，但需要巨大的复制成本和较多的内存。

2. 标记清除(mark-sweep)：收集器先从根开始访问所有活跃对象，标记为活跃对象。然后再遍历一次整个内存区域，把所有没有标记活跃的对象进行回收处理。该算法遍历整个空间的成本较大暂停时间随空间大小线性增大，而且整理后堆里的碎片很多。

3. 标记整理(mark-sweep-compact)：综合了上述两者的做法和优点，先标记活跃对象，然后将其合并成较大的内存块。
   可见，没有免费的午餐，无论采用复制还是标记清除算法，自动的东西都要付出很大的性能代价。

四：调整jboss后的相关参数来查看gc
通过在java的ops后面添加相关参数来查看gc，并输出到日志。
在set JAVA_OPTS=%JAVA_OPTS% -Xms128m -Xmx512m 后面加上：
-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+HeapDumpOnOutOfMemoryError -Xloggc:d:/gc.log

其中的PrintGCDetails 打印gc的详细信息。
PrintGCTimeStamps 打印gc的时间戳
-Xloggc:d:/gc.log 将日志信息输出到log，如果不指定就直接在jboss控制台输出了。
-XX:+HeapDumpOnOutOfMemoryError 可加可不加，特殊需要，我也不晓得是什么意思。

gc打印出来的详情一般为：
**[GC [DefNew: 8614K->781K(9088K), 0.0123035 secs] 118250K->113543K(130112K), 0.0124633 secs]
**这表示新生代大小整理前8614K，整理后还有781K（有可能为0，表示都清掉了），后面的9088表示总大小。
后面的就表示对整个堆空间而言。整理前118250K，整理后113543K，总大小为130112K。

五：常见jvm的参数设置
一般设置为：（参考江南白衣。。。）
***\*-server -XmsM -XmxM -XX:+UseConcMarkSweepGC -XX:+PrintGC Details -XX:+PrintGCTimeStamps
除此之外扩展的有一些典型设置：
1：堆大小设置

JVM 中最大堆大小有三方面限制：相关操作系统的数据模型（32-bt还是64-bit）限制；系统的可用虚拟内存限制；系统的可用物理内存限制。32位系统下，一般限制在1.5G~2G；64为操作系统对内存无限制。我在Windows Server 2003 系统，3.5G物理内存，JDK5.0下测试，最大可设置为1478m。
典型设置：

 java -Xmx3550m -Xms3550m -Xmn2g -Xss128k
-Xmx3550m：设置JVM最大可用内存为3550M。
-Xms3550m：设置JVM促使内存为3550m。此值可以设置与-Xmx相同，以避免每次垃圾回收完成后JVM重新分配内存。
-Xmn2g：设置年轻代大小为2G。整个堆大小=年轻代大小 + 年老代大小 + 持久代大小。持久代一般固定大小为64m，所以增大年轻代后，将会减小年老代大小。此值对系统性能影响较大，Sun官方推荐配置为整个堆的3/8。
-Xss128k：设置每个线程的堆栈大小。JDK5.0以后每个线程堆栈大小为1M，以前每个线程堆栈大小为256K。更具应用的线程所需内存大小进行调整。在相同物理内存下，减小这个值能生成更多的线程。但是操作系统对一个进程内的线程数还是有限制的，不能无限生成，经验值在3000~5000左右。
 java -Xmx3550m -Xms3550m -Xss128k -XX:NewRatio=4 -XX:SurvivorRatio=4 -XX:MaxPermSize=16m -XX:MaxTenuringThreshold=0
-XX:NewRatio=4:设置年轻代（包括Eden和两个Survivor区）与年老代的比值（除去持久代）。设置为4，则年轻代与年老代所占比值为1：4，年轻代占整个堆栈的1/5
-XX:SurvivorRatio=4：设置年轻代中Eden区与Survivor区的大小比值。设置为4，则两个Survivor区与一个Eden区的比值为2:4，一个Survivor区占整个年轻代的1/6
-XX:MaxPermSize=16m:设置持久代大小为16m。
-XX:MaxTenuringThreshold=0：设置垃圾最大年龄。如果设置为0的话，则年轻代对象不经过Survivor区，直接进入年老代。对于年老代比较多的应用，可以提高效率。如果将此值设置为一个较大值，则年轻代对象会在Survivor区进行多次复制，这样可以增加对象再年轻代的存活时间，增加在年轻代即被回收的概论。\****