package org.mapdb

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mapdb.guavaTests.ConcurrentMapInterfaceTest
import org.mapdb.jsr166Tests.ConcurrentHashMapTest
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentMap
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class HTreeMapTest{

    @Test fun hashAssertion(){
        val map = HTreeMap.make<ByteArray,Int>(keySerializer = Serializer.JAVA as Serializer<ByteArray>)

        try {
            for (i in 1..100)
                map.put(TT.randomByteArray(10), 11)
            fail("hash exception expected")
        }catch(e:IllegalArgumentException){
            assertTrue(e.message!!.contains("hash"))
        }

        val map2 = HTreeMap.make<Any,Int>(keySerializer = Serializer.JAVA,
                stores = arrayOf(StoreOnHeap()), concShift = 0)

        class NotSerializable{

        }
        map2.put(NotSerializable(), 11)
    }


    @Test fun valueCreator(){
        val map = HTreeMap.make<Int,Int>(valueCreator={it+10})
        assertEquals(11, map[1])
        assertEquals(1, map.size)
    }

@RunWith(Parameterized::class)
class Guava(val mapMaker:(generic:Boolean)->ConcurrentMap<Any?, Any?> ) :
        ConcurrentMapInterfaceTest<Int, String>(
            false,  // boolean allowsNullKeys,
            false,  // boolean allowsNullValues,
            true,   // boolean supportsPut,
            true,   // boolean supportsRemove,
            true,   // boolean supportsClear,
            true    // boolean supportsIteratorRemove
    ){

    companion object {

        val singleHashSerializer = object : Serializer<Int>() {
            override fun deserialize(input: DataInput2, available: Int) = input.readInt()

            override fun serialize(out: DataOutput2, value: Int) {
                out.writeInt(value)
            }

            override fun hashCode(a: Int, seed: Int): Int {
                //NOTE: fixed hash to generate collisions
                return seed
            }
        }

        @Parameterized.Parameters
        @Throws(IOException::class)
        @JvmStatic
        fun params(): Iterable<Any> {
            val ret = ArrayList<Any>()

            val bools = if(TT.shortTest()) TT.boolsFalse else TT.bools

            for(inlineKey in bools)
            for(inlineValue in bools)
            for(singleHash in bools)
            for(segmented in bools)
            for(createExpire in bools)
            for(updateExpire in bools)
            for(getExpire in bools)
            for(onHeap in bools)
            for(counter in bools)
            {
                ret.add(arrayOf<Any>({generic:Boolean->

                    var maker =
                            if(segmented) {
                                if(onHeap)DBMaker.heapSegmentedHashMap(3)
                                else DBMaker.memorySegmentedHashMap(3)
                            }else {
                                val db =
                                        if(onHeap) DBMaker.heapDB().make()
                                        else DBMaker.memoryDB().make()
                                db.hashMap("aa")
                            }

                    val keySerializer =
                            if (singleHash.not()) Serializer.INTEGER
                            else singleHashSerializer

                    if(inlineKey)
                        maker.keyInline()
                    if(inlineValue)
                        maker.valueInline()

                    if(createExpire)
                        maker.expireAfterCreate(Integer.MAX_VALUE.toLong())
                    if(updateExpire)
                        maker.expireAfterUpdate(Integer.MAX_VALUE.toLong())
                    if(getExpire)
                        maker.expireAfterGet(Integer.MAX_VALUE.toLong())
                    if(counter)
                        maker.counterEnable()

                    if(!generic)
                        maker.keySerializer(keySerializer).valueSerializer(Serializer.STRING)

                    maker.hashSeed(1).create()

                }))

            }

            return ret
        }

    }

    override fun getKeyNotInPopulatedMap(): Int = -10

    override fun getValueNotInPopulatedMap(): String = "-120"
    override fun getSecondValueNotInPopulatedMap(): String = "-121"

    open override fun makeEmptyMap(): ConcurrentMap<Int?, String?> {
        return mapMaker(false) as ConcurrentMap<Int?, String?>
    }

    override fun makePopulatedMap(): ConcurrentMap<Int?, String?>? {
        val ret = makeEmptyMap()
        for(i in 0 until 30) {
            ret.put(i,  "aa"+i)
        }
        return ret;
    }

    override fun supportsValuesHashCode(map: MutableMap<Int, String>?): Boolean {
        // keySerializer returns wrong hash on purpose for this test, so pass it
        return false;
    }

}


@RunWith(Parameterized::class)
class JSR166_ConcurrentHashMapTest(
        val mapMaker:(generic:Boolean)->ConcurrentMap<Any?, Any?>
        ) : ConcurrentHashMapTest()
{

    override fun makeGenericMap(): ConcurrentMap<Any?, Any?>? {
        return mapMaker(true)
    }

    override fun makeMap(): ConcurrentMap<Int?, String?>? {
        return mapMaker(false) as ConcurrentMap<Int?, String?>
    }

    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun params(): Iterable<Any> {
            return HTreeMapTest.Guava.params()
        }
    }

}

}